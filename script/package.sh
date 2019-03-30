#!/usr/bin/env bash

cd ..
./gradlew clean  makeJar
mv build/libs/intellij-kotlin.jar libs/