package org.hibernate.type.descriptor.kotlin

import kotlin.time.Instant

class KotlinInstantDescriptorTest : AbstractDescriptorTest<Instant>(KotlinInstantJavaType.INSTANCE) {
    override val testData = Data(
        original = Instant.fromEpochMilliseconds(1_476_340_818_745L),
        copy = Instant.fromEpochMilliseconds(1_476_340_818_745L),
        different = Instant.fromEpochMilliseconds(1_476_340_818_746L),
    )

    override val shouldBeMutable: Boolean = false
}
