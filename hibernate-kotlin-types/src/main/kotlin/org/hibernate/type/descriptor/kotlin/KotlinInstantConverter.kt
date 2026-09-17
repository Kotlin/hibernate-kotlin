package org.hibernate.type.descriptor.kotlin

import jakarta.persistence.AttributeConverter
import java.time.Instant
import kotlin.time.Instant.Companion.fromEpochSeconds

public class KotlinInstantConverter : AttributeConverter<kotlin.time.Instant?, Instant?> {
    public override fun convertToDatabaseColumn(attribute: kotlin.time.Instant?): Instant? {
        return if (attribute == null) null else Instant.ofEpochSecond(
            attribute.epochSeconds,
            attribute.nanosecondsOfSecond.toLong()
        )
    }

    public override fun convertToEntityAttribute(dbData: Instant?): kotlin.time.Instant? {
        return if (dbData == null) null else fromEpochSeconds(dbData.epochSecond, dbData.nano)
    }
}
