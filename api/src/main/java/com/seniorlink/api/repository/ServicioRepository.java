package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, UUID> {
    List<Servicio> findByEstaActivoTrue();
}