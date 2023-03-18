import org.jetbrains.kotlin.konan.properties.loadProperties

group = "com.itangcent"
version = properties["project_version"]!!

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

tasks.register("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin.srcDirs)
}

val mvnDescription = """
Help for developing plugins for JetBrains products.
Common utils
"""

val publishProps = loadProperties(project.rootDir.path + ("/script/publish.properties"))


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.itangcent"
            artifactId = "commons"
            version = "${project.version}"

            from(components["kotlin"])
            artifact(tasks.getByName("sourcesJar"))

            pom {
                name.set("Intellij Kotlin(commons)")
                description.set(mvnDescription)
                url.set("https://github.com/Earth-1610/intellij-kotlin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("tangcent")
                        name.set("Tangcent")
                        email.set("pentatengcent@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Earth-1610/intellij-kotlin")
                    developerConnection.set("scm:git:https://github.com/Earth-1610/intellij-kotlin")
                    url.set("https://github.com/Earth-1610/intellij-kotlin")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = "${publishProps["sonatypeUsername"]}"
                password = "${publishProps["sonatypePassword"]}"
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"]).map {
        it.signatory(signatories.getDefaultSignatory(project))
    }
}