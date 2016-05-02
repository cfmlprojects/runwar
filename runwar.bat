@echo off
SET "DIR=%~dp0%"
set CFDISTRO_HOME=%DIR%build\cfdistro
set FILE_URL="http://cfmlprojects.org/artifacts/cfdistro/latest/cfdistro.zip"
set FILE_DEST=%CFDISTRO_HOME%\cfdistro.zip
set JRE_ZIP=%DIR%jre.zip
set JRE_DIR=%DIR%jre
set buildfile=build\build.xml
set ANT_HOME=%CFDISTRO_HOME%\ant
set ANT_CMD=%CFDISTRO_HOME%\ant\bin\ant.bat
if not exist "%CFDISTRO_HOME%" (
  mkdir "%CFDISTRO_HOME%"
  echo Downloading with powershell: %FILE_URL% to %FILE_DEST%
  powershell.exe -command "$webclient = New-Object System.Net.WebClient; $url = \"%FILE_URL%\"; $file = \"%FILE_DEST%\"; $webclient.DownloadFile($url,$file);"
  echo Expanding with powershell to: %CFDISTRO_HOME%
  powershell -command "$shell_app=new-object -com shell.application; $zip_file = $shell_app.namespace(\"%FILE_DEST%\"); $destination = $shell_app.namespace(\"%CFDISTRO_HOME%\"); $destination.Copyhere($zip_file.items())"
  del %FILE_DEST%
)
if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal

set _JAVACMD=%JAVACMD%
if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto hasjava
:noJavaHome
if exist "%JRE_DIR%\bin\java.exe" (
  set JAVA_HOME=%JRE_DIR%
  set _JAVACMD=%JRE_DIR%\bin\java.exe
  goto hasjava
)

if "%_JAVACMD%" == "" (
  for %%X in (java.exe) do (set FOUND=%%~$PATH:X)
  if not defined FOUND goto nojava
)
:noJava
set downloadJRE=
set /p downloadJRE=  Download JRE? [Y]/n:
if /I '%downloadJRE%'=='n' goto hasjava
reg Query "HKLM\Hardware\Description\System\CentralProcessor\0" | find /i "x86" > NUL && set BITTYPE=32 || set BITTYPE=64
if %BITTYPE%==32 echo This is a 32bit operating system
if %BITTYPE%==64 echo This is a 64bit operating system
mkdir "%JRE_DIR%"
set JRE_URL="http://cfmlprojects.org/artifacts/oracle/jre/latest-win%BITTYPE%.zip"
echo Downloading with powershell: %JRE_URL%
echo   to %JRE_ZIP%
powershell.exe -command "$webclient = New-Object System.Net.WebClient; $url = \"%JRE_URL%\"; $file = \"%JRE_ZIP%\"; $webclient.DownloadFile($url,$file);"
echo Expanding with powershell to: %JRE_DIR%
powershell -command "$shell_app=new-object -com shell.application; $zip_file = $shell_app.namespace(\"%JRE_ZIP%\"); if (!(Test-Path \"%JRE_DIR%\")) { mkdir %JRE_DIR% }; $destination = $shell_app.namespace(\"%JRE_DIR%\"); $destination.Copyhere($zip_file.items())"
del %JRE_ZIP%
set JAVA_HOME=%JRE_DIR%
set _JAVACMD=%JRE_DIR%\bin\java.exe

:hasjava
if "%1" == "" goto MENU
set args=%1
SHIFT
:Loop
IF "%1" == "" GOTO Continue
SET args=%args% -D%1%
SHIFT
IF "%1" == "" GOTO Continue
SET args=%args%=%1%
SHIFT
GOTO Loop
:Continue
if not exist %buildfile% (
	set buildfile="%CFDISTRO_HOME%\build.xml"
)
call "%ANT_CMD%" -nouserlib -f %buildfile% %args%
goto end
:MENU
cls
echo.
echo       releng menu
REM echo       usage: releng.bat [start|stop|{target}]
echo.
echo       1. Start server and open browser
echo       2. Stop server
echo       3. List available targets
echo       4. Update project
echo       5. Run Target
echo       6. Quit
echo.
set choice=
set /p choice=      Enter option 1, 2, 3, 4, 5 or 6 :
echo.
if not '%choice%'=='' set choice=%choice:~0,1%
if '%choice%'=='1' goto startServer
if '%choice%'=='2' goto stopServer
if '%choice%'=='3' goto listTargets
if '%choice%'=='4' goto updateProject
if '%choice%'=='5' goto runTarget
if '%choice%'=='6' goto end
::
echo.
echo.
echo "%choice%" is not a valid option - try again
echo.
pause
goto MENU
::
:startServer
cls
call "%ANT_CMD%" -nouserlib -f %buildfile% build.start.launch
echo to stop the server, run this again or run: mapigator-releng.bat stop
goto end
::
:stopServer
call "%ANT_CMD%" -nouserlib -f %buildfile% server.stop
goto end
::
:listTargets
call "%ANT_CMD%" -nouserlib -f %buildfile% help
echo       press any key ...
pause > nul
goto MENU
::
:updateProject
call "%ANT_CMD%" -nouserlib -f %buildfile% project.update
echo       press any key ...
pause > nul
goto MENU
::
:runTarget
set target=
set /p target=      Enter target name:
if not "%target%"=="" call %0 %target%
echo       press any key ...
pause > nul
goto MENU
::
:end
set choice=
echo       press any key ...
pause
REM EXIT
			
