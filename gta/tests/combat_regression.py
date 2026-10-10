"""Generate a native-mocked harness from the actual production combat functions."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / 'src/script.cpp').read_text()
def function(signature):
    start = source.index(signature)
    opening = source.index('{', start)
    depth = 1
    end = opening + 1
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end]

prefix = r'''
#include <algorithm>
#include <cassert>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <cstdarg>
#include <map>
#include <set>
#include <string>
#include <vector>
using Entity=int; using Ped=int; using Vehicle=int; using BOOL=int;
constexpr int TRUE=1,FALSE=0;
struct Vector3 {float x,y,z;};
struct Projectile {float x,y,z; bool firework,seen,resolved=false;};
std::map<int,Projectile> g_projectiles;
std::set<int> g_doublePeds, g_pedsSent;
float g_mobHitScale=12;
int g_statHits=0;
float g_yOffset=10;
std::map<int,Vector3> positions;
std::vector<int> cars, destroyed;
int victim=0, damage=0, blasts=0, messages=0;
struct Hit {int entity; Vector3 end;};
std::vector<Hit> hits;
int worldGetAllVehicles(int* out,int cap) {int n=0; for(int v:cars) if(n<cap)out[n++]=v; return n;}
namespace natives {
bool DoesEntityExist(int e){return positions.count(e);}
int GetEntityHealth(int){return 100;}
Vector3 GetEntityCoords(int e,BOOL){return positions[e];}
void ExplodeVehicle(int e){destroyed.push_back(e);}
int traceFlags=0;
int StartShapeTestLosProbe(float,float,float,float,float,float,int flags,int){traceFlags=flags;return 1;}
int GetShapeTestResult(int,BOOL*hit,Vector3*end,Vector3*,Entity*e){
  *hit=!hits.empty(); if(*hit){*e=hits.front().entity;*end=hits.front().end;hits.erase(hits.begin());if(*e>4 && !(traceFlags & 4))*hit=false;}return 2;
}
int GetEntityType(int e){return e==2 || e==3 ? 2 : 1;}
void AddExplosion(float,float,float,int,float,BOOL,BOOL,float,BOOL){++blasts;}
void ApplyDamageToPed(int e,int d){victim=e;damage+=d;}
bool IsPedDeadOrDying(int){return false;}
void StartEntityFire(int){}
void SetPedToRagdoll(int,int){}
void ApplyForceToEntity(int,float,float,float){}
void ShootSingleBulletBetweenCoords(float,float,float,float,float,float,int,int,int){}
}
void shake_from(float,float,float,float){}
void sendf(const char*,...){++messages;}
'''
tests = r'''
int main(){
 positions={{1,{0,0,0}},{2,{0,0,0}},{3,{7,0,0}},{4,{20,0,0}}};
 cars={2,3,4};
 explode_cars(0,0,0,6); assert((destroyed==std::vector<int>{2}));
 destroyed.clear(); explode_cars(0,0,0,8); assert((destroyed==std::vector<int>{2,3}));
 destroyed.clear(); explode_cars(0,0,0,6,4); assert((destroyed==std::vector<int>{2,4}));
 // Arrow/NPC hits are handled by Minecraft proxies, never a second native trace.
 hits={{5,{1,0,0}}}; Projectile arrow{0,0,0,false,true};
 assert(!projectile_segment(1,40,arrow,2,0,0)); assert(victim==0 && damage==0);
 positions[5]={1,0,0}; g_pedsSent={5};
 mobhit_message(1,R"({"t":"mobhit","h":5,"d":6,"from":[1,10,0],"k":"arrow"})");
 assert(victim==5 && damage==72);
 victim=0; mobhit_message(1,R"({"t":"mobhit","h":7,"d":6,"k":"arrow"})"); assert(victim==0);
 // Minecraft mob doubles are ignored by GTA so vanilla Minecraft owns their arrow damage.
 g_doublePeds={6}; hits={{6,{0.1f,0,0}}}; victim=0;
 assert(!projectile_segment(1,41,arrow,0.2f,0,0)); assert(victim==0);
 // A rocket directly hits a vehicle and explodes only once, even when the next packet still contains it.
 hits={{2,{1,0,0}}}; destroyed.clear(); messages=blasts=0;
 const std::string packet=R"({"t":"proj","p":[[42,"firework",2,10,0]]})";
 projectiles_message(1,packet); assert(blasts==1 && messages==1 && !destroyed.empty());
 hits={{2,{1,0,0}}}; projectiles_message(1,packet); assert(blasts==1 && messages==1);
 projectiles_message(1,R"({"t":"proj","p":[]})"); assert(g_projectiles.empty());
 puts("Combat regression checks passed: rocket hits, TNT radius, NPC arrows, mob isolation, duplicate suppression and cleanup.");
}
'''
out=root/'build/combat_regression.cpp'
out.parent.mkdir(exist_ok=True)
out.write_text(prefix+'\n'.join(function(s) for s in ['const char *json_value(', 'double json_num(', 'std::string json_str(', 'void explode_cars(', 'bool projectile_segment(', 'void projectiles_message(', 'void mobhit_message('])+tests)
