package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilAcompanante;
import com.seniorlink.api.model.enums.EstadoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerfilAcompananteRepository extends JpaRepository<PerfilAcompanante, UUID> {

    // Buscar acompañantes filtrando por estado de validación (ej. solo APROBADO para el catálogo)
    List<PerfilAcompanante> findByEstadoVerificacion(EstadoVerificacion estadoVerificacion);

    // Buscar acompañantes vinculados a una institución educativa aliada específica
    List<PerfilAcompanante> findByInstitucionCodigoIdentificador(UUID institucionId);
}