package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.MetodoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MetodoPagoRepository extends JpaRepository<MetodoPago,UUID> {
 List<MetodoPago> findByUsuarioDniAndActivoTrue(String dni);
 Optional<MetodoPago> findByIdAndUsuarioDniAndActivoTrue(UUID id,String dni);
}
