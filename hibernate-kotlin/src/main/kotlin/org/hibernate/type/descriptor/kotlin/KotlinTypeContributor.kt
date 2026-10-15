package org.hibernate.type.descriptor.kotlin

import org.hibernate.boot.model.TypeContributions
import org.hibernate.boot.model.TypeContributor
import org.hibernate.service.ServiceRegistry

/**
 * Contributes descriptors for types from Kotlin standard library.
 */
public class KotlinTypeContributor : TypeContributor {
    public override fun contribute(typeContributions: TypeContributions, serviceRegistry: ServiceRegistry?) {
        contribute(typeContributions, KotlinTypeContributor::class.java.classLoader)
    }

    internal fun contribute(typeContributions: TypeContributions, classLoader: ClassLoader) {
        if (isClassPresent("kotlin.uuid.Uuid", classLoader)) {
            typeContributions.contributeJavaType(KotlinUuidJavaType.INSTANCE)
        }
        if (isClassPresent("kotlin.time.Duration", classLoader)) {
            typeContributions.contributeJavaType(KotlinDurationJavaType.INSTANCE)
        }
        if (isClassPresent("kotlin.time.Instant", classLoader)) {
            typeContributions.contributeJavaType(KotlinInstantJavaType.INSTANCE)
        }
    }

    private fun isClassPresent(className: String, classLoader: ClassLoader): Boolean =
        try {
            Class.forName(className, false, classLoader)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
}
