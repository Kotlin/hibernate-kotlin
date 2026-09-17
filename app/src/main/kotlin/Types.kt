package org.jetbrains.app

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid


@Entity
@Table(name = "timed_tasks")
open class TimeTypes(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    open var duration: Duration? = null,
    open var instant: Instant? = null,
    open var uuid: Uuid? = null,

    open var jDuration: java.time.Duration? = null,
    open var jInstant: java.time.Instant? = null,
) {
    constructor(): this(id = null)
}

@Entity
open class GeneratedUuid(
    @Id
    @GeneratedValue
    open var id: Uuid? = null,

    open var value: String? = null,
) {
    constructor() : this(id = null)
}

@Entity
open class GeneratedUuidExplicit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    open var id: Uuid? = null,

    open var value: String? = null,
) {
    constructor() : this(id = null)
}