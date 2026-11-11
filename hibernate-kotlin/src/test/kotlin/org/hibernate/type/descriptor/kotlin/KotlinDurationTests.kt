package org.hibernate.type.descriptor.kotlin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.sql.SQLException
import kotlin.reflect.KMutableProperty1
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class KotlinDurationTests : BaseSessionFactoryTest() {
    override val jdbcUrl: String = "jdbc:h2:mem:kotlin-duration-tests;DB_CLOSE_DELAY=-1"

    override val annotatedClasses: Array<Class<*>> = arrayOf(
        DurationColumnsEntity::class.java,
        NarrowDurationColumnsEntity::class.java,
    )

    @ParameterizedTest(name = "{0}: {1} ns")
    @MethodSource("finiteDurations")
    fun testColumnTypesWriteRead(column: DurationColumn, nanoseconds: Long) {
        assertDurationRoundTrip(column, nanoseconds.nanoseconds)
    }

    @Test
    fun testNullColumns() {
        val loaded = persistAndReload(DurationColumnsEntity(), DurationColumnsEntity::class.java) { it.id }
        DurationColumn.entries.forEach { assertNull(it.property.get(loaded), it.name) }
    }

    /**
     * Tests support for infinite durations for all columns excluding BIGINT.
     */
    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("infiniteDurations")
    fun testInfiniteWriteRead(column: DurationColumn, negative: Boolean) {
        assertDurationRoundTrip(column, if (negative) -Duration.INFINITE else Duration.INFINITE)
    }

    /**
     * Tests writing infinite durations to DOUBLE columns and reading it as Double and Float with expected value.
     */
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testDoubleInfiniteStorage(negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val row = persistAndReload(
            DurationColumnsEntity().apply { durationDouble = duration },
            DurationColumnsEntity::class.java,
        ) { it.id }

        val stored = inTransaction { session ->
            session.doReturningWork { connection ->
                connection.prepareStatement("select duration_double from kotlin_duration_columns where id = ?").use { statement ->
                    statement.setLong(1, requireNotNull(row.id))
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getDouble(1)
                    }
                }
            }
        }
        val marker = if (negative) -9.22346117152214e27 else 9.22346117152214e27
        assertEquals(marker, marker.toFloat().toDouble())
        assertEquals(marker, stored)
    }

    @ParameterizedTest
    @ValueSource(doubles = [5.0e24, -5.0e24, 9.6e27, -9.6e27])
    fun testDoubleReadOverflow(nanoseconds: Double) {
        assertReadOverflow("duration_double", nanoseconds)
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "PT9999999999998H",
        "-PT9999999999998H",
        "PT9999999999999999999999999H",
        "-PT9999999999999999999999999H",
    ])
    fun testFromStringOverflow(isoDuration: String) {
        val error = assertThrows(ArithmeticException::class.java) {
            KotlinDurationJavaType.INSTANCE.fromString(isoDuration)
        }
        assertTrue(error.message.orEmpty().contains("Duration overflow"))
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "PT9999999999998H",
        "-PT9999999999998H",
        "PT9999999999999999999999999H",
        "-PT9999999999999999999999999H",
    ])
    fun testVarcharReadOverflow(isoDuration: String) {
        assertReadOverflow("duration_varchar", isoDuration)
    }

    @ParameterizedTest
    @ValueSource(strings = ["PT9999999999999H", "-PT9999999999999H"])
    fun testVarcharReadInfinity(isoDuration: String) {
        val negative = isoDuration.startsWith('-')
        val id = if (negative) -10_002L else -10_001L
        val loaded = inTransaction { session ->
            session.createNativeMutationQuery(
                "insert into kotlin_duration_columns (id, duration_varchar) values (:id, :value)",
            ).setParameter("id", id)
                .setParameter("value", isoDuration)
                .execute()
            requireNotNull(session.find(DurationColumnsEntity::class.java, id))
        }

        assertEquals(if (negative) -Duration.INFINITE else Duration.INFINITE, loaded.durationVarchar)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testBigintRejectsInfinite(negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        assertBigintRejected(duration, "cant' be saved")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testBigintRejectsOverflow(negative: Boolean) {
        val duration = 1_000_000_000_000_000L.milliseconds
        assertBigintRejected(if (negative) -duration else duration, "too large")
    }

    @ParameterizedTest
    @EnumSource(value = DurationColumn::class, names = ["DEFAULT", "NUMERIC", "DECIMAL"])
    fun testNumericBeyondLongRange(column: DurationColumn) {
        val duration = 1_000_000_000_000_000L.milliseconds
        assertDurationRoundTrip(column, duration)
        assertDurationRoundTrip(column, -duration)
    }

    @ParameterizedTest
    @EnumSource(value = DurationColumn::class, names = ["DEFAULT", "NUMERIC", "DECIMAL"])
    fun testNumericFullFiniteRange(column: DurationColumn) {
        val maximum = (Long.MAX_VALUE / 2 - 1).milliseconds
        assertDurationRoundTrip(column, maximum)
        assertDurationRoundTrip(column, -maximum)
    }

    @ParameterizedTest
    @MethodSource("numericReadOverflows")
    fun testNumericReadOverflow(column: DurationColumn, nanoseconds: String) {
        assertReadOverflow(column.sqlName, BigDecimal(nanoseconds))
    }

    @ParameterizedTest
    @MethodSource("numericBoundaryOverflows")
    fun testNumericReadBoundaryOverflow(column: DurationColumn, nanoseconds: String) {
        assertReadOverflow(column.sqlName, BigDecimal(nanoseconds))
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testVarcharFullFiniteRange(negative: Boolean) {
        val duration = (Long.MAX_VALUE / 2 - 1).milliseconds
        assertDurationRoundTrip(DurationColumn.VARCHAR, if (negative) -duration else duration)
    }

    @Test
    fun testNumericStoresNanoseconds() {
        val row = persistAndReload(
            DurationColumnsEntity().apply { durationNumeric = -(12.seconds + 345.nanoseconds) },
            DurationColumnsEntity::class.java,
        ) { it.id }
        val stored = inTransaction { session ->
            session.doReturningWork { connection ->
                connection.prepareStatement("select duration_numeric from kotlin_duration_columns where id = ?").use { statement ->
                    statement.setLong(1, requireNotNull(row.id))
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getBigDecimal(1)
                    }
                }
            }
        }
        assertEquals(0, BigDecimal("-12000000345").compareTo(stored))
    }

    /**
     * Due to the specifics of [Duration] implementation, some values can't be represented in Duration.
     *
     * If the absolute value is greater than or equal to `4 611 686 018 427 000 000`, there are windows with a period of `1 000 000`.
     * Values inside each window are truncated toward zero to the nearest whole millisecond.
     */
    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testNumericReadPrecisionBoundary(negative: Boolean) {
        val maximumNanosecondValue = 4_611_686_018_426_999_999L
        val firstMillisecondValue = maximumNanosecondValue + 1
        val magnitudes = listOf(
            maximumNanosecondValue - 1,
            maximumNanosecondValue,
            firstMillisecondValue,
            firstMillisecondValue + 1,
            firstMillisecondValue + 999_999,
            firstMillisecondValue + 1_000_000,
        )
        val values = magnitudes.map { magnitude ->
            BigDecimal.valueOf(if (negative) -magnitude else magnitude)
        }
        val firstId = if (negative) -20_100L else -20_000L

        val durations = inTransaction { session ->
            values.forEachIndexed { index, value ->
                session.createNativeMutationQuery(
                    "insert into kotlin_duration_columns (id, duration_numeric) values (:id, :value)",
                ).setParameter("id", firstId - index)
                    .setParameter("value", value)
                    .execute()
            }
            session.clear()
            values.indices.map { index ->
                requireNotNull(
                    requireNotNull(
                        session.find(DurationColumnsEntity::class.java, firstId - index),
                    ).durationNumeric,
                )
            }
        }

        assertNotEquals(durations[0], durations[1])
        assertNotEquals(durations[1], durations[2])
        assertEquals(durations[2], durations[3])
        assertEquals(durations[2], durations[4])
        assertNotEquals(durations[4], durations[5])
    }

    @Test
    fun testHqlParameter() {
        val duration = 12.seconds + 345.nanoseconds
        val row = persistAndReload(
            DurationColumnsEntity().apply { durationNumeric = duration },
            DurationColumnsEntity::class.java,
        ) { it.id }

        val loadedIds = inTransaction { session ->
            session.createSelectionQuery(
                "select id from KotlinDurationColumns where durationNumeric = :value",
                Long::class.javaObjectType,
            ).setParameter("value", duration).resultList
        }
        assertTrue(loadedIds.contains(row.id))
    }

    @Test
    fun testDirtyChecking() {
        val row = persistAndReload(
            DurationColumnsEntity().apply { durationNumeric = 1.seconds },
            DurationColumnsEntity::class.java,
        ) { it.id }
        inTransaction { session ->
            val managed = requireNotNull(session.find(DurationColumnsEntity::class.java, requireNotNull(row.id)))
            managed.durationNumeric = (-1).nanoseconds
        }
        val loaded = inTransaction { session ->
            requireNotNull(session.find(DurationColumnsEntity::class.java, requireNotNull(row.id)))
        }
        assertEquals((-1).nanoseconds, loaded.durationNumeric)
    }

    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("narrowNumericInfinityValues")
    fun testInfinityRejectedByNarrowNumericColumn(column: NarrowDurationColumn, negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val error = assertThrows(RuntimeException::class.java) {
            inTransaction { session ->
                session.persist(NarrowDurationColumnsEntity().apply { column.property.set(this, duration) })
                session.flush()
            }
        }
        val sqlException = causes(error).filterIsInstance<SQLException>().firstOrNull()
        assertNotNull(sqlException, "Expected H2 to reject the 28-digit infinity sentinel")
        assertTrue(sqlException!!.sqlState.startsWith("22"), "Unexpected SQL state: ${sqlException.sqlState}")
    }

    private fun assertDurationRoundTrip(column: DurationColumn, duration: Duration) {
        val loaded = persistAndReload(
            DurationColumnsEntity().apply { column.property.set(this, duration) },
            DurationColumnsEntity::class.java,
        ) { it.id }
        assertEquals(duration, column.property.get(loaded), column.name)
    }

    private fun assertReadOverflow(sqlColumn: String, value: Any) {
        val error = assertThrows(RuntimeException::class.java) {
            inTransaction { session ->
                session.createNativeMutationQuery(
                    "insert into kotlin_duration_columns (id, $sqlColumn) values (-1, :value)",
                ).setParameter("value", value).execute()
                session.find(DurationColumnsEntity::class.java, -1L)
            }
        }
        val overflow = causes(error).filterIsInstance<ArithmeticException>().firstOrNull()
        assertNotNull(overflow, "Expected Duration overflow, got: $error")
        assertTrue(overflow!!.message.orEmpty().contains("Duration overflow"), "Unexpected error: $overflow")
    }

    private fun assertBigintRejected(duration: Duration, message: String) {
        val error = assertThrows(RuntimeException::class.java) {
            inTransaction { session ->
                session.persist(DurationColumnsEntity().apply { durationBigint = duration })
                session.flush()
            }
        }
        val illegalArgument = causes(error).filterIsInstance<IllegalArgumentException>().firstOrNull()
        assertNotNull(illegalArgument, "Expected IllegalArgumentException, got: $error")
        assertTrue(illegalArgument!!.message.orEmpty().contains(message), "Unexpected error: $illegalArgument")
    }

    private fun causes(error: Throwable): Sequence<Throwable> =
        generateSequence(error) { it.cause }

    private companion object {
        @JvmStatic
        fun finiteDurations(): List<Arguments> = DurationColumn.entries.flatMap { column ->
            listOf(0L, 1L, -1L, 999_999_999L, -999_999_999L, 12_000_000_345L, -12_000_000_345L)
                .map { Arguments.of(column, it) }
        }

        @JvmStatic
        fun infiniteDurations(): List<Arguments> = DurationColumn.entries
            .filter { it != DurationColumn.BIGINT }
            .flatMap { column -> listOf(false, true).map { Arguments.of(column, it) } }

        @JvmStatic
        fun numericReadOverflows(): List<Arguments> = numericArguments(
            "5000000000000000000000000",
            "9600000000000000000000000000",
        )

        @JvmStatic
        fun numericBoundaryOverflows(): List<Arguments> = numericArguments(
            "4611686018427387902000001",
            "4611686018427387902999999",
            "4611686018427387903000000",
            "9223461171522139999999999999",
            "9223461171522140000000000001",
        )

        private fun numericArguments(vararg values: String): List<Arguments> =
            listOf(DurationColumn.DEFAULT, DurationColumn.NUMERIC, DurationColumn.DECIMAL).flatMap { column ->
                values.flatMap { value ->
                    listOf(Arguments.of(column, value), Arguments.of(column, "-$value"))
                }
            }

        @JvmStatic
        fun narrowNumericInfinityValues(): List<Arguments> = NarrowDurationColumn.entries.flatMap { column ->
            listOf(false, true).map { negative -> Arguments.of(column, negative) }
        }
    }

    enum class DurationColumn(
        val sqlName: String,
        val property: KMutableProperty1<DurationColumnsEntity, Duration?>,
    ) {
        DEFAULT("duration_default", DurationColumnsEntity::durationDefault),
        NUMERIC("duration_numeric", DurationColumnsEntity::durationNumeric),
        DECIMAL("duration_decimal", DurationColumnsEntity::durationDecimal),
        VARCHAR("duration_varchar", DurationColumnsEntity::durationVarchar),
        BIGINT("duration_bigint", DurationColumnsEntity::durationBigint),
        DOUBLE("duration_double", DurationColumnsEntity::durationDouble),
    }

    enum class NarrowDurationColumn(
        val property: KMutableProperty1<NarrowDurationColumnsEntity, Duration?>,
    ) {
        DEFAULT(NarrowDurationColumnsEntity::durationDefault),
        NUMERIC(NarrowDurationColumnsEntity::durationNumeric),
        DECIMAL(NarrowDurationColumnsEntity::durationDecimal),
    }

    @Entity(name = "KotlinDurationColumns")
    @Table(name = "kotlin_duration_columns")
    open class DurationColumnsEntity {
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null

        @field:Column(name = "duration_default", precision = 28, scale = 0)
        open var durationDefault: Duration? = null

        @field:JdbcTypeCode(SqlTypes.NUMERIC)
        @field:Column(name = "duration_numeric", precision = 28, scale = 0)
        open var durationNumeric: Duration? = null

        @field:JdbcTypeCode(SqlTypes.DECIMAL)
        @field:Column(name = "duration_decimal", precision = 28, scale = 0)
        open var durationDecimal: Duration? = null

        @field:JdbcTypeCode(SqlTypes.VARCHAR)
        @field:Column(name = "duration_varchar")
        open var durationVarchar: Duration? = null

        @field:JdbcTypeCode(SqlTypes.BIGINT)
        @field:Column(name = "duration_bigint")
        open var durationBigint: Duration? = null

        @field:JdbcTypeCode(SqlTypes.DOUBLE)
        @field:Column(name = "duration_double", columnDefinition = "double precision")
        open var durationDouble: Duration? = null
    }

    @Entity
    @Table(name = "kotlin_narrow_duration_columns")
    open class NarrowDurationColumnsEntity {
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null

        @field:Column(precision = 27, scale = 0)
        open var durationDefault: Duration? = null

        @field:JdbcTypeCode(SqlTypes.NUMERIC)
        @field:Column(precision = 27, scale = 0)
        open var durationNumeric: Duration? = null

        @field:JdbcTypeCode(SqlTypes.DECIMAL)
        @field:Column(precision = 27, scale = 0)
        open var durationDecimal: Duration? = null
    }
}
