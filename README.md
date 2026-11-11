# hibernate-kotlin

Hibernate type mappers for Kotlin standard library types.

The library provides type mappers for Kotlin/JVM types from the standard library that are not directly mapped
to Java types, or otherwise are not supported by Hibernate.

### Supported types
- [kotlin.time.Duration](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-duration/)
- [kotlin.time.Instant](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-instant/)
- [kotlin.uuid.Uuid](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.uuid/-uuid/)

`Uuid` type can be used as an entity's identifier. Unless configured otherwise, 
the same generator Hibernate provides for the `java.util.UUID` class is used.

Alternatively, UUID generation algorithms provided by the Kotlin standard library can be used.
To configure it, annotate the `Uuid` property with `@KotlinUuidGenerator` annotation and select 
an appropriate generator version (by default, it is `V4`):
```kotlin
@Entity
open class MyEntity { 
    @field:Id 
    @field:KotlinUuidGenerator(version = KotlinUuidVersion.V7)
    open var id: Uuid? = null
}
```

### Requirements

- Kotlin: `2.3.0` or newer
- Hibernate: `8.0.0` or newer
- Java: `17` or newer

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
