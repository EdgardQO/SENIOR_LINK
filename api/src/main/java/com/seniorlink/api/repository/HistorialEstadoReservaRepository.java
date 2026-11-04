package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.HistorialEstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva, UUID> {
    List<HistorialEstadoReserva> findByReservaCodigoIdentificadorOrderByRegistradoEnDesc(UUID reservaId);
}