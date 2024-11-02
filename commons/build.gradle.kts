group = "com.itangcent"
version = "1.8.2"

repositories {
    mavenCentral()
}

dependencies {

    // https://mvnrepository.com/artifact/org.apache.commons/commons-lang3
    implementation("org.apache.commons:commons-lang3:3.9")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.8.9")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")

    // https://mvnrepository.com/artifact/org.apache.groovy/groovy-jsr223
    testImplementation("org.apache.groovy:groovy-jsr223:4.0.0")

    // https://search.maven.org/artifact/org.mockito.kotlin/mockito-kotlin/3.2.0/jar
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-test-common
    implementation("org.jetbrains.kotlin:kotlin-test-common:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit_version"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.0")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}