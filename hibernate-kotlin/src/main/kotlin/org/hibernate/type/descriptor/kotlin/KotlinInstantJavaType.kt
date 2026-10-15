package org.hibernate.type.descriptor.kotlin

import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractClassJavaType
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan
import org.hibernate.type.descriptor.java.InstantJavaType
import org.hibernate.type.descriptor.jdbc.JdbcType
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators
import kotlin.time.Instant
import java.time.Instant as JavaInstant

/**
 * Descriptor for [Instant] handling from Kotlin standard library.
 *
 * Delegates main work to [InstantJavaType] so as not to duplicate the complex logic for working with dates exclusions.
 */
public class KotlinInstantJavaType : AbstractClassJavaType<Instant>(
    Instant::class.java,
    ImmutableMutabilityPlan.instance()
) {

    override fun <X> unwrap(
        value: Instant?,
        type: Class<X>,
        options: WrapperOptions?
    ): X? {
        if (value == null) return null

        if (type == Instant::class.java) return type.cast(value)

        val javaInstant = JavaInstant.ofEpochSecond(
            value.epochSeconds,
            value.nanosecondsOfSecond.toLong()
        )

        return DELEGATE.unwrap(javaInstant, type, options)
    }

    override fun <X> wrap(
        value: X?,
        options: WrapperOptions?
    ): Instant? {
        if (value == null) return null

        if (value is Instant) return value

        val javaInstant = DELEGATE.wrap(value, options) ?: return null

        return Instant.fromEpochSeconds(javaInstant.epochSecond, javaInstant.nano)
    }

    override fun isInstance(value: Any?): Boolean {
        return value is Instant
    }

    override fun cast(value: Any?): Instant? {
        return value as Instant?
    }


    override fun useObjectEqualsHashCode(): Boolean {
        return true
    }

    override fun toString(value: Instant?): String? {
        if (value == null) {
            return null
        }

        return value.toString()
    }

    override fun fromString(string: CharSequence?): Instant? {
        return string?.let { Instant.parse(it) }
    }

    override fun getRecommendedJdbcType(context: JdbcTypeIndicators): JdbcType? {
        return DELEGATE.getRecommendedJdbcType(context)
    }

    public companion object {
        public val INSTANCE: KotlinInstantJavaType = KotlinInstantJavaType()

        private val DELEGATE = InstantJavaType.INSTANCE
    }
}
