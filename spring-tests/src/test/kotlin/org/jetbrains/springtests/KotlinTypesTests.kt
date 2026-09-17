package org.jetbrains.springtests

import jakarta.persistence.EntityManager
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.type.descriptor.kotlin.KotlinUuidJavaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.reflect.KMutableProperty1
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

@SpringBootTest(classes = [App::class])
@Transactional
abstract class KotlinTypesTests {
    @Autowired
    protected lateinit var entityManager: EntityManager

    protected abstract val sqlite: Boolean

    @Test
    fun testAutomaticTypeRegistration() {
        val factory = entityManager.entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        val types = factory.typeConfiguration.javaTypeRegistry
        assertSame(KotlinUuidJavaType.INSTANCE, types.getDescriptor<Uuid>(Uuid::class.java))
        assertEquals("KotlinDurationJavaType", types.getDescriptor<Duration>(Duration::class.java).javaClass.simpleName)
    }

    @Test
    fun testUuidAutoGeneration() {
        val first = persistAndReload(AutoUuid())
        val second = persistAndReload(AutoUuid())
        assertNotNull(first.id)
        assertNotNull(second.id)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun testUuidExplicitGeneration() {
        val first = persistAndReload(ExplicitUuid())
        val second = persistAndReload(ExplicitUuid())
        assertNotNull(first.id)
        assertNotNull(second.id)
        assertNotEquals(first.id, second.id)
    }

    @Test
    fun testUuidGenerationForCharColumn() {
        val first = persistAndReload(CharUuid())
        val second = persistAndReload(CharUuid())
        assertNotNull(first.id)
        assertNotNull(second.id)
        assertNotEquals(first.id, second.id)
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = [
        "00000000-0000-0000-0000-000000000000",
        "01234567-89ab-cdef-0123-456789abcdef",
        "fedcba98-7654-3210-89ab-cdef01234567",
        "ffffffff-ffff-ffff-ffff-ffffffffffff",
    ])
    fun testUuidColumnTypesWriteRead(text: String?) {
        val uuid = text?.let(Uuid::parse)
        val loaded = persistAndReload(UuidColumns().apply {
            uuidDefault = uuid
            uuidChar = uuid
            uuidVarchar = uuid
            uuidBinary = uuid
        })
        assertEquals(uuid, loaded.uuidDefault)
        assertEquals(uuid, loaded.uuidChar)
        assertEquals(uuid, loaded.uuidVarchar)
        assertEquals(uuid, loaded.uuidBinary)
    }

    @ParameterizedTest(name = "{0}: {1} ns")
    @MethodSource("finiteDurations")
    fun testDurationColumnTypesWriteRead(column: DurationColumn, nanos: Long) {
        assertDurationRoundTrip(column, nanos.nanoseconds)
    }

    @Test
    fun testDurationNullColumns() {
        val loaded = persistAndReload(DurationColumns())
        DurationColumn.entries.forEach { assertNull(it.property.get(loaded), it.name) }
    }

    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("infiniteDurations")
    fun testDurationInfiniteWriteRead(column: DurationColumn, negative: Boolean) {
        assertDurationRoundTrip(column, if (negative) -Duration.INFINITE else Duration.INFINITE)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testDurationDoubleInfiniteStorage(negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val row = persistAndReload(DurationColumns().apply { durationDouble = duration })
        assertEquals(duration, row.durationDouble)

        val stored = entityManager.createNativeQuery(
            "select duration_double from duration_columns where id = :id", Double::class.javaObjectType,
        ).setParameter("id", row.id).singleResult as Double
        val marker = if (negative) -9.22346117152214e27 else 9.22346117152214e27
        assertEquals(marker, marker.toFloat().toDouble())
        assertEquals(marker, stored)
    }

    @ParameterizedTest
    @ValueSource(doubles = [5.0e24, -5.0e24, 9.6e27, -9.6e27])
    fun testDurationDoubleReadOverflow(nanoseconds: Double) {
        entityManager.createNativeQuery("insert into duration_columns (id, duration_double) values (-1, :value)")
            .setParameter("value", nanoseconds)
            .executeUpdate()
        entityManager.clear()

        val error = assertThrows(ArithmeticException::class.java) {
            entityManager.find(DurationColumns::class.java, -1L)
        }
        assertTrue(error.message.orEmpty().contains("Duration overflow"), "Unexpected error: $error")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "4611686018427387902000001",
        "-4611686018427387902000001",
        "9999999999999999999999999999999999999999",
        "-9999999999999999999999999999999999999999",
    ])
    fun testDurationFromStringOverflow(nanoseconds: String) {
        val factory = entityManager.entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
        val type = requireNotNull(factory.typeConfiguration.javaTypeRegistry.findDescriptor(Duration::class.java))
        val error = assertThrows(ArithmeticException::class.java) { type.fromString(nanoseconds) }
        assertTrue(error.message.orEmpty().contains("Duration overflow"), "Unexpected error: $error")
    }

    @ParameterizedTest
    @ValueSource(strings = ["PT9999999999998H", "-PT9999999999998H", "PT9999999999999999999999999H", "-PT9999999999999999999999999H"])
    fun testDurationVarcharReadOverflow(isoDuration: String) {
        entityManager.createNativeQuery("insert into duration_columns (id, duration_varchar) values (-1, :value)")
            .setParameter("value", isoDuration)
            .executeUpdate()
        entityManager.clear()

        val error = assertThrows(ArithmeticException::class.java) {
            entityManager.find(DurationColumns::class.java, -1L)
        }
        assertTrue(error.message.orEmpty().contains("Duration overflow"), "Unexpected error: $error")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testDurationBigintRejectsInfinite(negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        assertBigintRejected(duration, "cant' be saved")
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testDurationBigintRejectsOverflow(negative: Boolean) {
        val duration = 1_000_000_000_000_000L.milliseconds
        assertBigintRejected(if (negative) -duration else duration, "too large")
    }

    @ParameterizedTest
    @EnumSource(value = DurationColumn::class, names = ["DEFAULT", "NUMERIC", "DECIMAL"])
    fun testDurationNumericBeyondLongRange(column: DurationColumn) {
        // 10^21 ns exceeds Long but remains exact when SQLite uses REAL storage.
        val duration = 1_000_000_000_000_000L.milliseconds
        assertDurationRoundTrip(column, duration)
        assertDurationRoundTrip(column, -duration)
    }

    @ParameterizedTest
    @MethodSource("numericReadOverflows")
    fun testDurationNumericReadOverflow(column: DurationColumn, nanoseconds: String) {
        assertNumericDurationReadOverflow(column, nanoseconds)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun testDurationVarcharFullFiniteRange(negative: Boolean) {
        val duration = (Long.MAX_VALUE / 2 - 1).milliseconds
        assertDurationRoundTrip(DurationColumn.VARCHAR, if (negative) -duration else duration)
    }

    @Test
    fun testDurationNumericStoresNanoseconds() {
        val row = persistAndReload(DurationColumns().apply { durationNumeric = -(12.seconds + 345.nanoseconds) })
        val number = entityManager.createNativeQuery(
            "select duration_numeric from duration_columns where id = :id", BigDecimal::class.java,
        ).setParameter("id", row.id).singleResult as BigDecimal
        assertEquals(0, BigDecimal("-12000000345").compareTo(number))
    }

    @ParameterizedTest
    @CsvSource("0, 0", "0, 1", "1234567890, 987654321", "-123456789, 123456789", "-1, 999999999")
    fun testInstantColumnTypesWriteRead(seconds: Long, nanos: Int) {
        val instant = Instant.fromEpochSeconds(seconds, nanos)
        val loaded = persistAndReload(InstantColumns().apply {
            instantDefault = instant
            instantTimestamp = instant
            instantUtc = instant
        })
        val expected = if (sqlite) Instant.fromEpochMilliseconds(instant.toEpochMilliseconds()) else instant
        assertEquals(expected, loaded.instantDefault)
        assertEquals(expected, loaded.instantTimestamp)
        assertEquals(expected, loaded.instantUtc)
    }

    @Test
    fun testInstantNullColumns() {
        val loaded = persistAndReload(InstantColumns())
        assertNull(loaded.instantDefault)
        assertNull(loaded.instantTimestamp)
        assertNull(loaded.instantUtc)
    }

    @Test
    fun testHqlParameters() {
        val uuid = Uuid.parse("01234567-89ab-cdef-0123-456789abcdef")
        val uuidRow = persistAndReload(UuidColumns().apply { uuidDefault = uuid })
        val duration = 12.seconds + 345.nanoseconds
        val durationRow = persistAndReload(DurationColumns().apply { durationNumeric = duration })
        val instant = Instant.fromEpochMilliseconds(1234567890123)
        val instantRow = persistAndReload(InstantColumns().apply { instantDefault = instant })

        assertEquals(uuidRow.id, entityManager.createQuery(
            "from UuidColumns where uuidDefault = :value", UuidColumns::class.java,
        ).setParameter("value", uuid).singleResult.id)
        assertEquals(durationRow.id, entityManager.createQuery(
            "from DurationColumns where durationNumeric = :value", DurationColumns::class.java,
        ).setParameter("value", duration).singleResult.id)
        assertEquals(instantRow.id, entityManager.createQuery(
            "from InstantColumns where instantDefault = :value", InstantColumns::class.java,
        ).setParameter("value", instant).singleResult.id)
    }

    @Test
    fun testDurationDirtyChecking() {
        val row = persistAndReload(DurationColumns().apply { durationNumeric = 1.seconds })
        row.durationNumeric = (-1).nanoseconds
        entityManager.flush()
        entityManager.clear()
        val loaded = requireNotNull(entityManager.find(DurationColumns::class.java, requireNotNull(row.id)))
        assertEquals((-1).nanoseconds, loaded.durationNumeric)
    }

    protected fun assertDurationRoundTrip(column: DurationColumn, duration: Duration) {
        val loaded = persistAndReload(DurationColumns().apply { column.property.set(this, duration) })
        assertEquals(duration, column.property.get(loaded), column.name)
    }

    protected fun assertNumericDurationReadOverflow(column: DurationColumn, nanoseconds: String) {
        val sqlColumn = when (column) {
            DurationColumn.DEFAULT -> "duration_default"
            DurationColumn.NUMERIC -> "duration_numeric"
            DurationColumn.DECIMAL -> "duration_decimal"
            else -> error("Not a numeric column: $column")
        }
        // Bypass Duration conversion on write so that only JDBC extraction is under test.
        entityManager.createNativeQuery("insert into duration_columns (id, $sqlColumn) values (-1, :value)")
            .setParameter("value", BigDecimal(nanoseconds))
            .executeUpdate()
        entityManager.clear()

        val error = assertThrows(ArithmeticException::class.java) {
            entityManager.find(DurationColumns::class.java, -1L)
        }
        assertTrue(error.message.orEmpty().contains("Duration overflow"), "Unexpected error: $error")
    }

    private fun assertBigintRejected(duration: Duration, message: String) {
        val error = assertThrows(RuntimeException::class.java) {
            entityManager.persist(DurationColumns().apply { durationBigint = duration })
            entityManager.flush()
        }
        val cause = generateSequence<Throwable>(error) { it.cause }.last()
        assertTrue(cause is IllegalArgumentException, "Unexpected cause: $cause")
        assertTrue(cause.message.orEmpty().contains(message), "Unexpected error: $cause")
    }

    protected fun <T : Any> persistAndReload(entity: T): T {
        entityManager.persist(entity)
        entityManager.flush()
        val id = requireNotNull(entityManager.entityManagerFactory.persistenceUnitUtil.getIdentifier(entity))
        // Force a SELECT; returning the managed instance would not test JDBC extraction.
        entityManager.clear()
        return requireNotNull(entityManager.find(entity.javaClass, id))
    }

    companion object {
        @JvmStatic
        fun numericReadOverflows(): List<Arguments> =
            listOf(DurationColumn.DEFAULT, DurationColumn.NUMERIC, DurationColumn.DECIMAL).flatMap { column ->
                listOf("5000000000000000000000000", "9600000000000000000000000000").flatMap { nanos ->
                    listOf(Arguments.of(column, nanos), Arguments.of(column, "-$nanos"))
                }
            }

        @JvmStatic
        fun finiteDurations(): List<Arguments> = DurationColumn.entries.flatMap { column ->
            listOf(0L, 1L, -1L, 999999999L, -999999999L, 12000000345L, -12000000345L)
                .map { Arguments.of(column, it) }
        }

        @JvmStatic
        fun infiniteDurations(): List<Arguments> = DurationColumn.entries
            .filter { it != DurationColumn.BIGINT }
            .flatMap { column -> listOf(false, true).map { Arguments.of(column, it) } }
    }
}

enum class DurationColumn(val property: KMutableProperty1<DurationColumns, Duration?>) {
    DEFAULT(DurationColumns::durationDefault),
    NUMERIC(DurationColumns::durationNumeric),
    DECIMAL(DurationColumns::durationDecimal),
    VARCHAR(DurationColumns::durationVarchar),
    BIGINT(DurationColumns::durationBigint),
    DOUBLE(DurationColumns::durationDouble),
}
