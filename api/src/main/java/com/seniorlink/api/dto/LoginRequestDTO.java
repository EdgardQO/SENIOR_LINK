package com.seniorlink.api.dto;

public record LoginRequestDTO(
        String correoElectronico,
        String contrasena
) {}