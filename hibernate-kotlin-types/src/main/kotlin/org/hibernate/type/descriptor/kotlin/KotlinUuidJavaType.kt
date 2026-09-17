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

public class KotlinUuidJavaType : AbstractClassJavaType<Uuid>(Uuid::class.java), UuidCapableJavaType<Uuid> {

    public override fun getRecommendedJdbcType(
        context: JdbcTypeIndicators?
    ): JdbcType? {
        return UUID_DELEGATE.getRecommendedJdbcType(context)
    }


    public override fun <X> unwrap(
        value: Uuid?,
        requestedType: Class<X?>,
        options: WrapperOptions?
    ): X? {
        if (value == null) {
            return null
        }

        if (Uuid::class.java.isAssignableFrom(requestedType)) {
            return requestedType.cast(value)
        }

        val javaUuid = value.toLongs { mostSignificantBits, leastSignificantBits ->
             UUID(mostSignificantBits, leastSignificantBits)
        }

        return UUID_DELEGATE.unwrap(
            javaUuid,
            requestedType,
            options
        )
    }


    public override fun <X> wrap(
        rawValue: X?,
        options: WrapperOptions?
    ): Uuid? {
        if (rawValue == null) {
            return null
        }

        if (rawValue is Uuid) {
            return rawValue
        }

        val javaUuid = UUID_DELEGATE.wrap<X?>(
            rawValue,
            options
        )

        return Uuid.fromLongs(javaUuid.mostSignificantBits, javaUuid.leastSignificantBits)
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
        return UUID_DELEGATE.getDefaultSqlLength(
            dialect,
            jdbcType
        )
    }

    override fun getUuidValueTransformer(): UuidCapableJavaType.ValueTransformer<Uuid> = UUID_TRANSFORMER

    override fun useObjectEqualsHashCode(): Boolean = true

    override fun prefersUuidGeneration(): Boolean = true

    public companion object {
        public val INSTANCE: KotlinUuidJavaType = KotlinUuidJavaType()

        private val UUID_DELEGATE: UUIDJavaType = UUIDJavaType.INSTANCE

        private val UUID_TRANSFORMER =
            object : UuidCapableJavaType.ValueTransformer<Uuid> {
                override fun transform(uuid: UUID): Uuid =
                    Uuid.fromLongs(
                        uuid.mostSignificantBits,
                        uuid.leastSignificantBits,
                    )

                override fun parse(value: Uuid): UUID =
                    value.toLongs { most, least -> UUID(most, least) }
            }

    }
}
