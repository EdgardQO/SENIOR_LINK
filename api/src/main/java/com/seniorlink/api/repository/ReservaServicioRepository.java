package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.ReservaServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaServicioRepository extends JpaRepository<ReservaServicio, Long> {
    List<ReservaServicio> findByReservaId(Long reservaId);
}