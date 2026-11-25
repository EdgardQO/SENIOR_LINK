package com.seniorlink.api;

import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.model.enums.EstadoReserva;
import com.seniorlink.api.repository.ReservaRepository;
import com.seniorlink.api.repository.UsuarioRepository;
import com.seniorlink.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class AgendaReservasIntegrationTests {
 private static final String ADULTO="97000001", ACOMP="97000002";
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 @Autowired RegistroUsuarioService registro;
 @Autowired UsuarioRepository usuarios;
 @Autowired ReservaRepository repo;
 @Autowired ReservaLocalService agenda;
 @Autowired PasarelaLocalService pagos;
 private OffsetDateTime inicio,fin;
 private Long servicio;

 @BeforeEach void preparar(){
  registro.registrar(new RegistroUsuarioDTO(ADULTO,"Adulto","Agenda",
    "adulto.agenda@seniorlink.test","ClaveLocalSolo123",null,"ADULTO_MAYOR",
    new RegistroUsuarioDTO.AdultoMayor(LocalDate.of(1950,2,5),"Lima","Contacto","900000000",null),null,null));
  registro.registrar(new RegistroUsuarioDTO(ACOMP,"Acompañante","Agenda",
    "acompanante.agenda@seniorlink.test","ClaveLocalSolo123",null,"ACOMPANANTE",
    null,null,new RegistroUsuarioDTO.Acompanante("Agenda",5,"Psicología",new BigDecimal("25.00"),new BigDecimal("25.00"),null)));
  var u=usuarios.findById(ACOMP).orElseThrow();
  u.setFotoPerfilRuta(ACOMP+"/perfil-test.png");u.revisarFoto("VERIFICADO",ADULTO,null);usuarios.saveAndFlush(u);
  List<ReservaLocalService.Horario> horarios=new ArrayList<>();
  for(short d=1;d<=7;d++)horarios.add(new ReservaLocalService.Horario(d,LocalTime.of(8,0),LocalTime.of(20,0)));
  agenda.guardarDisponibilidad(ACOMP,horarios);
  inicio=LocalDate.now(LIMA).plusDays(3).atTime(10,0).atZone(LIMA).toOffsetDateTime();
  fin=inicio.plusHours(2);servicio=agenda.servicios().getFirst().getId();
 }
 private ReservaLocalService.ReservaVista crear(OffsetDateTime i,OffsetDateTime f){
  return agenda.crear(ADULTO,new ReservaLocalService.NuevaReserva(ADULTO,ACOMP,servicio,i,f,"Lima","Agenda"));
 }
 private int ocupados(){return agenda.horariosOcupados(ACOMP,inicio,fin).size();}
 @Test void soloAceptacionOcupaYRechazaCruce(){
  var a=crear(inicio,fin);var b=crear(inicio.plusHours(1),fin.plusHours(1));
  assertEquals(0,ocupados());
  agenda.accion(ACOMP,a.id(),"ACEPTAR");
  assertEquals(1,ocupados());
  var consecutiva=crear(fin,fin.plusHours(1));
  assertEquals("PENDIENTE_PAGO",agenda.accion(ACOMP,consecutiva.id(),"ACEPTAR").estado());
  assertEquals(1,ocupados());
  assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,()->agenda.accion(ACOMP,b.id(),"ACEPTAR")).getStatusCode());
 }
 @Test void cancelaSinPagoYPermiteNuevaAceptacion(){
  var a=crear(inicio,fin);var b=crear(inicio,fin);
  agenda.accion(ACOMP,a.id(),"ACEPTAR");
  assertEquals("CANCELADA",agenda.accion(ADULTO,a.id(),"CANCELAR").estado());
  assertEquals(0,ocupados());
  assertEquals("PENDIENTE_PAGO",agenda.accion(ACOMP,b.id(),"ACEPTAR").estado());
  assertEquals(1,ocupados());
 }
 @Test void pagoVencidoLiberaSinEsperarTarea(){
  var a=crear(inicio,fin);var b=crear(inicio,fin);
  agenda.accion(ACOMP,a.id(),"ACEPTAR");
  var r=repo.findById(a.id()).orElseThrow();
  r.setVencePagoEn(OffsetDateTime.now().minusMinutes(1));repo.saveAndFlush(r);
  assertEquals(0,ocupados());
  assertEquals("EXPIRADA",agenda.obtener(ADULTO,a.id()).estado());
  assertEquals("PENDIENTE_PAGO",agenda.accion(ACOMP,b.id(),"ACEPTAR").estado());
  agenda.procesarPlazos();
  assertEquals(EstadoReserva.EXPIRADA,repo.findById(a.id()).orElseThrow().getEstadoActual());
 }
 @Test void cancelacionPagadaLiberaYConservaFlujoReembolso(){
  var a=crear(inicio,fin);var b=crear(inicio,fin);
  agenda.accion(ACOMP,a.id(),"ACEPTAR");
  var tarjeta=pagos.agregar(ADULTO,new PasarelaLocalService.NuevaTarjeta(null,"Adulto Agenda","DEBITO",(short)12,(short)2090,"123"));
  assertEquals("APROBADO",pagos.pagar(ADULTO,a.id(),new PasarelaLocalService.PagoRequest(tarjeta.id(),UUID.randomUUID(),"123")).estado());
  assertEquals(1,ocupados());
  assertEquals("CANCELADA",agenda.accion(ADULTO,a.id(),"CANCELAR").estado());
  assertEquals(0,ocupados());
  assertEquals("PENDIENTE_PAGO",agenda.accion(ACOMP,b.id(),"ACEPTAR").estado());
 }
 @Test void finalizacionLiberaAntesDeConfirmacion(){
  var a=crear(inicio,fin);var b=crear(inicio,fin);
  agenda.accion(ACOMP,a.id(),"ACEPTAR");
  // Preparamos el estado operativo sin esperar tres días ni usar pagos reales.
  var r=repo.findById(a.id()).orElseThrow();
  r.setEstadoActual(EstadoReserva.EN_PROGRESO);
  r.setInicioRealEn(OffsetDateTime.now().minusHours(1));repo.saveAndFlush(r);
  assertEquals(1,ocupados());
  assertEquals("PENDIENTE_CONFIRMACION",agenda.accion(ACOMP,a.id(),"FINALIZAR").estado());
  assertEquals(0,ocupados());
  assertEquals("COMPLETADA",agenda.accion(ADULTO,a.id(),"CONFIRMAR").estado());
  assertEquals("PENDIENTE_PAGO",agenda.accion(ACOMP,b.id(),"ACEPTAR").estado());
 }
 @Test void noPuedeQuitarHorarioConReservaAceptada(){
  var a=crear(inicio,fin);agenda.accion(ACOMP,a.id(),"ACEPTAR");
  assertEquals(HttpStatus.CONFLICT,assertThrows(ResponseStatusException.class,()->agenda.guardarDisponibilidad(ACOMP,List.of())).getStatusCode());
 }
}
