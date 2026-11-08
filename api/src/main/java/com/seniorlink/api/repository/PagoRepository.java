package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findByMpPagoId(String mpPagoId);
    Optional<Pago> findByReservaId(Long reservaId);
}