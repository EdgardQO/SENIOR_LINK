package com.seniorlink.api.controller;

import com.seniorlink.api.service.*;
import com.seniorlink.api.model.entity.Servicio;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

/**
 * Funciones de acompañamiento que deben estar disponibles tanto en localhost
 * como en el VPS, sin depender del perfil local ni habilitar cobros simulados.
 */
@RestController
@RequestMapping("/api/v1/operaciones")
@PreAuthorize("isAuthenticated()")
public class OperacionesController {
 private final ReservaLocalService reservas;
 private final PostServicioLocalService postServicio;
 private final ConfiguracionLocalService configuracion;
 private final RecibosConsultaService recibos;
 @org.springframework.beans.factory.annotation.Value("${seniorlink.pagos.simulacion-habilitada:false}") private boolean simulacion;

 public OperacionesController(ReservaLocalService reservas, PostServicioLocalService postServicio, ConfiguracionLocalService configuracion, RecibosConsultaService recibos) {
   this.reservas=reservas;
   this.postServicio=postServicio;
   this.configuracion=configuracion;this.recibos=recibos;
 }

 @GetMapping("/capacidades")
 public Map<String,Boolean> capacidades(){return Map.of("pagosSimulados",simulacion);}
 @GetMapping("/recibos")
 @PreAuthorize("hasAnyRole('ADMINISTRADOR','SOPORTE','ACOMPANANTE','ADULTO_MAYOR','FAMILIAR')")
 public List<RecibosConsultaService.Recibo> recibos(Authentication auth){return recibos.recibos(auth.getName());}
 @GetMapping("/servicios")
 public List<Servicio> servicios(){return reservas.servicios();}
 @GetMapping("/acompanantes")
 public List<ReservaLocalService.AcompananteVista> acompanantes(){return reservas.acompanantes();}
 @GetMapping("/acompanantes/{dni}/resenas")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ADMINISTRADOR')")
 public List<PostServicioLocalService.ResenaPublica> resenas(@PathVariable String dni){
  return postServicio.resenasAcompanante(dni);
 }
 @GetMapping("/acompanantes/{dni}/disponibilidad")
 public List<ReservaLocalService.Horario> disponibilidadDe(@PathVariable String dni){
  return reservas.disponibilidad(dni);
 }
 @GetMapping("/acompanantes/{dni}/ocupacion")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ACOMPANANTE','ADMINISTRADOR')")
 public List<ReservaLocalService.IntervaloOcupado> ocupacion(@PathVariable String dni,
  @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime inicio,
  @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime fin){
  return reservas.horariosOcupados(dni,inicio,fin);
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
 @GetMapping("/parametros-reserva")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ADMINISTRADOR')")
 public ConfiguracionLocalService.Parametros parametrosReserva(){return configuracion.obtener();}
 @PostMapping("/reservas/{id}/jornadas/{jornadaId}/{accion:iniciar|finalizar|confirmar}")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ACOMPANANTE')")
 public ReservaLocalService.ReservaVista cambiarJornada(Authentication auth,@PathVariable Long id,@PathVariable Long jornadaId,@PathVariable String accion){
  return reservas.accionJornada(auth.getName(),id,jornadaId,accion.toUpperCase(Locale.ROOT));
 }
 public record WhatsappConsentimiento(boolean autorizar,boolean mayorDeEdad){}
 @GetMapping("/reservas/{id}/whatsapp")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ACOMPANANTE')")
 public ReservaLocalService.ContactoWhatsapp consultarWhatsapp(Authentication auth,@PathVariable Long id){
  return reservas.consultarWhatsapp(auth.getName(),id);
 }
 @PutMapping("/reservas/{id}/whatsapp")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR','ACOMPANANTE')")
 public ReservaLocalService.ContactoWhatsapp autorizarWhatsapp(Authentication auth,@PathVariable Long id,@RequestBody WhatsappConsentimiento input){
  return reservas.autorizarWhatsapp(auth.getName(),id,input.autorizar(),input.mayorDeEdad());
 }
 @GetMapping("/parentescos")
 @PreAuthorize("hasAnyRole('ADULTO_MAYOR','FAMILIAR')")
 public List<Map<String,Object>> parentescos(){return reservas.catalogoParentescos();}
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
