#!/bin/sh
javac -encoding UTF-8 -d build src/*.java || exit 1
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -cp build Main "$@"
