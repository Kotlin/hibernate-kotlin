package org.hibernate.type.descriptor.kotlin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.uuid.Uuid

class KotlinUuidMappingTests : BaseSessionFactoryTest() {
    override val jdbcUrl: String = "jdbc:h2:mem:kotlin-uuid-mapping-tests;DB_CLOSE_DELAY=-1"

    override val annotatedClasses: Array<Class<*>> = arrayOf(UuidColumnsEntity::class.java)

    @ParameterizedTest
    @MethodSource("uuidValues")
    fun testColumnTypesWriteRead(uuid: Uuid?) {
        val loaded = persistAndReload(
            UuidColumnsEntity().apply {
                uuidDefault = uuid
                uuidChar = uuid
                uuidVarchar = uuid
                uuidBinary = uuid
            },
            UuidColumnsEntity::class.java,
        ) { it.id }

        assertEquals(uuid, loaded.uuidDefault)
        assertEquals(uuid, loaded.uuidChar)
        assertEquals(uuid, loaded.uuidVarchar)
        assertEquals(uuid, loaded.uuidBinary)
    }

    @Test
    fun testHqlParameter() {
        val uuid = Uuid.random()
        val row = persistAndReload(
            UuidColumnsEntity().apply { uuidDefault = uuid },
            UuidColumnsEntity::class.java,
        ) { it.id }

        val loadedId = inTransaction { session ->
            session.createSelectionQuery(
                "select id from KotlinUuidColumns where uuidDefault = :value",
                Long::class.javaObjectType,
            ).setParameter("value", uuid).singleResult
        }

        assertEquals(row.id, loadedId)
    }

    private companion object {
        @JvmStatic
        fun uuidValues(): List<Arguments> = listOf(
            Arguments.of(null as Uuid?),
            Arguments.of(Uuid.parse("00000000-0000-0000-0000-000000000000")),
            Arguments.of(Uuid.parse("01234567-89ab-cdef-0123-456789abcdef")),
            Arguments.of(Uuid.parse("fedcba98-7654-3210-89ab-cdef01234567")),
            Arguments.of(Uuid.parse("ffffffff-ffff-ffff-ffff-ffffffffffff")),
        )
    }

    @Entity(name = "KotlinUuidColumns")
    open class UuidColumnsEntity {
        @field:Id
        @field:GeneratedValue(strategy = GenerationType.IDENTITY)
        open var id: Long? = null

        open var uuidDefault: Uuid? = null

        @field:JdbcTypeCode(SqlTypes.CHAR)
        @field:Column(length = 36)
        open var uuidChar: Uuid? = null

        @field:JdbcTypeCode(SqlTypes.VARCHAR)
        @field:Column(length = 36)
        open var uuidVarchar: Uuid? = null

        @field:JdbcTypeCode(SqlTypes.BINARY)
        @field:Column(length = 16)
        open var uuidBinary: Uuid? = null
    }
}
