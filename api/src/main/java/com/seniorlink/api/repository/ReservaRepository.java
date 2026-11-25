package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Reserva;
import com.seniorlink.api.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findByClienteDni(String clienteDni);
    List<Reserva> findByAcompananteDni(String acompananteDni);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id=:id")
    Optional<Reserva> bloquear(@Param("id") Long id);
    @Query("select r from Reserva r left join r.adultoMayor a left join r.solicitante s where a.dni=:dni or s.dni=:dni or r.cliente.dni=:dni")
    List<Reserva> visiblesCliente(@Param("dni") String dni);
    @Query("select count(r)>0 from Reserva r where r.acompanante.dni=:dni and r.fechaHoraInicio<:fin and r.fechaHoraFin>:inicio and r.estadoActual in :estados")
    boolean hayCruce(@Param("dni") String dni,@Param("inicio") OffsetDateTime inicio,@Param("fin") OffsetDateTime fin,@Param("estados") Collection<EstadoReserva> estados);
    List<Reserva> findByEstadoActual(EstadoReserva estadoActual);
}