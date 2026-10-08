#pragma once
#include <windows.h>
#include <cstdint>
#include <cstring>
#include <stdexcept>
#include <type_traits>

// The RAGE native ABI, independent of the proprietary Script Hook V SDK.
using Hash = uint32_t;
using Entity = int;
using Ped = int;
using Player = int;
using Vehicle = int;
using Object = int;
using Cam = int;
using Blip = int;
using Void = void;
struct Vector3 { float x; uint32_t _x; float y; uint32_t _y; float z; uint32_t _z; };
static_assert(sizeof(Vector3) == 24);

namespace native_runtime {
struct Context {
    uint64_t* result;
    uint32_t count;
    uint64_t* arguments;
    int32_t vectorCount;
    Vector3* destinations[4];
    struct alignas(16) Source { float x, y, z, w; } sources[4];
};
static_assert(sizeof(Context) == 0x80);
using Keyboard = void (*)(DWORD, WORD, BYTE, BOOL, BOOL, BOOL, BOOL);
void call(uint64_t hash, Context& context);
void wait(DWORD milliseconds);
void start(HMODULE module, void (*script)(), Keyboard keyboard);
void stop();
}

template<class R, class... A> R invoke(uint64_t hash, A... values) {
    static_assert(sizeof...(A) <= 32);
    uint64_t args[32]{};
    uint64_t result[4]{};
    native_runtime::Context context{};
    context.result = result;
    context.arguments = args;
    unsigned index = 0;
    ([&] { static_assert(sizeof(A) <= 8); std::memcpy(&args[index++], &values, sizeof(A)); }(), ...);
    context.count = index;
    native_runtime::call(hash, context);
    if constexpr (!std::is_void_v<R>) {
        static_assert(sizeof(R) <= sizeof(result));
        R value{};
        std::memcpy(&value, result, sizeof(R));
        return value;
    }
}
inline void WAIT(DWORD milliseconds) { native_runtime::wait(milliseconds); }

// These interactions use actors around the player. Enumerate them through GTA's
// own native API instead of ScriptHookV's private pool-enumeration functions.
inline int worldGetAllPeds(int* handles, int capacity) {
    if (!handles || capacity <= 0) return 0;
    const int limit = capacity < 256 ? capacity : 256;
    int entries[514]{}; entries[0] = limit;
    int count = invoke<int>(0x23F8F5FC7E8C4A6B, invoke<Ped>(0xD80958FC74E988A6), entries, -1);
    count = count < 0 ? 0 : count > limit ? limit : count;
    for (int i=0;i<count;++i) handles[i]=entries[2+i*2];
    return count;
}
inline int worldGetAllVehicles(int* handles, int capacity) {
    if (!handles || capacity <= 0) return 0;
    const int limit = capacity < 256 ? capacity : 256;
    int entries[514]{}; entries[0] = limit;
    int count = invoke<int>(0xCFF869CBFA210D82, invoke<Ped>(0xD80958FC74E988A6), entries);
    count = count < 0 ? 0 : count > limit ? limit : count;
    for (int i=0;i<count;++i) handles[i]=entries[2+i*2];
    return count;
}
