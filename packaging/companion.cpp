#include <windows.h>
#include <filesystem>
#include <fstream>
#include <string>
#include <shellapi.h>
#include <tlhelp32.h>

// Use one physical path spelling throughout. Windows app-container redirection
// can otherwise give Java two names for a jar and duplicate Fabric's classes.
static std::filesystem::path realPath(const std::filesystem::path& path) {
    HANDLE handle=CreateFileW(path.c_str(),0,FILE_SHARE_READ|FILE_SHARE_WRITE|FILE_SHARE_DELETE,nullptr,OPEN_EXISTING,FILE_FLAG_BACKUP_SEMANTICS,nullptr);
    if(handle==INVALID_HANDLE_VALUE) return path;
    wchar_t resolved[32768]{};
    DWORD size=GetFinalPathNameByHandleW(handle,resolved,32768,FILE_NAME_NORMALIZED|VOLUME_NAME_DOS);
    CloseHandle(handle);
    if(!size || size>=32768) return path;
    std::wstring value(resolved);
    if(value.rfind(L"\\\\?\\UNC\\",0)==0) value=L"\\\\"+value.substr(8);
    else if(value.rfind(L"\\\\?\\",0)==0) value=value.substr(4);
    return value;
}

// Configures only this release's dedicated portable Prism data folder.
// Microsoft authentication stays inside Prism; no accounts or tokens are copied.
int WINAPI wWinMain(HINSTANCE,HINSTANCE,PWSTR,int) {
    wchar_t executable[32768]{},local[32768]{};
    GetModuleFileNameW(nullptr,executable,32768);
    GetEnvironmentVariableW(L"LOCALAPPDATA",local,32768);
    const auto install=realPath(executable).parent_path();
    const auto java=(install/L"java"/L"bin"/L"javaw.exe").wstring();
    HANDLE snapshot=CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS,0);
    if(snapshot!=INVALID_HANDLE_VALUE) {
        PROCESSENTRY32W entry{}; entry.dwSize=sizeof(entry);
        if(Process32FirstW(snapshot,&entry)) do {
            if(_wcsicmp(entry.szExeFile,L"javaw.exe")) continue;
            HANDLE process=OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION,FALSE,entry.th32ProcessID);
            wchar_t path[32768]{}; DWORD length=32768;
            bool alreadyRunning=process && QueryFullProcessImageNameW(process,0,path,&length) && !_wcsicmp(path,java.c_str());
            if(process) CloseHandle(process);
            if(alreadyRunning) { CloseHandle(snapshot); return 0; }
        } while(Process32NextW(snapshot,&entry));
        CloseHandle(snapshot);
    }
    const auto requestedRoot=std::filesystem::path(local)/L"MinecraftGTAV";
    std::filesystem::create_directories(requestedRoot);
    const auto root=realPath(requestedRoot);
    const auto data=root/L"prism";
    const auto instance=data/L"instances"/L"minecraft-gta-v";
    std::filesystem::create_directories(instance/L".minecraft"/L"mods");
    const auto launcherConfig=data/L"prismlauncher.cfg";
    if(!std::filesystem::exists(launcherConfig)) {
        std::ofstream defaults(launcherConfig);
        defaults<<"[General]\nLanguage=en_US\nIgnoreJavaWizard=true\nAutomaticJavaDownload=false\nAutomaticJavaSwitch=false\nUserAskedAboutAutomaticJavaDownload=true\nPastebinURL=\nApplicationTheme=dark\nIconTheme=pe_colored\nJavaPath="<<(install/L"java"/L"bin"/L"javaw.exe").generic_string()<<"\n";
    }
    for (const auto& entry:std::filesystem::directory_iterator(install/L"minecraft"/L"mods"))
        std::filesystem::copy_file(entry.path(),instance/L".minecraft"/L"mods"/entry.path().filename(),std::filesystem::copy_options::overwrite_existing);
    std::filesystem::copy_file(install/L"minecraft"/L"mmc-pack.json",instance/L"mmc-pack.json",std::filesystem::copy_options::overwrite_existing);
    const auto cfg=instance/L"instance.cfg";
    // Preserve account association and player choices, while replacing this mod's required settings.
    std::string general,other,line,section;
    { std::ifstream input(cfg); while(std::getline(input,line)) {
        if(!line.empty() && line.back()=='\r') line.pop_back();
        if(!line.empty() && line.front()=='[') { section=line; if(section!="[General]") other+=line+'\n'; continue; }
        if(!section.empty() && section!="[General]") { other+=line+'\n'; continue; }
        auto equal=line.find('='); auto key=line.substr(0,equal);
        if(key=="JavaPath"||key=="OverrideJavaLocation"||key=="AutomaticJava"||key=="OverrideJavaArgs"||key=="JvmArgs"||key=="OverrideMemory"||key=="MaxMemAlloc"||key=="MinMemAlloc") continue;
        general+=line+'\n';
    }}
    std::ofstream output(cfg);
    output<<"[General]\n";
    if(general.empty()) output<<"ConfigVersion=1.3\nInstanceType=OneSix\nname=Minecraft GTA V\niconKey=default\n";
    else output<<general;
    output<<"OverrideJavaLocation=true\nAutomaticJava=false\nJavaPath="<<(install/L"java"/L"bin"/L"javaw.exe").generic_string()<<"\n";
    output<<"OverrideJavaArgs=true\nJvmArgs=--enable-native-access=ALL-UNNAMED -Dpassthrough.maxFps=60 -Dpassthrough.ownSkin=true\nOverrideMemory=true\nMinMemAlloc=512\nMaxMemAlloc=4096\n";
    output<<other;
    output.close();
    const auto launcher=install/L"launcher"/L"prismlauncher.exe";
    const std::wstring arguments=L"-d \""+data.wstring()+L"\" -l minecraft-gta-v --show-window";
    const auto result=reinterpret_cast<INT_PTR>(ShellExecuteW(nullptr,L"open",launcher.c_str(),arguments.c_str(),install.c_str(),SW_SHOWNORMAL));
    if(result<=32) { MessageBoxW(nullptr,L"The bundled Minecraft launcher could not start.",L"Minecraft GTA V",MB_OK); return 1; }
    return 0;
}
