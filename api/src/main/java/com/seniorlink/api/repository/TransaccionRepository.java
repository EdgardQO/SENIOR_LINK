package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface TransaccionRepository extends JpaRepository<Transaccion,UUID> {
 Optional<Transaccion> findByClaveIdempotencia(UUID clave);
 boolean existsByReservaIdAndEstado(Long reservaId,String estado);
}
