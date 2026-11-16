package org.hibernate.type.descriptor.kotlin

import org.hibernate.Version
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
        if (isClassPresent("org.hibernate.type.descriptor.java.UuidCapableJavaType", classLoader)) {
            // hibernate 8+
            if (isClassPresent("kotlin.uuid.Uuid", classLoader)) {
                typeContributions.contributeJavaType(KotlinUuidJavaType.INSTANCE)
            }
        } else {
            // hibernate < 8 or unknown
            if (isClassPresent("kotlin.uuid.Uuid", classLoader)) {
                typeContributions.contributeJavaType(KotlinUuidNoIdJavaType.INSTANCE)
            }
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


    // read version here not to load extra class for all apps
    internal fun readHibernateVersion(): Triple<Int, Int, Int>? {
        val version = try {
            Version.getVersionString()
        } catch (_: LinkageError) {
            return null
        } catch (_: Exception) {
            return null
        }
        return parseHibernateVersion(version)
    }

    internal fun parseHibernateVersion(version: String?): Triple<Int, Int, Int>? {
        version?: return null
        val parts = version.substringBefore("-").split('.')
        if (parts.size < 3) {
            return null
        }

        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        return Triple(major, minor, patch)
    }
}
