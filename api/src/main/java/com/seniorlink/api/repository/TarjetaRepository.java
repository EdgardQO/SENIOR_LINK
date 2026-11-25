package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.Tarjeta;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface TarjetaRepository extends JpaRepository<Tarjeta,Long> {
 Optional<Tarjeta> findByNumeroTarjeta(String numeroTarjeta);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select t from Tarjeta t where t.numeroTarjeta=:numero")
 Optional<Tarjeta> bloquearPorNumero(@Param("numero") String numero);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select t from Tarjeta t where t.id=:id")
 Optional<Tarjeta> bloquear(@Param("id") Long id);
}
