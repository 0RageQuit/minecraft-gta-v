#!/bin/bash
# The current build vendors permitted build dependencies with license notices.
set -e
HERE=$(cd "$(dirname "$0")" && pwd)
for dependency in third_party/minhook/include/MinHook.h third_party/reshade/reshade.hpp; do
    [ -f "$HERE/$dependency" ] || { echo "Missing $dependency: use the complete source checkout or source release."; exit 1; }
done
echo 'MinHook source and ReShade headers are included. Build with gta/build.bat.'
echo 'ScriptHookV is not required. Release packaging is documented in docs/INSTALL.md.'
