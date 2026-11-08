package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilAcompanante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PerfilAcompananteRepository extends JpaRepository<PerfilAcompanante, String> {
    // Buscar acompañantes ordenados por mejor calificación
    List<PerfilAcompanante> findAllByOrderByPromedioCalificacionDesc();

    // Buscar acompañantes con tarifa menor o igual a un monto máximo
    List<PerfilAcompanante> findByTarifaActualPorHoraLessThanEqual(BigDecimal tarifaMaxima);
}