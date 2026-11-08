package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.HistorialEstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva, Long> {
    List<HistorialEstadoReserva> findByReservaIdOrderByRegistradoEnDesc(Long reservaId);
}