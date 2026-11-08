package com.seniorlink.api.converter;

import com.seniorlink.api.model.enums.EstadoReserva;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoReservaConverter implements AttributeConverter<EstadoReserva, String> {

    @Override
    public String convertToDatabaseColumn(EstadoReserva attribute) {
        return attribute == null ? null : attribute.getValor();
    }

    @Override
    public EstadoReserva convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoReserva.desdeValor(dbData);
    }
}