RUNTIME_CLASSES=jmeta/SyntaxError.class jmeta/ErrorObject.class jmeta/Memoize.class jmeta/State.class jmeta/Position.class
JMETA_CLASSES=jmeta/Utils.class jmeta/JMetaParser.class jmeta/JMetaCompiler.class

PREFIX=$(DESTDIR)/usr
LIBDIR=$(PREFIX)/share/java
BINDIR=$(PREFIX)/bin

default: jmeta-runtime.jar jmeta.jar

jmeta.jar: $(JMETA_CLASSES)
	jar cfe jmeta.jar jmeta.JMetaParser jmeta/*.class

jmeta-runtime.jar: $(RUNTIME_CLASSES) jmeta/BaseParser.class
	jar cf jmeta-runtime.jar $(RUNTIME_CLASSES) jmeta/BaseParser*.class

jmeta/JMetaParser.class: jmeta/JMetaParser.java jmeta/JMetaCompiler.class jmeta/Utils.class jmeta/BaseParser.class
	javac jmeta/JMetaParser.java

jmeta/JMetaCompiler.class: jmeta/BaseParser.class jmeta/JMetaCompiler.java
	javac jmeta/JMetaCompiler.java

jmeta/JMetaParser.java: boot/JMetaParser.jmeta boot/jmetaparser.js
	(cd boot; js boot-jmetaparser.js)

jmeta/JMetaCompiler.java: boot/JMetaCompiler.jmeta boot/jmetaparser.js
	(cd boot; js boot-jmetacompiler.js)

jmeta/BaseParser.class: jmeta/BaseParser.java jmeta/SyntaxError.class jmeta/ErrorObject.class
	javac jmeta/BaseParser.java

jmeta/SyntaxError.class: jmeta/SyntaxError.java
	javac jmeta/SyntaxError.java

jmeta/Utils.class: jmeta/Utils.java
	javac jmeta/Utils.java

jmeta/ErrorObject.class: jmeta/ErrorObject.java
	javac jmeta/ErrorObject.java


boot/jmetaparser.js boot/jmetaoptimizer.js boot/jmetacompiler.js: boot/jmetaparser.txt boot/jmetaoptimizer.txt boot/jmetacompiler.txt boot/boot.js
	(cd boot; js boot.js)


install: jmeta.jar jmeta-runtime.jar
	mkdir -p $(LIBDIR)/
	cp jmeta.jar $(LIBDIR)/
	cp jmeta-runtime.jar $(LIBDIR)/
	mkdir -p $(BINDIR)
	echo "#!/bin/sh\nexec java -jar $(LIBDIR)/jmeta.jar \"\$$@\"\n" > $(BINDIR)/jmeta
	chmod 755 $(BINDIR)/jmeta

uninstall:
	rm -f $(LIBDIR)/jmeta.jar $(LIBDIR)/jmeta-runtime.jar
	rm -f $(BINDIR)/jmeta

test: test-java test-left test-calc

test-java: jmeta-runtime.jar jmeta.jar
	java -jar jmeta.jar test/Java
	(cd test; javac -cp ../jmeta-runtime.jar Java.java)
	(cd test; java  -cp ../jmeta-runtime.jar:. Java)
test-left: jmeta-runtime.jar jmeta.jar
	java -jar jmeta.jar test/Left
	(cd test; javac -cp ../jmeta-runtime.jar Left.java)
	(cd test; java  -cp ../jmeta-runtime.jar:. Left)
test-calc: jmeta-runtime.jar jmeta.jar
	java -jar jmeta.jar test/Calculator
	(cd test; javac -cp ../jmeta-runtime.jar Calculator.java)
	(cd test; java  -cp ../jmeta-runtime.jar:. Calculator "4 * 3 - 2"; echo "should be: 10")

run: test

clean:
	rm -f *.jar
	rm -f jmeta/*.class jmeta/JMetaParser.java jmeta/JMetaCompiler.java
	rm -f boot/jmetaparser.js boot/jmetaoptimizer.js boot/jmetacompiler.js
	rm -f test/*.class test/Calculator.java test/Java.java test/Left.java

.PHONY: default clean test test-java test-left test-calc install uninstall
