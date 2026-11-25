package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.Resena;
import com.seniorlink.api.model.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;
public interface ResenaRepository extends JpaRepository<Resena,UUID> {
 java.util.Optional<Resena> findByReservaId(Long reservaId);
 @Query("select s from Resena s join s.reserva r where r.acompanante.dni=:dni and r.estadoActual=:estado order by s.creadoEn desc")
 List<Resena> resenasDeReservas(@Param("dni") String dni,@Param("estado") EstadoReserva estado);
}
