.PHONY: build run clean

build:
	javac -cp "./jackson-core-3.0.0.jar:./jackson-databind-3.0.3.jar:./jackson-annotations-3.0-rc5.jar" ./main/java/*.java -d ../target

run:
	java -cp "./jackson-core-3.0.0.jar:./jackson-databind-3.0.3.jar:./jackson-annotations-3.0-rc5.jar:../target" Tema1 $(ARGS)

clean:
	rm -rf ../target
	rm -rf *.txt