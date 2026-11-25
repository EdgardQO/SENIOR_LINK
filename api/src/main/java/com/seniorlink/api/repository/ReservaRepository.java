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
    // La agenda se deriva de reservas aceptadas, no de solicitudes pendientes.
    // Los intervalos son [inicio, fin): dos servicios consecutivos no se cruzan.
    String OCUPACION = """
       (r.estadoActual in :firmes
        or (r.estadoActual = :pendientePago and (r.vencePagoEn is null or r.vencePagoEn > :ahora))
        or (r.estadoActual = :disputa and r.finRealEn is null and not exists
          (select h.identificadorHistorial from HistorialEstadoReserva h where h.reserva = r
            and (h.estado = :completada or h.estado = :cancelada
                 or h.motivoODetalle like 'Cancelación con pago:%'))))
       """;

    @Query("select r from Reserva r where r.acompanante.dni=:dni and r.id<>:excluir and r.fechaHoraInicio<:fin and r.fechaHoraFin>:inicio and " + OCUPACION + " order by r.fechaHoraInicio, r.id")
    List<Reserva> ocupacion(@Param("dni") String dni, @Param("inicio") OffsetDateTime inicio,
      @Param("fin") OffsetDateTime fin, @Param("excluir") Long excluir, @Param("ahora") OffsetDateTime ahora,
      @Param("firmes") Collection<EstadoReserva> firmes, @Param("pendientePago") EstadoReserva pendientePago,
      @Param("disputa") EstadoReserva disputa, @Param("completada") EstadoReserva completada,
      @Param("cancelada") EstadoReserva cancelada);

    @Query("select r.acompanante.dni from Reserva r where r.id=:id")
    Optional<String> dniAcompanante(@Param("id") Long id);

    @Query("select r.id from Reserva r where r.estadoActual in :estados order by r.id")
    List<Long> idsPorEstados(@Param("estados") Collection<EstadoReserva> estados);

    @Query("select count(r)>0 from Reserva r where r.acompanante.dni=:dni and r.id<>:excluir and r.estadoActual=:estado")
    boolean existeOtroEnEstado(@Param("dni") String dni,@Param("excluir") Long excluir,@Param("estado") EstadoReserva estado);

    List<Reserva> findByEstadoActual(EstadoReserva estadoActual);
}