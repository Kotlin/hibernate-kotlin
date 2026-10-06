package org.hibernate.type.descriptor.kotlin

import org.hibernate.SharedSessionContract
import org.hibernate.dialect.Dialect
import org.hibernate.type.SqlTypes
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractClassJavaType
import org.hibernate.type.descriptor.java.MutabilityPlan
import org.hibernate.type.descriptor.jdbc.JdbcType
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators
import java.io.Serializable
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 *
 * Supports finite values in range `-4611686018427387902000000`..`4611686018427387902000000` nanoseconds.
 * Except for the infinity representations described below, reading values outside this range throws an [ArithmeticException].
 *
 * For exact numeric columns, duration is stored in nanoseconds.
 * The values `-9223461171522140000000000000` and `9223461171522140000000000000` encode `-Duration.INFINITE` and `Duration.INFINITE`, respectively.
 * The full finite range requires a precision of at least 25, and infinite values require a precision of at least 28.
 * `BIGINT` columns only support finite durations whose nanosecond value fits in a [Long].
 *
 * For text columns, duration is stored in ISO-8601 format.
 * The values `"-PT9999999999999H"` and `"PT9999999999999H"` encode `-Duration.INFINITE` and `Duration.INFINITE`, respectively.
 *
 * For floating-point columns, duration is stored in nanoseconds.
 * The values `-9223461171522140000000000000` and `9223461171522140000000000000` encode `-Duration.INFINITE` and `Duration.INFINITE`, respectively.
 * `-Inf` and `Inf` also represent `-Duration.INFINITE` and `Duration.INFINITE`, respectively.
 * Floating-point precision may cause additional rounding of finite values.
 *
 * The Duration value is serialized as a string in ISO-8601 format, while infinite values are encoded as `"PT9999999999999H"` and `"-PT9999999999999H"`.
 *
 *
 * Due to the specifics of [Duration] implementation, some values can't be represented in Duration.
 *
 * If the absolute raw value is greater than or equal to `4 611 686 018 427 000 000`, there are windows with a period of `1 000 000`.
 * Values inside each window are truncated toward zero to the nearest whole millisecond.
 *
 * Based on [org.hibernate.type.descriptor.java.DurationJavaType].
 */
