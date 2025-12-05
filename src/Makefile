.PHONY: build run clean

build:
	mvn compile

run:
	mvn exec:java -Dexec.mainClass="main.Main" -Dexec.args="$(ARGS)"

clean:
	mvn clean