#!/usr/bin/env bash

cd ..
gradle clean build

rm -r libs/*
mv commons/build/libs/commons*.jar libs/commons.jar
mv guice-action/build/libs/guice-action*.jar libs/guice-action.jar
mv intellij-idea/build/libs/intellij-idea*.jar libs/intellij-idea.jar
mv intellij-jvm/build/libs/intellij-jvm*.jar libs/intellij-jvm.jar