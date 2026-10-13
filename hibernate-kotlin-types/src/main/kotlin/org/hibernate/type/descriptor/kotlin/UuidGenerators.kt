package org.hibernate.type.descriptor.kotlin

import org.hibernate.annotations.IdGeneratorType
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.GeneratorCreationContext
import org.hibernate.id.IdentifierGenerator
import java.lang.reflect.Member
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * UUID version of generated ID value for Kotlin [Uuid] type.
 */
public enum class KotlinUuidVersion {
    V4,
    V7
}

/**
 * Annotation for ID generation using Kotlin algorithms.
 */
@IdGeneratorType(KotlinUuidGeneratorImpl::class)
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class KotlinUuidGenerator(val version: KotlinUuidVersion = KotlinUuidVersion.V4)


internal class KotlinUuidGeneratorImpl(
    annotation: KotlinUuidGenerator,
    member: Member?,
    context: GeneratorCreationContext?
) : IdentifierGenerator {
    private val version: KotlinUuidVersion = annotation.version

    @OptIn(ExperimentalUuidApi::class)
    override fun generate(
        session: SharedSessionContractImplementor?,
        entity: Any?
    ): Any {
        return when (version) {
            KotlinUuidVersion.V4 -> Uuid.generateV4()
            KotlinUuidVersion.V7 -> Uuid.generateV7()
        }
    }
}