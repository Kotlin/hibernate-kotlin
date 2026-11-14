import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

group = "org.hibernate.orm"
version = libs.versions.hibernate.get()

kotlin {
    explicitApi()
    jvmToolchain(21)

    compilerOptions {
        // Min JDK version supported by Hibernate
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}

java {
    withJavadocJar()
    withSourcesJar()

    targetCompatibility = JavaVersion.VERSION_17
    sourceCompatibility = JavaVersion.VERSION_17
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

val testSourceSet = sourceSets.test.get()

val testHibernate8 = tasks.register<Test>("testHibernate8") {
    group = "verification"
    description = "Runs tests against the current Hibernate 8."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    useJUnitPlatform {
        excludeTags("before8")
    }
}

val compatibilityVersions = mapOf(
    "60" to libs.versions.hibernate60.get(),
    "66" to libs.versions.hibernate66.get(),
    "70" to libs.versions.hibernate70.get(),
)

val compatibilityTests = compatibilityVersions.map { (suffix, hibernateVersion) ->
    val runtimeClasspath = configurations.create("hibernate${suffix}TestRuntimeClasspath") {
        isCanBeConsumed = false
        isCanBeResolved = true
        extendsFrom(configurations.testRuntimeClasspath.get())
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        }
        resolutionStrategy.eachDependency {
            if (requested.group == "org.hibernate.orm") {
                useVersion(hibernateVersion)
                because("All Hibernate ORM modules must use the compatibility test version")
            }
        }
    }

    tasks.register<Test>("testHibernate$suffix") {
        group = "verification"
        description = "Runs tests against Hibernate $hibernateVersion."
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = files(testSourceSet.output, sourceSets.main.get().output, runtimeClasspath)
        useJUnitPlatform {
            excludeTags("hibernate8")
        }
    }
}

tasks.test {
    // Only tests for the different versions
    enabled = false
    dependsOn(testHibernate8, compatibilityTests)
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
