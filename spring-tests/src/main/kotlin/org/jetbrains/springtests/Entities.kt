package org.jetbrains.springtests

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity
@Table(name = "uuid_columns")
open class UuidColumns {
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

@Entity
@Table(name = "duration_columns")
open class DurationColumns {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    // The infinity sentinel has 28 digits, exceeding Hibernate's default NUMERIC(21).
    @field:Column(precision = 28, scale = 0)
    open var durationDefault: Duration? = null

    @field:JdbcTypeCode(SqlTypes.NUMERIC)
    @field:Column(precision = 28, scale = 0)
    open var durationNumeric: Duration? = null

    @field:JdbcTypeCode(SqlTypes.DECIMAL)
    @field:Column(precision = 28, scale = 0)
    open var durationDecimal: Duration? = null

    @field:JdbcTypeCode(SqlTypes.VARCHAR)
    open var durationVarchar: Duration? = null

    @field:JdbcTypeCode(SqlTypes.BIGINT)
    open var durationBigint: Duration? = null

    @field:JdbcTypeCode(SqlTypes.DOUBLE)
    @field:Column(columnDefinition = "double precision")
    open var durationDouble: Duration? = null
}

@Entity
@Table(name = "narrow_duration_columns")
open class NarrowDurationColumns {
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

@Entity
@Table(name = "instant_columns")
open class InstantColumns {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @field:Column(precision = 9)
    open var instantDefault: Instant? = null

    @field:JdbcTypeCode(SqlTypes.TIMESTAMP)
    @field:Column(precision = 9)
    open var instantTimestamp: Instant? = null

    @field:JdbcTypeCode(SqlTypes.TIMESTAMP_UTC)
    @field:Column(precision = 9)
    open var instantUtc: Instant? = null
}

@Entity
@Table(name = "auto_uuid")
open class AutoUuid {
    @field:Id
    @field:GeneratedValue
    open var id: Uuid? = null
}

@Entity
@Table(name = "explicit_uuid")
open class ExplicitUuid {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    open var id: Uuid? = null
}

@Entity
@Table(name = "char_uuid")
open class CharUuid {
    @field:Id
    @field:GeneratedValue(strategy = GenerationType.UUID)
    @field:JdbcTypeCode(SqlTypes.CHAR)
    @field:Column(length = 36)
    open var id: Uuid? = null
}
