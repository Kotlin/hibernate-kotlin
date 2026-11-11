package org.hibernate.type.descriptor.kotlin

import org.hibernate.boot.MetadataSources
import org.hibernate.boot.model.TypeContributions
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.boot.spi.MetadataImplementor
import org.hibernate.cfg.AvailableSettings
import org.hibernate.dialect.H2Dialect
import org.hibernate.type.descriptor.java.JavaType
import org.hibernate.type.spi.TypeConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

class KotlinTypeContributorTest {
    @Test
    fun testRegistering() {
        val registry = StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.DIALECT, H2Dialect::class.java.name)
            .applySetting(AvailableSettings.ALLOW_METADATA_ON_BOOT, false)
            .build()

        try {
            val metadata =
                MetadataSources(registry).buildMetadata() as MetadataImplementor
            val javaTypes = metadata.typeConfiguration.javaTypeRegistry

            assertSame(
                KotlinUuidJavaType.INSTANCE,
                javaTypes.findDescriptor(Uuid::class.java),
            )
            assertSame(
                KotlinDurationJavaType.INSTANCE,
                javaTypes.findDescriptor(Duration::class.java),
            )
            assertSame(
                KotlinInstantJavaType.INSTANCE,
                javaTypes.findDescriptor(Instant::class.java),
            )
        } finally {
            StandardServiceRegistryBuilder.destroy(registry)
        }
    }

    @Test
    fun testDoesNotRegisterTypesMissingFromClasspath() {
        val types = listOf(
            Uuid::class.java,
            Duration::class.java,
            Instant::class.java,
        )

        types.forEach { type ->
            val contributions = RecordingTypeContributions()
            val classLoader = FilteringClassLoader(javaClass.classLoader, type.canonicalName)

            KotlinTypeContributor().contribute(contributions, classLoader)

            val registeredType = contributions.javaTypes.singleOrNull {
                it.javaTypeClass == type
            }
            assertNull(registeredType, "${type.canonicalName} must not be registered")
            assertEquals(2, contributions.javaTypes.size, "Other available types must be registered")
        }
    }

    private class RecordingTypeContributions : TypeContributions {
        val javaTypes = mutableListOf<JavaType<*>>()

        override fun getTypeConfiguration(): TypeConfiguration = TypeConfiguration()

        override fun contributeJavaType(descriptor: JavaType<*>) {
            javaTypes += descriptor
        }
    }

    private class FilteringClassLoader(
        parent: ClassLoader?,
        private val missingClassName: String,
    ) : ClassLoader(parent) {
        override fun loadClass(name: String, resolve: Boolean): Class<*> {
            if (name == missingClassName) {
                throw ClassNotFoundException(name)
            }
            return super.loadClass(name, resolve)
        }
    }
}
