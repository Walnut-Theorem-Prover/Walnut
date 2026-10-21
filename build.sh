#!/bin/bash

# to run the build with tests, add the "-t" option. For instance, instead of running "build.sh", run "build.sh -t".

SCRIPTNAME="walnut.sh"

set -e

if [[ $1 = "-t" ]]; then
	# run with tests
	echo "Building Walnut and running tests."
	./mvnw clean install -Pcode-coverage,fat-jar
	# Also compile the browser version, so a change that breaks it is caught locally.
	# (Needs only a JDK. Building the full site is web/scripts/build-site.sh.)
	echo "Compiling the browser version."
	./mvnw -f web/pom.xml clean install
else
	# If you want a fast build without tests, you can run:
	echo "Building Walnut. To run tests, add the -t flag to the command."
	./mvnw clean package -DskipTests -Pfat-jar
fi	

chmod +x $SCRIPTNAME
