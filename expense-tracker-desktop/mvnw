#!/bin/sh
# Maven wrapper bash script
DIR="$(cd "$(dirname "$0")" && pwd)"
export MAVEN_VERSION="3.9.9"
MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION/apache-maven-$MAVEN_VERSION"

if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
    echo "Downloading Maven $MAVEN_VERSION..."
    mkdir -p "$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip" -o "$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION/maven.zip"
    unzip -q "$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION/maven.zip" -d "$HOME/.m2/wrapper/dists/apache-maven-$MAVEN_VERSION"
    chmod +x "$MAVEN_HOME/bin/mvn"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
