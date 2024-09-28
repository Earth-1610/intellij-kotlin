# IntelliJ-Kotlin

[![CI](https://github.com/Earth-1610/intellij-kotlin/actions/workflows/ci.yml/badge.svg)](https://github.com/Earth-1610/intellij-kotlin/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Earth-1610/intellij-kotlin/branch/master/graph/badge.svg?token=UAW8KECMLW)](https://codecov.io/gh/Earth-1610/intellij-kotlin)
![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-kotlin)

IntelliJ-Kotlin is a collection of utilities and enhancements designed to improve the Kotlin development experience
within IntelliJ IDEA. By addressing inconsistencies and providing additional features, this project aims to facilitate
seamless development and integration for Kotlin developers.

## Features

- **Enhanced Kotlin Support**: Addresses inconsistencies within the PSI (Program Structure Interface) between Java and
  Kotlin.
- **Modular Design**: Organized into multiple sub-modules, each serving a specific purpose.
- **Integration Utilities**: Provides utilities for configuration reading, PSI manipulation, and Guice integration.

## Modules

### commons

![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/commons)

Provides common utilities shared across multiple other modules in the project.

### guice-action

![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/guice-action)

Implements `KotlinAnAction`, providing an `ActionContext` to support Guice injections for plugin development.

### intellij-idea

![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-idea)

- **config**: Provides utility classes for reading configurations from different sources.
- **psi**: Contains utility methods for working with the Program Structure Interface (PSI).

### intellij-jvm

![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-jvm)

Addresses inconsistencies between Java and Kotlin within PSI, ensuring consistent behavior and representation within
IntelliJ IDEA.

### intellij-kotlin-support

![Maven Central](https://img.shields.io/maven-central/v/com.itangcent/intellij-kotlin-support)

Provides support for additional Kotlin features within IntelliJ IDEA.

## Installation

You can include the modules in your project by adding the appropriate dependencies from Maven Central:

```xml

<dependency>
    <groupId>com.itangcent</groupId>
    <artifactId>{module-name}</artifactId>
    <version>{latest-version}</version>
</dependency>
```

Replace `{module-name}` with the desired module (e.g., `commons`, `guice-action`) and `{latest-version}` with the latest
version available on Maven Central.

## Usage

Here's a basic example of how to use the `guice-action` module:

```kotlin
class MyAction : KotlinAnAction() {
    override fun actionPerformed(actionContext: ActionContext) {
        // Your code here
    }
}
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Commit your changes with clear messages.
4. Submit a pull request.

Please make sure to update tests as appropriate.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.