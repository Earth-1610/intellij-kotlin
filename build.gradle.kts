plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("application")
    id("jacoco")
}

subprojects {
    plugins.apply {
        apply("java")
        apply("kotlin")
        apply("jacoco")
        apply("maven-publish")
        apply("org.gradle.java-library")
        apply("org.gradle.signing")
    }

    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "11"
            apiVersion = "1.8"
            languageVersion = "1.8"
        }
    }
}

group = "com.itangcent"
version = properties["project_version"]!!

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.8.9")

    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.9")


    // https://mvnrepository.com/artifact/commons-io/commons-io
    implementation("commons-io:commons-io:2.7")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-reflect
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")

    implementation("com.google.inject:guice:4.2.2")

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    testImplementation("org.junit.jupiter:junit-jupiter-params:${properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit_version"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.create("codeCoverageReport", JacocoReport::class) {
    executionData(
        fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec")
    )
    subprojects.forEach {
        sourceDirectories.from(it.file("src/main/kotlin"))
        classDirectories.from(it.file("build/classes/kotlin/main"))
    }
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("${buildDir}/reports/jacoco/report.xml").apply { parentFile.mkdirs() })
        html.required.set(false)
        csv.required.set(false)
    }

    generate()
}
