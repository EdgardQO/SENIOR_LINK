package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {
    Optional<Valoracion> findByReservaId(Long reservaId);
    List<Valoracion> findByAcompananteDni(String acompananteDni);
    @org.springframework.data.jpa.repository.Query("select v from Valoracion v join v.reserva r where r.acompanante.dni = :dni and v.acompanante.dni = :dni and r.estadoActual = :estado order by v.creadoEn desc")
    List<Valoracion> resenasDeReservas(@org.springframework.data.repository.query.Param("dni") String dni,
        @org.springframework.data.repository.query.Param("estado") com.seniorlink.api.model.enums.EstadoReserva estado);
}