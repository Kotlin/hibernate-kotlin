package org.hibernate.type.descriptor.kotlin

import org.hibernate.type.descriptor.java.JavaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.Serializable

/**
 * Adopted from Hibernate's [org.hibernate.orm.test.mapping.type.java.AbstractDescriptorTest]
 */
abstract class AbstractDescriptorTest<T : Any>(
    private val typeDescriptor: JavaType<T>,
) {
    protected data class Data<T>(
        val original: T,
        val copy: T,
        val different: T,
    )

    protected abstract val testData: Data<T>

    protected abstract val shouldBeMutable: Boolean

    @Test
    fun testEquality() {
        assertTrue(typeDescriptor.areEqual(testData.original, testData.original))
        assertTrue(typeDescriptor.areEqual(testData.original, testData.copy))
        assertFalse(typeDescriptor.areEqual(testData.original, testData.different))
    }

    @Test
    fun testExternalization() {
        val externalized = typeDescriptor.toString(testData.original)
        val consumed = typeDescriptor.fromString(externalized)

        assertTrue(typeDescriptor.areEqual(testData.original, consumed))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun testPassThrough() {
        assertTrue(typeDescriptor.areEqual(testData.original, typeDescriptor.wrap(testData.original, null)))
        assertTrue(
            typeDescriptor.areEqual(
                testData.original,
                typeDescriptor.unwrap(
                    testData.original,
                    typeDescriptor.javaTypeClass as Class<T?>,
                    null,
                ),
            ),
        )
    }

    @Test
    fun testMutabilityPlan() {
        val mutabilityPlan = typeDescriptor.mutabilityPlan
        assertEquals(shouldBeMutable, mutabilityPlan.isMutable)

        val copy = mutabilityPlan.deepCopy(testData.copy)
        assertTrue(typeDescriptor.areEqual(copy, testData.copy))
        if (!shouldBeMutable) {
            assertSame(copy, testData.copy)
        }

        val cached = mutabilityPlan.disassemble(testData.copy, null)
        // extra `is Serializable` is added because kotlin.time.Duration isn't Serializable class and disassembled with different identity
        if (!shouldBeMutable && testData.copy is Serializable) {
            assertSame(cached, testData.copy)
        }
        val reassembled = mutabilityPlan.assemble(cached, null)
        assertTrue(typeDescriptor.areEqual(testData.original, reassembled))
    }
}
