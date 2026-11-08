package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface DescuentoRepository extends JpaRepository<Descuento, Long> {

    @Query("SELECT d FROM Descuento d WHERE d.codigoCupon = :cupon AND d.estaActivo = true " +
            "AND :ahora BETWEEN d.validoDesde AND d.validoHasta " +
            "AND (d.limiteUsos IS NULL OR d.cantidadUsada < d.limiteUsos)")
    Optional<Descuento> findCuponValido(@Param("cupon") String cupon, @Param("ahora") OffsetDateTime ahora);
}