#!/bin/sh
# Biên dịch bằng module jdk.compiler (dùng khi chỉ có JRE) hoặc thay bằng: javac -d out *.java
rm -rf out && mkdir out
java -m jdk.compiler/com.sun.tools.javac.Main -d out *.java
