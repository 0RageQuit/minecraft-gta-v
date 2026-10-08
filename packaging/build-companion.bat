@echo off
setlocal
if not defined VCVARS for /f "usebackq delims=" %%i in (`"%ProgramFiles(x86)%\Microsoft Visual Studio\Installer\vswhere.exe" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath`) do set "VCVARS=%%i\VC\Auxiliary\Build\vcvars64.bat"
if not exist "%VCVARS%" (echo Install Visual Studio C++ x64 Build Tools or set VCVARS to vcvars64.bat& exit /b 1)
call "%VCVARS%" >nul || exit /b 1
cl /nologo /O2 /EHsc /std:c++20 /MT /DWIN32_LEAN_AND_MEAN /DNOMINMAX "%~dp0companion.cpp" /Fo"%~dp0companion.obj" /Fe"%~dp0MinecraftCompanion.exe" /link /SUBSYSTEM:WINDOWS shell32.lib user32.lib
exit /b %errorlevel%
