# intellij-kotlin

[![Build Status](https://travis-ci.com/Earth-1610/intellij-kotlin.svg?branch=master)](https://travis-ci.com/Earth-1610/intellij-kotlin)
[![CI](https://github.com/Earth-1610/intellij-kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/Earth-1610/intellij-kotlin/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Earth-1610/intellij-kotlin/branch/master/graph/badge.svg?token=UAW8KECMLW)](https://codecov.io/gh/Earth-1610/intellij-kotlin)

This project aims to enhance the Kotlin development experience within IntelliJ IDEA.
It consists of several sub-modules, each serving a distinct purpose and providing various utilities and features to facilitate seamless development

## sub modules

### commons ![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/commons)

- Provides common utilities shared across multiple other modules in the project.

### guice-action ![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/guice-action)

- Implements KotlinAnAction, providing an ActionContext to support Guice injections for the plugin develop.

### intellij-idea ![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-idea)

- config: Provides various utility classes for reading configurations from different
  sources.

- psi: Contains utility methods for working with Programmable Structure Interface (PSI).

### intellij-jvm ![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-jvm)

- Addresses inconsistencies between Java and Kotlin within PSI, ensuring consistent behavior or representation within
  IntelliJ IDEA.

### intellij-kotlin-support ![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-kotlin-support)

- Provides support for additional Kotlin features within IntelliJ IDEA.
