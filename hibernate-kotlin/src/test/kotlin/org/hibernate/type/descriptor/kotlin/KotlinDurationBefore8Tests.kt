package org.hibernate.type.descriptor.kotlin

import org.hibernate.query.MutationQuery
import org.junit.jupiter.api.Tag

@Tag("before8")
class KotlinDurationBefore8Tests : AbstractKotlinDurationTests() {
    // Hibernate versions before 8 require executeUpdate().
    @Suppress("DEPRECATION")
    override fun MutationQuery.executeMutation(): Int = executeUpdate()
}
