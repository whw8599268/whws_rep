@echo off
echo Compressor JS and CSS?
pause
cd %~dp0

call compressor\compressor.bat ..\WEB-INF\views\themes\default\dyformdesign\design.core.js

echo.
echo Compressor Success
pause
echo on