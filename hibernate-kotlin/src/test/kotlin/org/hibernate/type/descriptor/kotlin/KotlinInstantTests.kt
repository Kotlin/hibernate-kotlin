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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.time.Instant
import java.time.Instant as JavaInstant

class KotlinInstantTests : BaseSessionFactoryTest() {
    override val jdbcUrl: String = "jdbc:h2:mem:kotlin-instant-tests;DB_CLOSE_DELAY=-1"

    override val annotatedClasses: Array<Class<*>> = arrayOf(InstantColumnsEntity::class.java)

    @ParameterizedTest
    @MethodSource("instantValues")
    fun testColumnTypesWriteRead(instant: Instant) {
        val loaded = persistAndReload(
            InstantColumnsEntity().apply {
                instantDefault = instant
                instantTimestamp = instant
                instantUtc = instant
            },
            InstantColumnsEntity::class.java,
        ) { it.id }

        assertEquals(instant, loaded.instantDefault)
        assertEquals(instant, loaded.instantTimestamp)
        assertEquals(instant, loaded.instantUtc)
    }

    @Test
    fun testNullColumns() {
        val loaded = persistAndReload(InstantColumnsEntity(), InstantColumnsEntity::class.java) { it.id }

        assertNull(loaded.instantDefault)
        assertNull(loaded.instantTimestamp)
        assertNull(loaded.instantUtc)
    }

    @Test
    fun testHqlParameter() {
        val instant = Instant.parse("2025-02-27T01:01:01.123456789Z")
        val row = persistAndReload(
            InstantColumnsEntity().apply { instantDefault = instant },
            InstantColumnsEntity::class.java,
        ) { it.id }

        val loadedId = inTransaction { session ->
            session.createSelectionQuery(
                "select id from KotlinInstantColumns where instantDefault = :value",
                Long::class.javaObjectType,
            ).setParameter("value", instant).singleResult
        }

        assertEquals(row.id, loadedId)
    }

    @Test
    fun testHibernateWriteThenJdbcRead() {
        val instant = Instant.parse("1904-12-31T23:59:59.999999999Z")
        val row = persistAndReload(
            InstantColumnsEntity().apply { instantUtc = instant },
            InstantColumnsEntity::class.java,
        ) { it.id }

        val jdbcValue = inTransaction { session ->
            session.doReturningWork { connection ->
                connection.prepareStatement(
                    "select instant_utc from kotlin_instant_columns where id = ?",
                ).use { statement ->
                    statement.setLong(1, requireNotNull(row.id))
                    statement.executeQuery().use { resultSet ->
                        resultSet.next()
                        resultSet.getObject(1, OffsetDateTime::class.java).toInstant()
                    }
                }
            }
        }

        assertEquals(instant.toJavaInstant(), jdbcValue)
    }

    @Test
    fun testJdbcWriteThenHibernateRead() {
        val id = -1L
        val instant = Instant.parse("1899-12-31T23:59:59.999999999Z")

        inTransaction { session ->
            session.doWork { connection ->
                connection.prepareStatement(
                    "insert into kotlin_instant_columns (id, instant_utc) values (?, ?)",
                ).use { statement ->
                    statement.setLong(1, id)
                    statement.setObject(2, instant.toJavaInstant().atOffset(ZoneOffset.UTC))
                    statement.executeUpdate()
                }
            }
        }

        val loaded = inTransaction { session ->
            requireNotNull(session.find(InstantColumnsEntity::class.java, id))
        }

        assertEquals(instant, loaded.instantUtc)
    }

    private fun Instant.toJavaInstant(): JavaInstant =
        JavaInstant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

    private companion object {
        @JvmStatic
        fun instantValues(): List<Arguments> = listOf(
            Arguments.of(Instant.fromEpochSeconds(0, 0)),
            Arguments.of(Instant.fromEpochSeconds(0, 1)),
            Arguments.of(Instant.fromEpochSeconds(1_234_567_890, 987_654_321)),
            Arguments.of(Instant.fromEpochSeconds(-123_456_789, 123_456_789)),
            Arguments.of(Instant.fromEpochSeconds(-1, 999_999_999)),
            // values from InstantTests
            Arguments.of(Instant.parse("1899-12-31T23:59:59.999999999Z")),
            Arguments.of(Instant.parse("1904-12-31T23:59:59.999999999Z")),
            Arguments.of(Instant.parse("1905-01-01T00:00:00Z")),
        )
    }

    @Entity(name = "KotlinInstantColumns")
    @Table(name = "kotlin_instant_columns")
    open class InstantColumnsEntity {
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null

        @field:Column(name = "instant_default", precision = 9)
        open var instantDefault: Instant? = null

        @field:JdbcTypeCode(SqlTypes.TIMESTAMP)
        @field:Column(name = "instant_timestamp", precision = 9)
        open var instantTimestamp: Instant? = null

        @field:JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
        @field:Column(name = "instant_utc", precision = 9)
        open var instantUtc: Instant? = null
    }
}
