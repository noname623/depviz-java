@echo off
chcp 65001 >nul
javac -encoding UTF-8 -d build src\*.java || exit /b 1
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp build Main %*
