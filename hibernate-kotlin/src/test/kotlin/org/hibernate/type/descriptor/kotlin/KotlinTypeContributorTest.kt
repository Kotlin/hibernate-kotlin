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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.lang.reflect.Type
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

class KotlinTypeContributorTest {
    @Test
    fun testReadsHibernateVersion() {
        assertNotNull(KotlinTypeContributor().readHibernateVersion())
    }

    @ParameterizedTest
    @ValueSource(strings = ["6.6.29", "6.6.29.Final", "6.6.29-SNAPSHOT", "6.6.29.CR1"])
    fun testParsesHibernateVersion(version: String) {
        val parsed = requireNotNull(KotlinTypeContributor().parseHibernateVersion(version))
        assertEquals(Triple(6, 6, 29), parsed)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = [
        "[WORKING]", "6.6", "6.6.x", "6.6.29invalid", "prefix6.6.29", "-6.6.29",
        "2147483648.6.29", "6.2147483648.29", "6.6.2147483648",
    ])
    fun testReturnsNullForUnknownHibernateVersion(version: String?) {
        assertNull(KotlinTypeContributor().parseHibernateVersion(version))
    }

    @Test
    @Tag("hibernate8")
    fun testRegisteringHibernate8() {
        assertRegistering(KotlinUuidJavaType.INSTANCE)
    }

    @Test
    @Tag("before8")
    fun testRegisteringHibernateBefore8() {
        assertRegistering(KotlinUuidNoIdJavaType.INSTANCE)
    }

    private fun assertRegistering(expectedUuidType: JavaType<Uuid>) {
        val registry = StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.DIALECT, H2Dialect::class.java.name)
            .applySetting(AvailableSettings.ALLOW_METADATA_ON_BOOT, false)
            .build()

        try {
            val metadata =
                MetadataSources(registry).buildMetadata() as MetadataImplementor
            val javaTypes = metadata.typeConfiguration.javaTypeRegistry

            assertSame(
                expectedUuidType,
                javaTypes.findDescriptor(Uuid::class.java as Type),
            )
            assertSame(
                KotlinDurationJavaType.INSTANCE,
                javaTypes.findDescriptor(Duration::class.java as Type),
            )
            assertSame(
                KotlinInstantJavaType.INSTANCE,
                javaTypes.findDescriptor(Instant::class.java as Type),
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
