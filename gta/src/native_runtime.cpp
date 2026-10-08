#include "native_runtime.hpp"
#include "compositor.h"
#include <MinHook.h>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <string>
#include <unordered_map>
#include <vector>
#include <atomic>
#include <algorithm>
#include <intrin.h>

// Own adapter for the Legacy native ABI. No ScriptHookV DLL/SDK is loaded.
// Native hash translation is read from a separately licensed data file.
namespace native_runtime {
namespace {
    using Handler = void (*)(Context*);
    using Lookup = Handler (*)(void*, uint64_t);
    HMODULE module;
    Lookup lookup;
    void* table;
    using RunThreads = bool (*)(uint32_t);
    RunThreads originalRunThreads;
    struct ThreadArray { void** data; uint16_t count, capacity; };
    ThreadArray* gameThreads;
    void (*scriptEntry)();
    Keyboard keyboardEntry;
    DWORD* tlsIndex;
    uint32_t scriptField;
    uint32_t activeField;
    void* currentThread() {
        auto slots=reinterpret_cast<unsigned char**>(__readgsqword(0x58));
        auto tls=slots?slots[*tlsIndex]:nullptr;
        return tls?*reinterpret_cast<void**>(tls+scriptField):nullptr;
    }
    void* parentFiber;
    void* scriptFiber;
    DWORD scriptThreadId;
    int lastFrame = -1;
    ULONGLONG wakeAt;
    bool keyDown[256]{};
    ULONGLONG keyRepeatAt[256]{};
    bool keyboardFocused = false;
    std::atomic<bool> stopped{false};
    std::unordered_map<uint64_t, Handler> handlers;
    std::filesystem::path logPath;
    thread_local uint64_t lastNative;
    HANDLE diagnosticLog = INVALID_HANDLE_VALUE;
    PVOID diagnosticHandler;
    LONG CALLBACK traceException(EXCEPTION_POINTERS* exception) {
        if (exception->ExceptionRecord->ExceptionCode != EXCEPTION_ACCESS_VIOLATION || diagnosticLog == INVALID_HANDLE_VALUE)
            return EXCEPTION_CONTINUE_SEARCH;
        // Test-only bounded metadata, never a memory dump or input recording.
        // Observe the exception without swallowing it or changing game handling.
        const auto address = exception->ExceptionRecord->ExceptionAddress;
        HMODULE faultModule{};
        char filename[MAX_PATH]{};
        GetModuleHandleExA(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            reinterpret_cast<LPCSTR>(address), &faultModule);
        GetModuleFileNameA(faultModule, filename, MAX_PATH);
        const char* name = strrchr(filename, '\\');
        char line[512]{};
        const auto offset = reinterpret_cast<uintptr_t>(address) - reinterpret_cast<uintptr_t>(faultModule);
        const auto count = _snprintf_s(line, sizeof(line), _TRUNCATE,
            "AV thread=%lu module=%s offset=%llx native=%llx access=%llu target=%llx\r\n",
            GetCurrentThreadId(), name ? name+1 : filename, static_cast<unsigned long long>(offset),
            static_cast<unsigned long long>(lastNative),
            exception->ExceptionRecord->ExceptionInformation[0], exception->ExceptionRecord->ExceptionInformation[1]);
        DWORD written{};
        if(count > 0) WriteFile(diagnosticLog, line, DWORD(count), &written, nullptr);
        return EXCEPTION_CONTINUE_SEARCH;
    }
    void log(const std::string& line) {
        std::ofstream(logPath, std::ios::app) << line << '\n';
        if(diagnosticLog!=INVALID_HANDLE_VALUE) {
            const auto text=line+"\r\n"; DWORD written{};
            WriteFile(diagnosticLog,text.data(),DWORD(text.size()),&written,nullptr);
        }
    }
    uintptr_t scan(const char* pattern) {
        std::istringstream tokens(pattern);
        std::string token;
        std::vector<int> bytes;
        while (tokens >> token) bytes.push_back(token == "?" ? -1 : std::stoi(token, nullptr, 16));
        auto base = reinterpret_cast<uintptr_t>(GetModuleHandleW(nullptr));
        auto dos = reinterpret_cast<IMAGE_DOS_HEADER*>(base);
        auto nt = reinterpret_cast<IMAGE_NT_HEADERS*>(base + dos->e_lfanew);
        auto sections = IMAGE_FIRST_SECTION(nt);
        uintptr_t found = 0;
        for (unsigned s = 0; s < nt->FileHeader.NumberOfSections; ++s) {
            if (!(sections[s].Characteristics & IMAGE_SCN_MEM_EXECUTE)) continue;
            auto begin = base + sections[s].VirtualAddress;
            auto end = begin + sections[s].Misc.VirtualSize;
            for (uintptr_t region=begin; region<end;) {
                MEMORY_BASIC_INFORMATION info{};
                if(!VirtualQuery(reinterpret_cast<void*>(region),&info,sizeof(info))) break;
                auto next=std::min(end,reinterpret_cast<uintptr_t>(info.BaseAddress)+info.RegionSize);
                if(next<=region) break;
                if(info.State!=MEM_COMMIT || (info.Protect&(PAGE_NOACCESS|PAGE_GUARD))) { region=next; continue; }
                auto data=reinterpret_cast<unsigned char*>(region);
                auto size=next-region;
                for (size_t i = 0; i + bytes.size() <= size; ++i) {
                bool equal = true;
                for (size_t j = 0; j < bytes.size(); ++j)
                    if (bytes[j] >= 0 && data[i+j] != bytes[j]) { equal = false; break; }
                if (equal) { if (found) return 0; found = reinterpret_cast<uintptr_t>(data+i); }
                }
                region=next;
            }
        }
        return found;
    }
    uint32_t joaat(const char* value) {
        uint32_t h=0; while (*value) { h += static_cast<unsigned char>(*value++); h += h<<10; h ^= h>>6; }
        h += h<<3; h ^= h>>11; h += h<<15; return h;
    }
    Handler lookupSafe(uint64_t hash) {
        __try { return lookup(table,hash); }
        __except(EXCEPTION_EXECUTE_HANDLER) { return nullptr; }
    }
    Handler resolve(uint64_t publicHash, uint64_t runtimeHash) {
        Handler h = lookupSafe(runtimeHash);
        if (h) handlers[publicHash] = h;
        return h;
    }
    bool loadMap(const std::filesystem::path& root) {
        std::ifstream input(root / L"MCPassthrough" / L"native-map.csv");
        std::string line;
        size_t count=0;
        while (std::getline(input,line)) {
            auto comma=line.find(','); if(comma==std::string::npos) continue;
            auto a=std::stoull(line.substr(0,comma),nullptr,16);
            auto b=std::stoull(line.substr(comma+1),nullptr,16);
            if (!resolve(a,b)) { log("Required native unavailable; adapter stays disabled: "+line); return false; }
            ++count;
        }
        log("Resolved required native handlers: "+std::to_string(count));
        return count > 100;
    }
    void WINAPI fiberMain(void*) {
        try { scriptEntry(); }
        catch(const std::exception& e) { log(std::string("Plugin stopped: ")+e.what()); stopped=true; compositor::set_active(false); }
        for (;;) SwitchToFiber(parentFiber);
    }
    void pollKeyboard() {
        // The script runs on an engine worker, not the window message thread.
        // Read physical key state only while this game's window owns focus.
        DWORD foregroundProcess = 0;
        const auto windowThread = GetWindowThreadProcessId(GetForegroundWindow(), &foregroundProcess);
        const bool focused = foregroundProcess == GetCurrentProcessId();
        const auto layout = GetKeyboardLayout(windowThread);
        const auto now = GetTickCount64();
        for (DWORD key = VK_BACK; key < 255; ++key) {
            // Generic modifiers already combine both sides; don't send duplicates.
            if (key >= VK_LSHIFT && key <= VK_RMENU) continue;
            const bool down = focused && (GetAsyncKeyState(key) & 0x8000) != 0;
            const bool wasDown = keyDown[key];
            keyDown[key] = down;
            if (focused && !keyboardFocused) {
                keyRepeatAt[key] = now + 500;
                continue; // Don't interpret keys held while switching apps as presses.
            }
            if (!keyboardEntry || stopped) continue;
            const bool repeat = down && wasDown && now >= keyRepeatAt[key];
            if (down != wasDown || repeat) {
                const auto scan = MapVirtualKeyExW(key, MAPVK_VK_TO_VSC, layout);
                keyboardEntry(key, 1, BYTE(scan), FALSE, FALSE, wasDown, !down);
                keyRepeatAt[key] = now + (repeat ? 33 : 500);
            }
        }
        keyboardFocused = focused;
    }
    bool invokeSafe(Handler fn, Context* context) {
        __try { fn(context); return true; }
        __except(EXCEPTION_EXECUTE_HANDLER) { return false; }
    }
    void* persistentThread() {
        __try {
            const auto persistent=joaat("main_persistent");
            if(!gameThreads || !gameThreads->data || gameThreads->count>2048 || gameThreads->count>gameThreads->capacity) return nullptr;
            for(unsigned i=0;i<gameThreads->count;++i) {
                auto thread=static_cast<unsigned char*>(gameThreads->data[i]);
                if(thread && *reinterpret_cast<uint32_t*>(thread+8)!=0 && *reinterpret_cast<uint32_t*>(thread+0x0C)==persistent) return thread;
            }
            return nullptr;
        } __except(EXCEPTION_EXECUTE_HANDLER) { return nullptr; }
    }
    struct ScopedScriptContext {
        unsigned char* tls;
        void* previous;
        bool previousActive;
        explicit ScopedScriptContext(void* host) {
            auto slots=reinterpret_cast<unsigned char**>(__readgsqword(0x58));
            tls=slots[*tlsIndex];
            previous=*reinterpret_cast<void**>(tls+scriptField);
            previousActive=*(tls+activeField)!=0;
            *reinterpret_cast<void**>(tls+scriptField)=host;
            *(tls+activeField)=1;
        }
        ~ScopedScriptContext() {
            *reinterpret_cast<void**>(tls+scriptField)=previous;
            *(tls+activeField)=previousActive;
        }
    };
    bool onRunThreads(uint32_t operations) {
        // Finish GTA's own interpreter pass first. The mod does not replace WAIT
        // or run inside a VM native call. Restore the caller's TLS on every exit.
        const bool result=originalRunThreads(operations);
        if(auto host=stopped?nullptr:persistentThread()) {
            if (!scriptThreadId) scriptThreadId=GetCurrentThreadId();
            if (scriptThreadId==GetCurrentThreadId()) {
                ScopedScriptContext scriptContext(host);
                try {
                    if(invoke<BOOL>(0x9DE624D2FC4B603F) || invoke<BOOL>(0x10FAB35428CCC9D7)) {
                        stopped=true; compositor::set_active(false); log("Online session detected; plugin disabled.");
                    } else if(GetTickCount64()>=wakeAt && invoke<int>(0xFC8202EFC642E6F2)!=lastFrame) {
                        lastFrame=invoke<int>(0xFC8202EFC642E6F2);
                        pollKeyboard();
                        // Engine jobs may use a different fiber on the same OS
                        // thread next frame. Always yield to this invocation's
                        // caller, rather than retaining a returned stack.
                        parentFiber=IsThreadAFiber()?GetCurrentFiber():ConvertThreadToFiber(nullptr);
                        if(parentFiber && !scriptFiber) {
                            scriptFiber=CreateFiber(0,fiberMain,nullptr);
                            log("Keyboard forwarding uses focused physical key states.");
                            log("Story Mode adapter running after the game script loop, without ScriptHookV.");
                        }
                        if(scriptFiber) SwitchToFiber(scriptFiber);
                    }
                } catch(const std::exception& e) { stopped=true; compositor::set_active(false); log(e.what()); }
            }
        }
        return result;
    }
    DWORD initializeAdapter() {
        wchar_t filename[32768]{}; GetModuleFileNameW(module,filename,32768);
        auto root=std::filesystem::path(filename).parent_path();
        // The marker is created only for local diagnosis, never included in a release.
        if(std::filesystem::exists(root / L"MCPassthrough" / L"diagnostic-test.flag")) {
            const auto path=root / L"MCPassthrough" / L"diagnostic-test.log";
            diagnosticLog=CreateFileW(path.c_str(),FILE_APPEND_DATA,FILE_SHARE_READ,nullptr,OPEN_ALWAYS,FILE_ATTRIBUTE_NORMAL,nullptr);
            if(diagnosticLog!=INVALID_HANDLE_VALUE) diagnosticHandler=AddVectoredExceptionHandler(0,traceException);
        }
        wchar_t local[32768]{};
        GetEnvironmentVariableW(L"LOCALAPPDATA",local,32768);
        auto logs=std::filesystem::path(local) / L"MinecraftGTAV" / L"logs";
        std::filesystem::create_directories(logs);
        logPath=logs / L"native-adapter.log";
        const auto command=std::wstring(GetCommandLineW());
        if(command.find(L"-nobattleye")==std::wstring::npos) { stopped=true; log("Official offline -nobattleye launch argument required; adapter disabled."); return 0; }
        // Wait for the game's own code to be ready. Unsupported or ambiguous signatures fail closed.
        uintptr_t nativeSite=0;
        for(int i=0;i<120 && !stopped;++i) {
            nativeSite=scan("48 8D 0D ? ? ? ? 48 8B 14 FA E8 ? ? ? ? 48 85 C0 75 0A");
            if(nativeSite) break;
            Sleep(1000);
        }
        if(!nativeSite) { stopped=true; log("Legacy native signature missing or ambiguous; game build unsupported."); return 0; }
        // The game's own script-context switch loads the pointer field into r15
        // and its adjacent active byte into r14. Resolve both from one unique
        // instruction sequence; don't assume another build's TLS layout.
        auto scriptSite=scan("41 BF ? ? ? ? 41 BE ? ? ? ? 4A 8B 3C 3A");
        if(!scriptSite) { stopped=true; log("Script-context switch missing or ambiguous; adapter disabled."); return 0; }
        scriptField=*reinterpret_cast<uint32_t*>(scriptSite+2);
        activeField=*reinterpret_cast<uint32_t*>(scriptSite+8);
        if(scriptField>0x8000 || activeField!=scriptField+sizeof(void*)) { stopped=true; log("Script-context layout invalid; adapter disabled."); return 0; }
        auto gameBase=reinterpret_cast<uintptr_t>(GetModuleHandleW(nullptr));
        auto gameDos=reinterpret_cast<IMAGE_DOS_HEADER*>(gameBase);
        auto gameNt=reinterpret_cast<IMAGE_NT_HEADERS*>(gameBase+gameDos->e_lfanew);
        const auto tlsRva=gameNt->OptionalHeader.DataDirectory[IMAGE_DIRECTORY_ENTRY_TLS].VirtualAddress;
        if(!tlsRva) { stopped=true; log("Game TLS directory missing; adapter disabled."); return 0; }
        auto tlsDirectory=reinterpret_cast<IMAGE_TLS_DIRECTORY64*>(gameBase+tlsRva);
        tlsIndex=reinterpret_cast<DWORD*>(tlsDirectory->AddressOfIndex);
        if(!tlsIndex || *tlsIndex>4096) { stopped=true; log("Game TLS index invalid; adapter disabled."); return 0; }
        table=reinterpret_cast<void*>(nativeSite+7+*reinterpret_cast<int32_t*>(nativeSite+3));
        lookup=reinterpret_cast<Lookup>(nativeSite+16+*reinterpret_cast<int32_t*>(nativeSite+12));
        log("Legacy native lookup found; using PE TLS index and verified script field "+std::to_string(scriptField)+".");
        // Wait until the built-in WAIT native has been registered before reading the full map.
        for(int i=0;i<120 && !stopped;++i) {
            if(lookupSafe(0x4EDE34FBADD967A6)) break;
            Sleep(1000);
        }
        try { if(!loadMap(root)) { stopped=true; return 0; } }
        catch(const std::exception& e) { stopped=true; log(e.what()); return 0; }
        auto runnerSite=scan("45 33 F6 8B E9 85 C9 B8");
        if(!runnerSite) { stopped=true; log("Legacy script loop missing or ambiguous; adapter disabled."); return 0; }
        gameThreads=reinterpret_cast<ThreadArray*>(runnerSite+*reinterpret_cast<int32_t*>(runnerSite-4)-8);
        auto runner=reinterpret_cast<void*>(runnerSite-0x1F);
        if(MH_Initialize()!=MH_OK || MH_CreateHook(runner,reinterpret_cast<void*>(onRunThreads),reinterpret_cast<void**>(&originalRunThreads))!=MH_OK || MH_EnableHook(runner)!=MH_OK) {
            stopped=true; log("Could not install Story Mode scheduler; adapter disabled."); return 0;
        }
        log("Native adapter initialized; waiting for Story Mode.");
        return 0;
    }
    DWORD WINAPI initialize(void*) {
        try { return initializeAdapter(); }
        catch(const std::exception& e) { stopped=true; log(std::string("Adapter initialization failed: ")+e.what()); return 0; }
    }
}
void call(uint64_t hash, Context& context) {
    lastNative=hash;
    auto it=handlers.find(hash);
    if(it==handlers.end() || !it->second || !invokeSafe(it->second,&context)) throw std::runtime_error("Unavailable or failed GTA native.");
    if(context.vectorCount<0 || context.vectorCount>4) throw std::runtime_error("Invalid native vector result.");
    for(int i=0;i<context.vectorCount;++i) {
        auto d=context.destinations[i]; if(!d) throw std::runtime_error("Invalid native vector destination.");
        d->x=context.sources[i].x; d->y=context.sources[i].y; d->z=context.sources[i].z;
    }
}
void wait(DWORD milliseconds) { wakeAt=GetTickCount64()+milliseconds; SwitchToFiber(parentFiber); }
void start(HMODULE mod,void (*entry)(),Keyboard keyboard) {
    module=mod; scriptEntry=entry; keyboardEntry=keyboard;
    auto handle=CreateThread(nullptr,0,initialize,nullptr,0,nullptr); if(handle) CloseHandle(handle);
}
void stop() {
    stopped=true;
    if(diagnosticHandler) RemoveVectoredExceptionHandler(diagnosticHandler);
    if(diagnosticLog!=INVALID_HANDLE_VALUE) CloseHandle(diagnosticLog);
}
}
