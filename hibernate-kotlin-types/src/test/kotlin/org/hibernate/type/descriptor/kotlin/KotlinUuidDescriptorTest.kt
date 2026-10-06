package org.hibernate.type.descriptor.kotlin

import kotlin.uuid.Uuid

class KotlinUuidDescriptorTest : AbstractDescriptorTest<Uuid>(KotlinUuidJavaType.INSTANCE) {
    override val testData = Data(
        original = Uuid.parse("01234567-89ab-cdef-0123-456789abcdef"),
        copy = Uuid.parse("01234567-89ab-cdef-0123-456789abcdef"),
        different = Uuid.parse("fedcba98-7654-3210-89ab-cdef01234567"),
    )

    override val shouldBeMutable: Boolean = false
}
