package org.hibernate.type.descriptor.kotlin

import org.hibernate.query.MutationQuery
import org.junit.jupiter.api.Tag

@Tag("hibernate8")
class KotlinDurationTests : AbstractKotlinDurationTests() {
    override fun MutationQuery.executeMutation(): Int = execute()
}
