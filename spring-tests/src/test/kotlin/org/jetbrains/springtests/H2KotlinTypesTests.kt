package org.jetbrains.springtests

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.context.ActiveProfiles
import java.sql.SQLException
import kotlin.reflect.KMutableProperty1
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@ActiveProfiles("h2")
class H2KotlinTypesTests : KotlinTypesTests() {
    override val sqlite: Boolean = false

    @ParameterizedTest
    @EnumSource(value = DurationColumn::class, names = ["DEFAULT", "NUMERIC", "DECIMAL"])
    fun testDurationNumericFullFiniteRange(column: DurationColumn) {
        val maximum = (Long.MAX_VALUE / 2 - 1).milliseconds
        assertDurationRoundTrip(column, maximum)
        assertDurationRoundTrip(column, -maximum)
    }

    @ParameterizedTest
    @MethodSource("numericBoundaryOverflows")
    fun testDurationNumericReadBoundaryOverflow(column: DurationColumn, nanoseconds: String) {
        assertNumericDurationReadOverflow(column, nanoseconds)
    }

    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("narrowNumericInfinityValues")
    fun testDurationInfinityRejectedByNarrowNumericColumn(column: NarrowDurationColumn, negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val error = assertThrows(RuntimeException::class.java) {
            entityManager.persist(NarrowDurationColumns().apply { column.property.set(this, duration) })
            entityManager.flush()
        }
        val sqlException = generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<SQLException>()
            .firstOrNull()
        assertNotNull(sqlException, "Expected the database to reject the 28-digit infinity sentinel")
        assertTrue(sqlException!!.sqlState.startsWith("22"), "Unexpected SQL state: ${sqlException.sqlState}")
    }

    companion object {
        @JvmStatic
        fun numericBoundaryOverflows(): List<Arguments> =
            listOf(DurationColumn.DEFAULT, DurationColumn.NUMERIC, DurationColumn.DECIMAL).flatMap { column ->
                // H2 preserves the last nanosecond, unlike SQLite's REAL representation.
                listOf(
                    "4611686018427387902000001", // Maximum finite Duration + 1 ns.
                    "4611686018427387902999999", // Still rounds down to the maximum finite millisecond.
                    "4611686018427387903000000", // Maximum finite Duration + 1 ms.
                    "9223461171522139999999999999", // Immediately below the infinity sentinel.
                    "9223461171522140000000000001", // Immediately above the infinity sentinel.
                ).flatMap { nanos ->
                    listOf(Arguments.of(column, nanos), Arguments.of(column, "-$nanos"))
                }
            }

        @JvmStatic
        fun narrowNumericInfinityValues(): List<Arguments> = NarrowDurationColumn.entries.flatMap { column ->
            listOf(false, true).map { negative -> Arguments.of(column, negative) }
        }
    }
}

enum class NarrowDurationColumn(val property: KMutableProperty1<NarrowDurationColumns, Duration?>) {
    DEFAULT(NarrowDurationColumns::durationDefault),
    NUMERIC(NarrowDurationColumns::durationNumeric),
    DECIMAL(NarrowDurationColumns::durationDecimal),
}
