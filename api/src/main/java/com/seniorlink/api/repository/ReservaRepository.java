package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, UUID> {
    List<Reserva> findByFamiliarCodigoIdentificador(UUID familiarId);
    List<Reserva> findByAcompananteCodigoIdentificador(UUID acompananteId);
}