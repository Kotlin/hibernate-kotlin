# hibernate-kotlin

Hibernate type mappers for Kotlin standard library types.

The library provides type mappers for Kotlin/JVM types from the standard library that are not directly mapped
to Java types, or otherwise are not supported by Hibernate.

### Supported types
- [kotlin.time.Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/)
- [kotlin.time.Instant](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-instant/)
- [kotlin.uuid.Uuid](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/)

### Requirements

- Kotlin: `2.4.20+`
- Hibernate: `8.0.0+`
- Java: `8+`

### Applying to your project

If you use Maven:
```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-kotlin</artifactId>
    <version>8.0.0-SNAPSHOT</version>
</dependency>
```

If you use Gradle:
```kotlin
dependencies {
    implementation("org.hibernate.orm:hibernate-kotlin:8.0.0-SNAPSHOT")
}
```
