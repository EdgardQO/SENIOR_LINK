package com.seniorlink.api.converter;

import com.seniorlink.api.model.enums.EstadoPasarelaPago;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoPasarelaPagoConverter implements AttributeConverter<EstadoPasarelaPago, String> {

    @Override
    public String convertToDatabaseColumn(EstadoPasarelaPago attribute) {
        return attribute == null ? null : attribute.getValor();
    }

    @Override
    public EstadoPasarelaPago convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoPasarelaPago.desdeValor(dbData);
    }
}