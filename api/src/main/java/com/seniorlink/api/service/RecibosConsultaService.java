package com.seniorlink.api.service;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.*;
import com.seniorlink.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
@Service
public class RecibosConsultaService {
 private final UsuarioRepository usuarios;
 private final TransaccionRepository transacciones;
 private final PagoRepository pagos;
 private final IncidenciaReservaRepository incidencias;
 private final ReservaLocalService motor;
 public RecibosConsultaService(UsuarioRepository usuarios,TransaccionRepository transacciones,PagoRepository pagos,IncidenciaReservaRepository incidencias,ReservaLocalService motor){
  this.usuarios=usuarios;this.transacciones=transacciones;this.pagos=pagos;this.incidencias=incidencias;this.motor=motor;
 }
 private ResponseStatusException no(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"No autorizado");}
 public record Recibo(String referencia,Long reservaId,String estado,BigDecimal monto,String moneda,OffsetDateTime fecha,String metodo,String ultimosCuatro){}

  @Transactional(readOnly=true)
 public List<Recibo> recibos(String dni){
  Usuario u=usuarios.findById(dni).orElseThrow(this::no);
  if(!Boolean.TRUE.equals(u.getEstaActivo()))throw no();
  String rol=u.getRol().getNombre();
  if(!Set.of("ADMINISTRADOR","SOPORTE","ACOMPANANTE","ADULTO_MAYOR","FAMILIAR").contains(rol))throw no();
  Set<Long> autorizadas=new HashSet<>();
  if("SOPORTE".equals(rol)){
   // Soporte consulta únicamente reservas relacionadas con casos registrados.
   autorizadas.addAll(incidencias.findAll().stream().map(i->i.getReserva().getId()).toList());
  }else{
   autorizadas.addAll(motor.listar(dni).stream().map(ReservaLocalService.ReservaVista::id).toList());
  }
  List<Recibo> resultado=new ArrayList<>();
  for(Long id:autorizadas){
   var operaciones=transacciones.findByReservaIdAndEstadoInOrderByCreadoEnDesc(id,List.of("APROBADO","REEMBOLSADO"));
   if(!operaciones.isEmpty()){
    for(Transaccion t:operaciones){
     Tarjeta tarjeta=t.getTarjeta();
     boolean titular=dni.equals(tarjeta.getTitularDni());
     String numero=tarjeta.getNumeroTarjeta();
     resultado.add(new Recibo(t.getId().toString(),id,t.getEstado(),t.getMonto(),"PEN",t.getCreadoEn(),
      titular?tarjeta.getTipoTarjeta():null,titular&&numero!=null&&numero.length()>=4?numero.substring(numero.length()-4):null));
    }
   }else{
    // Compatibilidad con pagos históricos sin una transacción de la pasarela local.
    pagos.findByReservaId(id).filter(p->p.getMpEstado()==EstadoPasarelaPago.APROBADO||p.getMpEstado()==EstadoPasarelaPago.REEMBOLSADO)
     .ifPresent(p->resultado.add(new Recibo(p.getMpPagoId()!=null?p.getMpPagoId():"PAGO-"+p.getId(),
      id,p.getMpEstado().name(),p.getMonto(),p.getMoneda(),p.getCreadoEn(),null,null)));
   }
  }
  return resultado.stream().sorted(Comparator.comparing(Recibo::fecha,Comparator.nullsLast(Comparator.reverseOrder()))).toList();
 }

}
