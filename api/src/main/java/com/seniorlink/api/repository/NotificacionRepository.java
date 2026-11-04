package com.seniorlink.api.repository;

import com.seniorlink.api.model.entity.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {
    List<Notificacion> findByUsuarioCodigoIdentificadorAndEstaLeidaFalseOrderByCreadoEnDesc(UUID usuarioId);
}