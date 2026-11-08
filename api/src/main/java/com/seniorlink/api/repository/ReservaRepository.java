package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Reserva;
import com.seniorlink.api.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByClienteDni(String clienteDni);
    List<Reserva> findByAcompananteDni(String acompananteDni);
    List<Reserva> findByEstadoActual(EstadoReserva estadoActual);
}