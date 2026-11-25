package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.DisponibilidadAcompanante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DisponibilidadAcompananteRepository extends JpaRepository<DisponibilidadAcompanante,Long> {
 List<DisponibilidadAcompanante> findByAcompananteDniAndEstaActivoTrue(String dni);
 List<DisponibilidadAcompanante> findByAcompananteDni(String dni);
 void deleteByAcompananteDni(String dni);
}
