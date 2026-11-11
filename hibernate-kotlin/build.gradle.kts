import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "org.hibernate.orm"
version = libs.versions.hibernate.get()

kotlin {
    explicitApi()
    jvmToolchain(21)
}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly(libs.hibernate.core)
    compileOnly(kotlin("stdlib"))

    testImplementation(libs.hibernate.core)
    testImplementation(libs.hibernate.community.dialects)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.h2)
    testRuntimeOnly(libs.sqlite.jdbc)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
    }
}

repositories {
    mavenCentral()

    maven("https://central.sonatype.com/repository/maven-snapshots/") {
        mavenContent {
            snapshotsOnly()
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("Maven") {
            from(components["java"])

            artifactId = "hibernate-kotlin"

            pom {
                name = "Hibernate mappers for Kotlin types"
                description = "Hibernate type mappers for Kotlin standard library types"
                url = "https://github.com/Kotlin/hibernate-kotlin"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "TODO" // ???
                        name = "TODO" // ???
                    }
                }
            }
        }
    }
}
