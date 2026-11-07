#!/bin/sh
# Compile the whole project into the out/ directory (Linux / macOS / Git Bash).
# With a full JDK you can also run:
#   javac -encoding UTF-8 -d out $(find src -name '*.java')
rm -rf out && mkdir out
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d out $(find src -name '*.java')
