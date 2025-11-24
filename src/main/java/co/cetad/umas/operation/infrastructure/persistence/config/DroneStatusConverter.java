package co.cetad.umas.operation.infrastructure.persistence.config;

import co.cetad.umas.operation.domain.model.entity.DroneStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DroneStatusConverter implements AttributeConverter<DroneStatus, String> {

    @Override
    public String convertToDatabaseColumn(DroneStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public DroneStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : DroneStatus.valueOf(dbData);
    }

}