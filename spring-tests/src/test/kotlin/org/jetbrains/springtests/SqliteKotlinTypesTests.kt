package org.jetbrains.springtests

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import kotlin.time.Duration

@ActiveProfiles("sqlite")
class SqliteKotlinTypesTests : KotlinTypesTests() {
    override val sqlite: Boolean = true

    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("narrowNumericInfinityValues")
    fun testDurationInfinityAllowedByNarrowNumericColumn(column: NarrowDurationColumn, negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val loaded = persistAndReload(NarrowDurationColumns().apply { column.property.set(this, duration) })
        assertEquals(duration, column.property.get(loaded))
    }

    companion object {
        @JvmStatic
        fun narrowNumericInfinityValues(): List<Arguments> = NarrowDurationColumn.entries.flatMap { column ->
            listOf(false, true).map { negative -> Arguments.of(column, negative) }
        }
    }
}
