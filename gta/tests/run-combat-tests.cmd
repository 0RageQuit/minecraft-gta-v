@echo off
setlocal
call "C:\Program Files (x86)\Microsoft Visual Studio\2022\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul
if errorlevel 1 exit /b 1
cl /nologo /EHsc /std:c++20 /Fe"build\combat_regression.exe" /Fo"build\combat_regression.obj" "build\combat_regression.cpp"
if errorlevel 1 exit /b 1
"build\combat_regression.exe"
