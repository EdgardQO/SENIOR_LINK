package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {
    Optional<Valoracion> findByReservaId(Long reservaId);
    List<Valoracion> findByAcompananteDni(String acompananteDni);
}