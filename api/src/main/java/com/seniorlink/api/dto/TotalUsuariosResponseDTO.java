package com.seniorlink.api.dto;

import java.util.List;

public record TotalUsuariosResponseDTO(
        long totalUsuarios,
        List<UsuarioDTO> usuarios
) {}