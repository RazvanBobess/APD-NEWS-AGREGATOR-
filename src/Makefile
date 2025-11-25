JAVAC = javac
JAVA = java
SOURCES = ./main/*.java
CLASSES = ./main/*.class

build: main
	$(JAVAC) $(SOURCES)

run: build
	$(JAVA) Main

clean:
	rm -f $(CLASSES)