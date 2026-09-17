package org.hibernate.type.descriptor.kotlin

import org.hibernate.dialect.Dialect
import org.hibernate.type.SqlTypes
import org.hibernate.type.descriptor.WrapperOptions
import org.hibernate.type.descriptor.java.AbstractClassJavaType
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan
import org.hibernate.type.descriptor.jdbc.JdbcType
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 *
 * Based on [org.hibernate.type.descriptor.java.DurationJavaType].
 */
internal class KotlinDurationJavaType :
    AbstractClassJavaType<Duration>(Duration::class.java, ImmutableMutabilityPlan.instance<Duration?>()) {
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
        if (value.isInfinite()) {
            return if (value.isPositive()) DurationConstants.INFINIT_MAGIC_NUMBER else DurationConstants.NEG_INFINIT_MAGIC_NUMBER
        }
        return value.toComponents { seconds, nanoseconds ->
            if (seconds == 0L) nanoseconds.toString()
            else seconds.toString() + abs(nanoseconds).toString().padStart(9, '0')
        }
    }

    override fun fromString(string: CharSequence?): Duration? {
        if (string == null) {
            return null
        }
        if (string.contentEquals(DurationConstants.INFINIT_MAGIC_NUMBER)) return Duration.INFINITE
        if (string.contentEquals(DurationConstants.NEG_INFINIT_MAGIC_NUMBER)) return -Duration.INFINITE

        val negative = string.firstOrNull() == '-'
        val start = if (negative || string.firstOrNull() == '+') 1 else 0
        if (string.length == start) throw NumberFormatException("Invalid duration: $string")
        for (index in start until string.length) {
            if (string[index] !in '0'..'9') throw NumberFormatException("Invalid duration: $string")
        }

        // check in string form because Duration truncates nanoseconds exceeding the maximum Duration value till millisecond
        if (exceedsMaxDuration(string, start)) {
            throw ArithmeticException("Duration overflow: $string nanoseconds exceeds the finite Duration range")
        }

        if (string.length - start <= 9) {
            return java.lang.Long.parseLong(string, 0, string.length, 10).nanoseconds
        }
        // The last nine digits are the nanosecond component; the sign applies to both parts.
        val cutoff = string.length - 9
        val seconds = java.lang.Long.parseLong(string, 0, cutoff, 10)
        val nanos = Integer.parseInt(string, cutoff, string.length, 10)
        return seconds.seconds + (if (negative) -nanos else nanos).nanoseconds
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
            if (DurationConstants.isPositiveInfinityMarker(value)) return Duration.INFINITE
            if (DurationConstants.isNegativeInfinityMarker(value)) return -Duration.INFINITE
            if (!value.isFinite() || abs(value) > DurationConstants.MAX_FINITE_NANOSECONDS_DOUBLE) {
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
            val duration = Duration.parseIsoString(value)
            if (duration.isInfinite() && value != duration.toIsoString()) {
                throw ArithmeticException("Duration overflow: $value exceeds the finite Duration range")
            }
            return duration
        }

        throw unknownWrap(value.javaClass)
    }

    private fun exceedsMaxDuration(value: CharSequence, start: Int): Boolean {
        var firstDigit = start
        while (firstDigit < value.length && value[firstDigit] == '0') firstDigit++

        val significantDigits = value.length - firstDigit
        val maximum = DurationConstants.MAX_FINITE_NANOSECONDS_STRING
        if (significantDigits != maximum.length) return significantDigits > maximum.length
        for (index in maximum.indices) {
            val difference = value[firstDigit + index] - maximum[index]
            if (difference != 0) return difference > 0
        }
        return false
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

    // Avoids initializing the BigDecimal class when not strictly necessary
    // Initializing BigDecimal generates a lot of allocations
    private object BigDecimalHolder {
        val BILLION: BigDecimal = BigDecimal.valueOf(1000000000L)
        // Kotlin Duration reserves Long.MAX_VALUE / 2 milliseconds for infinity.
        val MAX_FINITE_NANOSECONDS: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE / 2 - 1).movePointRight(6)
        val INFINIT_MAGIC_NUMBER_BIG_DECIMAL = DurationConstants.INFINIT_MAGIC_NUMBER.toBigDecimal()
        val NEG_INFINIT_MAGIC_NUMBER_BIG_DECIMAL = DurationConstants.NEG_INFINIT_MAGIC_NUMBER.toBigDecimal()
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
