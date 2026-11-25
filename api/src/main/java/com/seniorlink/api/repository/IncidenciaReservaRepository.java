package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.IncidenciaReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IncidenciaReservaRepository extends JpaRepository<IncidenciaReserva,Long>{
 List<IncidenciaReserva> findByReservaId(Long id);
 boolean existsByReservaIdAndEstado(Long id,String estado);
}
