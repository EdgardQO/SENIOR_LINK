package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.HistorialEstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva,UUID> {
 List<HistorialEstadoReserva> findByReservaIdOrderByRegistradoEnDesc(Long reservaId);
}
