package com.seniorlink.api.dto;

import com.seniorlink.api.model.entity.InstitucionAliada;

public record InstitucionAliadaDTO(
        String ruc,
        String nombre,
        String detallesConvenio
) {
    public static InstitucionAliadaDTO desdeEntidad(InstitucionAliada institucion) {
        return new InstitucionAliadaDTO(
                institucion.getRuc(),
                institucion.getNombre(),
                institucion.getDetallesConvenio()
        );
    }
}
