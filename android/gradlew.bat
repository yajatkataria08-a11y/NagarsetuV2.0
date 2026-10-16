@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
@rem Set local scope
setlocal enabledelayedexpansion enableextensions
set DIRNAME=%~dp0
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
