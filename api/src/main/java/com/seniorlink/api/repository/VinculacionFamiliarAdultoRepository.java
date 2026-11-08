package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.VinculacionFamiliarAdulto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VinculacionFamiliarAdultoRepository extends JpaRepository<VinculacionFamiliarAdulto, Long> {

    // Obtener solicitud por ID validando que el adulto mayor sea el destinatario
    Optional<VinculacionFamiliarAdulto> findByIdAndAdultoMayorDni(Long id, String adultoMayorDni);

    // Listar solicitudes según el adulto mayor y estado (ej. 'PENDIENTE', 'ACEPTADO')
    List<VinculacionFamiliarAdulto> findByAdultoMayorDniAndEstado(String adultoMayorDni, String estado);

    // Listar vinculaciones de un familiar
    List<VinculacionFamiliarAdulto> findByFamiliarDni(String familiarDni);

    // Comprobar si ya existe una vinculación o solicitud previa entre ambos
    boolean existsByFamiliarDniAndAdultoMayorDni(String familiarDni, String adultoMayorDni);
}