JAVAC = javac -encoding UTF-8
JAVA = java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8

.PHONY: build run test clean

build:
	$(JAVAC) -d build src/*.java

run: build
	$(JAVA) -cp build Main $(ARGS)

test:
	$(JAVAC) -d build src/*.java tests/*.java
	$(JAVA) -cp build AllTests

clean:
	rm -rf build
