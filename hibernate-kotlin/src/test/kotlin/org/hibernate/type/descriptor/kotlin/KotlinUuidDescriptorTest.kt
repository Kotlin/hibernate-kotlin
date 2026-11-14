package org.hibernate.type.descriptor.kotlin

import org.hibernate.type.descriptor.java.UUIDJavaType
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.uuid.Uuid

@Tag("hibernate8")
class KotlinUuidDescriptorTest : AbstractDescriptorTest<Uuid>(KotlinUuidJavaType.INSTANCE) {
    override val testData = Data(
        original = Uuid.parse("01234567-89ab-cdef-0123-456789abcdef"),
        copy = Uuid.parse("01234567-89ab-cdef-0123-456789abcdef"),
        different = Uuid.parse("fedcba98-7654-3210-89ab-cdef01234567"),
    )

    override val shouldBeMutable: Boolean = false

    @Test
    fun testByteArrayRepresentationMatchesJavaUuid() {
        val value = "01234567-89ab-cdef-fedc-ba9876543210"

        val javaBytes = UUIDJavaType.INSTANCE.unwrap(
            UUID.fromString(value),
            ByteArray::class.java,
            null,
        )
        val kotlinBytes = KotlinUuidJavaType.INSTANCE.unwrap(
            Uuid.parse(value),
            ByteArray::class.java,
            null,
        )

        assertArrayEquals(javaBytes, kotlinBytes)
    }
}
