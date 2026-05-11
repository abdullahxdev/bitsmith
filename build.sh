#!/bin/bash
set -e
cd "$(dirname "$0")"
rm -rf out
mkdir out
javac -d out src/aluviz/*.java
jar cfm ALUVisualizer.jar manifest.mf -C out .
echo "Built ALUVisualizer.jar"
echo "Run with: java -jar ALUVisualizer.jar"
