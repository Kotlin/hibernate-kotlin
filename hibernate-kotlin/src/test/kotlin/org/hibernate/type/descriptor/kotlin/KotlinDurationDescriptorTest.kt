package org.hibernate.type.descriptor.kotlin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for [KotlinDurationJavaType].
 */
class KotlinDurationDescriptorTest : AbstractDescriptorTest<Duration>(KotlinDurationJavaType.INSTANCE) {
    override val testData = Data(
        original = 3621.seconds + 256.nanoseconds,
        copy = 3621.seconds + 256.nanoseconds,
        different = 1621.seconds + 156.nanoseconds,
    )

    override val shouldBeMutable: Boolean = false

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testInfinityExternalization(negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val expected = if (negative) "-PT9999999999999H" else "PT9999999999999H"

        val externalized = KotlinDurationJavaType.INSTANCE.toString(duration)

        assertEquals(expected, externalized)
        assertEquals(duration, KotlinDurationJavaType.INSTANCE.fromString(externalized)!!)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testWrapDoubleInfinity(negative: Boolean) {
        val value = if (negative) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        val expected = if (negative) -Duration.INFINITE else Duration.INFINITE

        assertEquals(expected, KotlinDurationJavaType.INSTANCE.wrap(value, null)!!)
    }

    @Test
    fun testWrapDoubleNaN() {
        val error = assertThrows(ArithmeticException::class.java) {
            KotlinDurationJavaType.INSTANCE.wrap(Double.NaN, null)
        }

        assertTrue(error.message.orEmpty().contains("NaN"), "Unexpected error: $error")
    }
}
