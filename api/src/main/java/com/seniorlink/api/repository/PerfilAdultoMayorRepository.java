package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.PerfilAdultoMayor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PerfilAdultoMayorRepository extends JpaRepository<PerfilAdultoMayor, UUID> {

    // Listar todos los adultos mayores registrados por un familiar específico
    List<PerfilAdultoMayor> findByFamiliarCodigoIdentificador(UUID familiarId);
}