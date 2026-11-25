package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.LiquidacionAcompanante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface LiquidacionAcompananteRepository extends JpaRepository<LiquidacionAcompanante,Long>{
 Optional<LiquidacionAcompanante> findByReservaId(Long reservaId);
}
