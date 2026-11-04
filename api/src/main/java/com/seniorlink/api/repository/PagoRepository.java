package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagoRepository extends JpaRepository<Pago, UUID> {
    Optional<Pago> findByMpPagoId(String mpPagoId);
    Optional<Pago> findByReservaCodigoIdentificador(UUID reservaId);
}