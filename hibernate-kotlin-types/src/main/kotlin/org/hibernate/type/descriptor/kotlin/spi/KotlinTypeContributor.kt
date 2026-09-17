package org.hibernate.type.descriptor.kotlin.spi

import org.hibernate.boot.model.TypeContributions
import org.hibernate.boot.model.TypeContributor
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.spi.MetadataBuilderInitializer
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.descriptor.kotlin.KotlinDurationJavaType
import org.hibernate.type.descriptor.kotlin.KotlinInstantConverter
import org.hibernate.type.descriptor.kotlin.KotlinUuidJavaType

public class KotlinTypeContributor : TypeContributor, MetadataBuilderInitializer {
    public override fun contribute(typeContributions: TypeContributions, serviceRegistry: ServiceRegistry?) {
        typeContributions.contributeJavaType(KotlinUuidJavaType.INSTANCE)
        typeContributions.contributeJavaType(KotlinDurationJavaType.INSTANCE)
    }

    public override fun contribute(
        metadataBuilder: MetadataBuilder,
        serviceRegistry: StandardServiceRegistry,
    ) {
        metadataBuilder.applyAttributeConverter(KotlinInstantConverter::class.java, true)
    }
}
