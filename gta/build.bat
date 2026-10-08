@echo off
rem Builds MCPassthrough.asi for Ultimate ASI Loader, with no ScriptHookV dependency.
rem Uses the newest Visual Studio with the C++ x64 tools; set VCVARS to another vcvars64.bat to pick one.
setlocal
set HERE=%~dp0
if not defined VCVARS for /f "usebackq delims=" %%i in (`"%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VCVARS=%%i\VC\Auxiliary\Build\vcvars64.bat"
if not exist "%VCVARS%" (echo No Visual Studio with the C++ x64 tools was found. Install "Desktop development with C++" ^(Visual Studio 2022 or its Build Tools^), or set VCVARS to your vcvars64.bat& exit /b 1)
if not exist "%HERE%third_party\minhook\include\MinHook.h" (echo MinHook 1.3.4 is missing& exit /b 1)
if not exist "%HERE%third_party\reshade\reshade.hpp" (echo ReShade 6.8 add-on headers are missing& exit /b 1)
call "%VCVARS%" >nul || exit /b 1
if not exist "%HERE%build" mkdir "%HERE%build"
cl /nologo /LD /O2 /Zi /Fd"%HERE%build\MCPassthrough.pdb" /EHsc /std:c++20 /MT /W3 /DWIN32_LEAN_AND_MEAN /DNOMINMAX ^
  /I "%HERE%third_party\minhook\include" /I "%HERE%third_party\reshade" ^
  "%HERE%src\script.cpp" "%HERE%src\compositor.cpp" "%HERE%src\ws.cpp" "%HERE%src\native_runtime.cpp" ^
  "%HERE%third_party\minhook\src\buffer.c" "%HERE%third_party\minhook\src\hook.c" "%HERE%third_party\minhook\src\trampoline.c" "%HERE%third_party\minhook\src\hde\hde64.c" ^
  /Fo"%HERE%build\\" /Fe"%HERE%build\MCPassthrough.asi" ^
  /link /DEBUG:FULL /PDB:"%HERE%build\MCPassthrough.pdb" /PDBALTPATH:MCPassthrough.pdb ws2_32.lib user32.lib
exit /b %errorlevel%
