.PHONY: build run clean

build:
	mvn compile

run: build
	mvn exec:java -Dexec.mainClass="main.Main"

clean:
	mvn clean