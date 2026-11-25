package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.BloqueoDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.List;
public interface BloqueoDisponibilidadRepository extends JpaRepository<BloqueoDisponibilidad,Long> {
 List<BloqueoDisponibilidad> findByAcompananteDni(String dni);
 boolean existsByAcompananteDniAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(String dni,OffsetDateTime fin,OffsetDateTime inicio);
}
