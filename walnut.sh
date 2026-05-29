#!/bin/bash

# Invoked with 15GB memory. Change as you see fit.
./mvnw exec:java -Dexec.mainClass="Main.Prover" -Dexec.args="$*"

#Invoked with 15GB of memory
#MAVEN_OPTS="${MAVEN_OPTS} -Xmx15G" ./mvnw exec:java -Dexec.mainClass="Main.Prover" -Dexec.args="$*"

