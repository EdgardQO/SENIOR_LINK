package com.seniorlink.api.service;

import com.seniorlink.api.dto.TotalUsuariosResponseDTO;
import com.seniorlink.api.dto.UsuarioDTO;
import com.seniorlink.api.model.entity.Usuario;
import com.seniorlink.api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public TotalUsuariosResponseDTO obtenerUsuariosConTotal() {
        List<Usuario> lista = usuarioRepository.findAll();
        List<UsuarioDTO> dtos = lista.stream()
                .map(UsuarioDTO::desdeEntidad)
                .toList();
        return new TotalUsuariosResponseDTO(dtos.size(), dtos);
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listarTodosLosUsuarios() {
        return usuarioRepository.findAll()
                .stream()
                .map(UsuarioDTO::desdeEntidad)
                .toList();
    }
}