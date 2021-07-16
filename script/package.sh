#!/usr/bin/env bash

SOURCE="$0"
while [[ -h "$SOURCE"  ]]; do # resolve $SOURCE until the file is no longer a symlink
    scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
    SOURCE="$(readlink "$SOURCE")"
    [[ ${SOURCE} != /*  ]] && SOURCE="$scriptDir/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
basedir=${scriptDir%/*}
echo "baseDir:"${basedir}
cd ${basedir}

./gradlew clean build  -x test

if [ ! -d libs ]; then
        mkdir libs
else
        rm -r libs/*
fi
mv commons/build/libs/commons*.jar libs/commons.jar
mv guice-action/build/libs/guice-action*.jar libs/guice-action.jar
mv intellij-idea/build/libs/intellij-idea*.jar libs/intellij-idea.jar
mv intellij-idea-test/build/libs/intellij-idea-test*.jar libs/intellij-idea-test.jar
mv intellij-jvm/build/libs/intellij-jvm*.jar libs/intellij-jvm.jar
mv intellij-kotlin-support/build/libs/intellij-kotlin-support*.jar libs/intellij-kotlin-support.jar