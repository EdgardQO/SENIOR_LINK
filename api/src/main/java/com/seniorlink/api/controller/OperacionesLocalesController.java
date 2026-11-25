package com.seniorlink.api.controller;

import com.seniorlink.api.service.*;
import com.seniorlink.api.model.entity.Servicio;
import com.seniorlink.api.model.entity.DisponibilidadAcompanante;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/local")
@Profile("local")
@ConditionalOnProperty(name="seniorlink.pagos.simulacion-habilitada",havingValue="true")
@PreAuthorize("isAuthenticated()")
public class OperacionesLocalesController {
 private final ReservaLocalService reservas;
 private final PasarelaLocalService pagos;
 private final PostServicioLocalService postServicio;
 public OperacionesLocalesController(ReservaLocalService reservas,PasarelaLocalService pagos,PostServicioLocalService postServicio){
  this.reservas=reservas;this.pagos=pagos;this.postServicio=postServicio;
 }
 @GetMapping("/servicios")
 public List<Servicio> servicios(){return reservas.servicios();}
 @GetMapping("/acompanantes")
 public List<ReservaLocalService.AcompananteVista> acompanantes(){return reservas.acompanantes();}
 @GetMapping("/acompanantes/{dni}/disponibilidad")
 public List<ReservaLocalService.Horario> disponibilidadDe(@PathVariable String dni){
  return reservas.disponibilidad(dni);
 }
 @GetMapping("/mi-disponibilidad")
 @PreAuthorize("hasRole('ACOMPANANTE')")
 public List<ReservaLocalService.Horario> miDisponibilidad(Authentication auth){
  return reservas.disponibilidad(auth.getName());
 }
 @PutMapping("/mi-disponibilidad")
 @PreAuthorize("hasRole('ACOMPANANTE')")
 public List<ReservaLocalService.Horario> guardarDisponibilidad(Authentication auth,
  @RequestBody List<ReservaLocalService.Horario> horarios){
  return reservas.guardarDisponibilidad(auth.getName(),horarios);
 }
 public record TarifaInput(BigDecimal tarifa){}
 @PutMapping("/mi-tarifa")
 @PreAuthorize("hasRole('ACOMPANANTE')")
 public Map<String,BigDecimal> actualizarTarifa(Authentication auth,@RequestBody TarifaInput input){
  return Map.of("tarifa",reservas.tarifa(auth.getName(),input.tarifa()));
 }
 @PostMapping("/reservas")
 @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public ReservaLocalService.ReservaVista reservar(Authentication auth,
  @RequestBody ReservaLocalService.NuevaReserva input){
  return reservas.crear(auth.getName(),input);
 }
 @GetMapping("/reservas")
 public List<ReservaLocalService.ReservaVista> listar(Authentication auth){
  return reservas.listar(auth.getName());
 }
 @GetMapping("/reservas/{id}")
 public ReservaLocalService.ReservaVista reserva(Authentication auth,@PathVariable Long id){
  return reservas.obtener(auth.getName(),id);
 }
 @PostMapping("/reservas/{id}/{accion:aceptar|rechazar|iniciar|finalizar|confirmar|cancelar}")
 public ReservaLocalService.ReservaVista cambiar(Authentication auth,@PathVariable Long id,
  @PathVariable String accion){
  return reservas.accion(auth.getName(),id,accion.toUpperCase(Locale.ROOT));
 }
 @GetMapping("/vinculos")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public List<ReservaLocalService.VinculoVista> vinculos(Authentication auth){
  return reservas.vinculos(auth.getName());
 }
 public record SolicitudVinculo(String otroDni,Short parentescoId){}
 @PostMapping("/vinculos")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public ReservaLocalService.VinculoVista vincular(Authentication auth,@RequestBody SolicitudVinculo input){
  return reservas.solicitarVinculo(auth.getName(),input.otroDni(),input.parentescoId());
 }
 public record Consentimiento(boolean aprobar){}
 @PostMapping("/vinculos/{id}/responder")
 @PreAuthorize("hasRole('ADULTO_MAYOR')")
 public ReservaLocalService.VinculoVista responder(Authentication auth,@PathVariable Long id,
  @RequestBody Consentimiento input){
  return reservas.responderVinculo(auth.getName(),id,input.aprobar());
 }
 @GetMapping("/metodos-pago")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public List<PasarelaLocalService.MetodoVista> metodos(Authentication auth){
  return pagos.listar(auth.getName());
 }
 @PostMapping("/metodos-pago")
 @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public PasarelaLocalService.MetodoVista agregarMetodo(Authentication auth,
  @RequestBody PasarelaLocalService.NuevaTarjeta input){
  return pagos.agregar(auth.getName(),input);
 }
 @DeleteMapping("/metodos-pago/{id}")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public void quitarMetodo(Authentication auth,@PathVariable UUID id){pagos.quitar(auth.getName(),id);}
 @PostMapping("/reservas/{id}/pagar")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public PasarelaLocalService.Resultado pagar(Authentication auth,@PathVariable Long id,
  @RequestBody PasarelaLocalService.PagoRequest input){
  return pagos.pagar(auth.getName(),id,input);
 }
 @PostMapping("/admin/reservas/{id}/reembolsar")
 @PreAuthorize("hasRole('ADMINISTRADOR')")
 public PasarelaLocalService.Resultado reembolsar(Authentication auth,@PathVariable Long id){
  return pagos.reembolsar(auth.getName(),id);
 }
 public record IncidenciaInput(String descripcion){}
 @GetMapping("/incidencias")
 public List<PostServicioLocalService.IncidenciaVista> incidencias(Authentication auth){
  return postServicio.incidencias(auth.getName());
 }
 @PostMapping("/reservas/{id}/incidencias")
 @PreAuthorize("hasAnyRole('ACOMPANANTE','ADULTO_MAYOR','FAMILIAR')")
 public PostServicioLocalService.IncidenciaVista reportar(Authentication auth,@PathVariable Long id,
  @RequestBody IncidenciaInput input){
  return postServicio.reportar(auth.getName(),id,input.descripcion());
 }
 public record ResolverIncidencia(String resolucion){}
 @PostMapping("/incidencias/{id}/resolver")
 @PreAuthorize("hasAnyRole('SOPORTE','ADMINISTRADOR')")
 public PostServicioLocalService.IncidenciaVista resolver(Authentication auth,@PathVariable Long id,
  @RequestBody ResolverIncidencia input){
  return postServicio.resolver(auth.getName(),id,input.resolucion());
 }
 @GetMapping("/liquidaciones")
 @PreAuthorize("hasAnyRole('ADMINISTRADOR','ACOMPANANTE')")
 public List<PostServicioLocalService.LiquidacionVista> liquidaciones(Authentication auth){
  return postServicio.liquidaciones(auth.getName());
 }
 @PostMapping("/liquidaciones/{reservaId}")
 @PreAuthorize("hasRole('ADMINISTRADOR')")
 public PostServicioLocalService.LiquidacionVista liquidar(Authentication auth,@PathVariable Long reservaId){
  return postServicio.liquidar(auth.getName(),reservaId);
 }
 public record ValoracionInput(short puntuacion,String comentario){}
 @PostMapping("/reservas/{id}/valorar")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public PostServicioLocalService.ValoracionVista valorar(Authentication auth,@PathVariable Long id,
  @RequestBody ValoracionInput input){
  return postServicio.valorar(auth.getName(),id,input.puntuacion(),input.comentario());
 }

}
