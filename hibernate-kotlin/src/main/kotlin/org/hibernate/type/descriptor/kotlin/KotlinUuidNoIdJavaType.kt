package org.hibernate.type.descriptor.kotlin


import org.hibernate.dialect.Dialect
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractClassJavaType
import org.hibernate.type.descriptor.java.UUIDJavaType
import org.hibernate.type.descriptor.java.UuidCapableJavaType
import org.hibernate.type.descriptor.jdbc.JdbcType
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators
import java.util.*
import kotlin.uuid.Uuid

/**
 * Descriptor for [Uuid] handling from Kotlin standard library.
 * Converts Kotlin UUID value from and to [String], [ByteArray] or [java.util.UUID].
 *
 * String and bytes representations are the same as for Java UUID.
 * Therefore, this descriptor is interchangeable with [UUIDJavaType] in terms of values in the database.
 *
 * Used for Hibernate versions below 8 and does not contain any dependencies on [UuidCapableJavaType].
 */
public class KotlinUuidNoIdJavaType : AbstractClassJavaType<Uuid>(Uuid::class.java) {
    private val supportsJdbcTypeLookup = KotlinTypeContributor().readHibernateVersion()
        ?.let { (major, minor) -> major > 6 || (major == 6 && minor >= 2) } == true

    override fun isInstance(value: Any?): Boolean {
        return value is Uuid
    }

    override fun cast(value: Any?): Uuid? {
        return value as Uuid?
    }

    override fun getRecommendedJdbcType(context: JdbcTypeIndicators): JdbcType? {
        val jdbcTypeCode = context.preferredSqlTypeCodeForUuid
        // JdbcTypeIndicators.getJdbcType() is available since Hibernate 6.2.
        return if (supportsJdbcTypeLookup) {
            context.getJdbcType(jdbcTypeCode)
        } else {
            context.typeConfiguration.jdbcTypeRegistry.getDescriptor(jdbcTypeCode)
        }
    }


    public override fun <X> unwrap(
        value: Uuid?,
        requestedType: Class<X>,
        options: WrapperOptions?
    ): X? {
        if (value == null) {
            return null
        }

        return when {
            Uuid::class.java.isAssignableFrom(requestedType) -> requestedType.cast(value)
            String::class.java.isAssignableFrom(requestedType) -> requestedType.cast(value.toHexDashString())
            ByteArray::class.java.isAssignableFrom(requestedType) -> requestedType.cast(value.toByteArray())
            UUID::class.java.isAssignableFrom(requestedType) -> {
                requestedType.cast(value.toLongs { mostSignificantBits, leastSignificantBits ->
                    UUID(mostSignificantBits, leastSignificantBits)
                })
            }
            else -> throw unknownUnwrap(requestedType)
        }
    }


    public override fun <X> wrap(
        rawValue: X?,
        options: WrapperOptions?
    ): Uuid? {
        if (rawValue == null) {
            return null
        }

        return when (rawValue) {
            is Uuid -> rawValue
            is String -> Uuid.parseHexDash(rawValue)
            is ByteArray -> Uuid.fromByteArray(rawValue)
            is UUID -> Uuid.fromLongs(rawValue.mostSignificantBits, rawValue.leastSignificantBits)
            else -> throw unknownWrap(rawValue.javaClass)
        }
    }

    public override fun toString(value: Uuid?): String? {
        value ?: return null
        return value.toHexDashString()
    }

    public override fun fromString(string: CharSequence?): Uuid? {
        string ?: return null

        return Uuid.parseHexDash(string.toString())
    }

    public override fun getDefaultSqlLength(
        dialect: Dialect?,
        jdbcType: JdbcType?
    ): Long {
        return DELEGATE.getDefaultSqlLength(
            dialect,
            jdbcType
        )
    }


    override fun useObjectEqualsHashCode(): Boolean = true


    public companion object {
        public val INSTANCE: KotlinUuidNoIdJavaType = KotlinUuidNoIdJavaType()

        private val DELEGATE: UUIDJavaType = UUIDJavaType.INSTANCE
    }
}
