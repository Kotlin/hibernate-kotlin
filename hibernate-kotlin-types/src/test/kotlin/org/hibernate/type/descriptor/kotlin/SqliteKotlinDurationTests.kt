package org.hibernate.type.descriptor.kotlin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.community.dialect.SQLiteDialect
import org.hibernate.type.SqlTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KMutableProperty1
import kotlin.time.Duration

class SqliteKotlinDurationTests : BaseSessionFactoryTest() {
    private lateinit var databaseFile: Path

    override val dialectClass = SQLiteDialect::class.java
    override val jdbcDriver: String = "org.sqlite.JDBC"
    override val jdbcUser: String? = null
    override val jdbcUrl: String
        get() = "jdbc:sqlite:$databaseFile"
    override val annotatedClasses: Array<Class<*>> = arrayOf(SqliteNarrowDurationColumnsEntity::class.java)

    override fun beforeSessionFactoryCreated() {
        databaseFile = Files.createTempFile("kotlin-duration-tests-", ".sqlite")
    }

    override fun afterSessionFactoryClosed() {
        Files.deleteIfExists(databaseFile)
    }

    /**
     * Tests that SQLite ignores `precision` parameter and saves values as REAL - in this case we can store INFINITE.
     */
    @ParameterizedTest(name = "{0}: negative={1}")
    @MethodSource("narrowNumericInfinityValues")
    fun testInfinityAllowedByNarrowNumericColumn(column: NarrowDurationColumn, negative: Boolean) {
        val duration = if (negative) -Duration.INFINITE else Duration.INFINITE
        val loaded = persistAndReload(
            SqliteNarrowDurationColumnsEntity().apply { column.property.set(this, duration) },
            SqliteNarrowDurationColumnsEntity::class.java,
        ) { it.id }
        assertEquals(duration, column.property.get(loaded)!!)
    }

    private companion object {
        @JvmStatic
        fun narrowNumericInfinityValues(): List<Arguments> = NarrowDurationColumn.entries.flatMap { column ->
            listOf(false, true).map { negative -> Arguments.of(column, negative) }
        }
    }

    enum class NarrowDurationColumn(
        val property: KMutableProperty1<SqliteNarrowDurationColumnsEntity, Duration?>,
    ) {
        DEFAULT(SqliteNarrowDurationColumnsEntity::durationDefault),
        NUMERIC(SqliteNarrowDurationColumnsEntity::durationNumeric),
        DECIMAL(SqliteNarrowDurationColumnsEntity::durationDecimal),
    }

    @Entity
    open class SqliteNarrowDurationColumnsEntity {
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null

        @Suppress("JpaAttributeTypeInspection")
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
