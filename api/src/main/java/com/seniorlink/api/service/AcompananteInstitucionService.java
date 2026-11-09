package com.seniorlink.api.service;

import com.seniorlink.api.dto.InstitucionAliadaDTO;
import com.seniorlink.api.model.entity.InstitucionAliada;
import com.seniorlink.api.model.entity.PerfilAcompanante;
import com.seniorlink.api.repository.InstitucionAliadaRepository;
import com.seniorlink.api.repository.PerfilAcompananteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@Service
public class AcompananteInstitucionService {

    private final PerfilAcompananteRepository acompananteRepository;
    private final InstitucionAliadaRepository institucionRepository;

    public AcompananteInstitucionService(
            PerfilAcompananteRepository acompananteRepository,
            InstitucionAliadaRepository institucionRepository) {
        this.acompananteRepository = acompananteRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(readOnly = true)
    public List<InstitucionAliadaDTO> listarInstituciones() {
        return institucionRepository.findAll().stream()
                .sorted(Comparator.comparing(InstitucionAliada::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(InstitucionAliadaDTO::desdeEntidad)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitucionAliadaDTO> listarPorAcompanante(String dni) {
        return buscarAcompanante(dni).getInstitucionesAliadas().stream()
                .sorted(Comparator.comparing(InstitucionAliada::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(InstitucionAliadaDTO::desdeEntidad)
                .toList();
    }

    @Transactional
    public void vincular(String dni, String ruc) {
        validarRuc(ruc);
        PerfilAcompanante acompanante = buscarAcompanante(dni);
        InstitucionAliada institucion = institucionRepository.findById(ruc)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La institución aliada no existe"));

        boolean yaVinculado = acompanante.getInstitucionesAliadas().stream()
                .anyMatch(i -> i.getRuc().equals(ruc));
        if (yaVinculado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El acompañante ya está vinculado a esta institución");
        }
        acompanante.getInstitucionesAliadas().add(institucion);
        acompananteRepository.save(acompanante);
    }

    @Transactional
    public void desvincular(String dni, String ruc) {
        validarRuc(ruc);
        PerfilAcompanante acompanante = buscarAcompanante(dni);
        boolean eliminado = acompanante.getInstitucionesAliadas()
                .removeIf(i -> i.getRuc().equals(ruc));
        if (!eliminado) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe la vinculación indicada");
        }
        acompananteRepository.save(acompanante);
    }

    private PerfilAcompanante buscarAcompanante(String dni) {
        return acompananteRepository.findById(dni)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe un perfil de acompañante con DNI " + dni));
    }

    private void validarRuc(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El RUC debe contener exactamente 11 dígitos");
        }
    }
}