internal class KotlinDurationJavaType :
    AbstractClassJavaType<Duration>(Duration::class.java, DurationMutabilityPlan) {
    override fun isInstance(value: Any?): Boolean {
        return value is Duration
    }

    override fun cast(value: Any?): Duration? {
        return value as Duration?
    }

    override fun getRecommendedJdbcType(context: JdbcTypeIndicators): JdbcType? {
        return context.typeConfiguration
            .jdbcTypeRegistry
            .getDescriptor(context.preferredSqlTypeCodeForDuration)
    }

    override fun useObjectEqualsHashCode(): Boolean {
        return true
    }

    override fun toString(value: Duration?): String? {
        if (value == null) {
            return null
        }
        return value.toIsoString()
    }

    override fun fromString(string: CharSequence?): Duration? {
        if (string == null) {
            return null
        }
        return parseIsoDuration(string.toString())
    }

    override fun <X> unwrap(duration: Duration?, type: Class<X?>, options: WrapperOptions?): X? {
        if (duration == null) {
            return null
        }

        if (Duration::class.java.isAssignableFrom(type)) {
            return type.cast(duration)
        }

        if (BigDecimal::class.java.isAssignableFrom(type)) {
            val bigDecimal = when {
                duration.isInfinite() && duration.isPositive() -> BigDecimalHolder.INFINIT_MAGIC_NUMBER_BIG_DECIMAL
                duration.isInfinite() && duration.isNegative() -> BigDecimalHolder.NEG_INFINIT_MAGIC_NUMBER_BIG_DECIMAL
                else -> duration.toComponents { seconds, nanoseconds ->
                    BigDecimal(seconds)
                        .movePointRight(9)
                        .add(nanoseconds.toBigDecimal())
                }
            }

            return type.cast(bigDecimal)
        }

        if (Double::class.javaObjectType.isAssignableFrom(type)) {
            val double = when {
                duration.isInfinite() && duration.isPositive() -> DurationConstants.INFINIT_MAGIC_NUMBER_DOUBLE
                duration.isInfinite() && duration.isNegative() -> DurationConstants.NEG_INFINIT_MAGIC_NUMBER_DOUBLE
                else -> duration.toComponents { seconds, nanoseconds ->
                    seconds.toDouble() * DurationConstants.BILLION_DOUBLE + nanoseconds
                }
            }
            return type.cast(double)
        }

        if (String::class.java.isAssignableFrom(type)) {
            return type.cast(duration.toIsoString())
        }

        if (Long::class.javaObjectType.isAssignableFrom(type)) {
            if (duration.isInfinite()) throw IllegalArgumentException("$duration cant' be saved in column with type LONG")

            val nanos = duration.inWholeNanoseconds
            if (nanos == Long.MAX_VALUE) throw IllegalArgumentException("Duration $duration is too large to be saved in column with type LONG")
            if (nanos == Long.MIN_VALUE) throw IllegalArgumentException("Duration $duration is too large to be saved in column with type LONG")

            return type.cast(nanos)
        }

        throw unknownUnwrap(type)
    }

    override fun <X> wrap(value: X?, options: WrapperOptions?): Duration? {
        if (value == null) {
            return null
        }

        if (value is Duration) {
            return value
        }

        if (value is BigDecimal) {
            if (value.compareTo(BigDecimalHolder.INFINIT_MAGIC_NUMBER_BIG_DECIMAL) == 0) return Duration.INFINITE
            if (value.compareTo(BigDecimalHolder.NEG_INFINIT_MAGIC_NUMBER_BIG_DECIMAL) == 0) return -Duration.INFINITE

            // Check before conversion: Duration can truncate sub-millisecond overflow or saturate to infinity.
            if (value.abs() > BigDecimalHolder.MAX_FINITE_NANOSECONDS) {
                throw ArithmeticException("Duration overflow: $value nanoseconds exceeds the finite Duration range")
            }

            val secondsAndNanos = value.divideAndRemainder(BigDecimalHolder.BILLION)
            val seconds = secondsAndNanos[0]!!.longValueExact().seconds
            // use intValue() not intValueExact() here, because
            // the database will sometimes produce garbage digits
            // in a floating point multiplication, and we would
            // get an unwanted ArithmeticException
            val nanos = secondsAndNanos[1]!!.toInt().nanoseconds

            return seconds + nanos
        }

        if (value is Double) {
            if (value.isInfinite()) {
                return if (value > 0) Duration.INFINITE else -Duration.INFINITE
            }
            if (value.isNaN()) {
                throw ArithmeticException("Duration overflow: NaN value is not allowed")
            }
            if (DurationConstants.isPositiveInfinityMarker(value)) return Duration.INFINITE
            if (DurationConstants.isNegativeInfinityMarker(value)) return -Duration.INFINITE
            if (abs(value) > DurationConstants.MAX_FINITE_NANOSECONDS_DOUBLE) {
                throw ArithmeticException("Duration overflow: $value nanoseconds exceeds the finite Duration range")
            }

            val duration = value.nanoseconds
            if (duration.isInfinite()) {
                // A Double near the boundary can round to the millisecond value reserved for infinity.
                throw ArithmeticException("Duration overflow: $value nanoseconds exceeds the finite Duration range")
            }
            return duration
        }

        if (value is Long) {
            return value.nanoseconds
        }

        if (value is String) {
            return parseIsoDuration(value)
        }

        throw unknownWrap(value.javaClass)
    }

    public override fun getDefaultSqlPrecision(dialect: Dialect, jdbcType: JdbcType): Int = if (jdbcType.ddlTypeCode == SqlTypes.INTERVAL_SECOND) {
        // Usually the maximum precision for interval types
        18
    } else {
        // 19+9 = 28 digits is the maximum possible Duration
        // precision, but is an unnecessarily large default,
        // except for cosmological applications. Thirty
        // millennia in both timelike directions should be
        // sufficient time for most businesses!
        min(21, dialect.typeSizingProfile.defaultDecimalPrecision())
    }

    public override fun getDefaultSqlScale(dialect: Dialect, jdbcType: JdbcType): Int {
        return if (jdbcType.ddlTypeCode == SqlTypes.INTERVAL_SECOND)
            dialect.typeSizingProfile.defaultIntervalSecondScale()
        else
            0 // For non-interval types, we use the type numeric(21)
    }

    private fun parseIsoDuration(value: String): Duration {
        val duration = Duration.parseIsoString(value)
        // duration.isInfinite() can be a mark of overflow, so compare original value with INFINITE representations
        if (duration.isInfinite() && value != LazyConstants.ISO_STRING_INFINITE && value != LazyConstants.ISO_STRING_NEG_INFINITE) {
            throw ArithmeticException("Duration overflow: $value exceeds the finite Duration range")
        }
        return duration
    }

    // Avoids initializing the BigDecimal class when not strictly necessary
    // Initializing BigDecimal generates a lot of allocations
    private object BigDecimalHolder {
        val BILLION: BigDecimal = BigDecimal.valueOf(1000000000L)
        // Kotlin Duration reserves Long.MAX_VALUE / 2 milliseconds for infinity.
        val MAX_FINITE_NANOSECONDS: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE / 2 - 1).movePointRight(6)
        val INFINIT_MAGIC_NUMBER_BIG_DECIMAL = DurationConstants.INFINIT_MAGIC_NUMBER.toBigDecimal()
        val NEG_INFINIT_MAGIC_NUMBER_BIG_DECIMAL = DurationConstants.NEG_INFINIT_MAGIC_NUMBER.toBigDecimal()
    }

    private object LazyConstants {
        val ISO_STRING_INFINITE = Duration.INFINITE.toIsoString()
        val ISO_STRING_NEG_INFINITE = "-$ISO_STRING_INFINITE"
    }

    private object DurationConstants {
        /**
         * Magic number used to represent infinite Duration values.
         *
         * Selected to meet the set of criteria:
         * - to be greater than the maximum value of Java Duration (-9223372036854775808000000000..9223372036854775807999999999)
         * - to have the minimum number of decimal places
         * - to have the same representation both in Double and in Float
         * - have as much as possible zero lower digits, to be preserved without loss when writing in the 123E42 format, since some JDBC drivers use such compression during transmission, for example, SQLite
         */
        const val INFINIT_MAGIC_NUMBER = "9223461171522140000000000000"
        const val NEG_INFINIT_MAGIC_NUMBER = "-$INFINIT_MAGIC_NUMBER"
        const val BILLION_DOUBLE: Double = 1_000_000_000.0

        // This decimal marker resolves to the same value at Float and Double precision.
        val INFINIT_MAGIC_NUMBER_DOUBLE: Double = INFINIT_MAGIC_NUMBER.toDouble()
        val NEG_INFINIT_MAGIC_NUMBER_DOUBLE: Double = -INFINIT_MAGIC_NUMBER_DOUBLE
        const val MAX_FINITE_NANOSECONDS_STRING: String = "4611686018427387902000000"
        const val MAX_FINITE_NANOSECONDS_DOUBLE: Double = (Long.MAX_VALUE / 2 - 1).toDouble() * 1_000_000.0

        fun isPositiveInfinityMarker(value: Double): Boolean =
            value == INFINIT_MAGIC_NUMBER_DOUBLE

        fun isNegativeInfinityMarker(value: Double): Boolean =
            value == NEG_INFINIT_MAGIC_NUMBER_DOUBLE
    }

    internal companion object {
        /**
         * Singleton access
         */
        val INSTANCE: KotlinDurationJavaType = KotlinDurationJavaType()
    }
}

private object DurationMutabilityPlan : MutabilityPlan<Duration> {
    override fun isMutable(): Boolean = false

    override fun deepCopy(value: Duration?): Duration? = value

    override fun disassemble(value: Duration?, session: SharedSessionContract?): Serializable? =
        KotlinDurationJavaType.INSTANCE.toString(value)

    override fun assemble(cached: Serializable?, session: SharedSessionContract?): Duration? =
        KotlinDurationJavaType.INSTANCE.fromString(cached as String?)
}
