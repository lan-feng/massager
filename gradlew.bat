@ECHO OFF
@REM ##########################################################################
@REM
@REM  Gradle startup script for Windows
@REM
@REM ##########################################################################

@REM Set local scope for the variables with windows NT shell
IF "%~1"=="" (SET CMD_LINE_ARGS=) ELSE (SET CMD_LINE_ARGS=%*)

SET SCRIPT_DIR=%~dp0
SET APP_BASE_NAME=%~n0
SET APP_HOME=%SCRIPT_DIR%

@REM Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
SET DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@REM Find java.exe
IF DEFINED JAVA_HOME GOTO findJavaFromJavaHome

SET JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
IF %ERRORLEVEL% EQU 0 GOTO init

ECHO.
ECHO ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
ECHO.
ECHO Please set the JAVA_HOME variable in your environment to match the
ECHO location of your Java installation.

GOTO fail

:findJavaFromJavaHome
SET JAVA_HOME=%JAVA_HOME:"=%
SET JAVA_EXE=%JAVA_HOME%\bin\java.exe

IF EXIST "%JAVA_EXE%" GOTO init

ECHO.
ECHO ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
ECHO.
ECHO Please set the JAVA_HOME variable in your environment to match the
ECHO location of your Java installation.

GOTO fail

:init
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

:execute
%JAVA_EXE% %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %CMD_LINE_ARGS%
GOTO end

:fail
REM Do not use EXIT /B to avoid confusing environment with an error level.
CMD /C EXIT /B 1

:end
REM End of file
