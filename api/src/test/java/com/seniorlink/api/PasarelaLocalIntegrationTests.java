package com.seniorlink.api;

import com.seniorlink.api.dto.RegistroUsuarioDTO;
import com.seniorlink.api.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class PasarelaLocalIntegrationTests {
 @Autowired RegistroUsuarioService registro;
 @Autowired com.seniorlink.api.repository.UsuarioRepository usuarios;
 @Autowired ReservaLocalService reservas;
 @Autowired com.seniorlink.api.repository.TarjetaRepository tarjetas;
 @Autowired org.springframework.security.crypto.password.PasswordEncoder encoder;
 private String provisionarTarjeta(){
  var t=new com.seniorlink.api.model.entity.Tarjeta();
  t.setTitularDni("95000001");t.setNumeroTarjeta("DEMO-97000001");t.setNombreTitular("Adulto Demo");
  t.setTipoTarjeta("DEBITO");t.setMesVencimiento((short)12);t.setAnioVencimiento((short)2090);
  t.setCvvHash(encoder.encode("123"));t.setSaldo(new BigDecimal("500.00"));tarjetas.saveAndFlush(t);
  return t.getNumeroTarjeta();
 }
 @Autowired PasarelaLocalService pasarela;

 @Test void rechazaTarjetaNoRegistrada(){
  registro.registrar(new RegistroUsuarioDTO("95000001","Adulto","Demo",
   "adulto.it.demo@seniorlink.test","ClaveLocalSolo123",null,"ADULTO_MAYOR",
   new RegistroUsuarioDTO.AdultoMayor(LocalDate.of(1950,2,5),"Lima","Contacto","900000000",null),null,null));
  long antes=tarjetas.count();
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->pasarela.agregar("95000001",
   new PasarelaLocalService.NuevaTarjeta("DEMO-99999999","Adulto Demo","DEBITO",(short)12,(short)2090,"123")));
  assertEquals(antes,tarjetas.count());
 }
 @Test void pagoSimuladoExitosoYRechazoPorFondosInsuficientes() {
  RegistroUsuarioDTO adulto=new RegistroUsuarioDTO("95000001","Adulto","Demo",
   "adulto.it.demo@seniorlink.test","ClaveLocalSolo123",null,"ADULTO_MAYOR",
   new RegistroUsuarioDTO.AdultoMayor(LocalDate.of(1950,2,5),"Lima","Contacto",
    "900000000",null),null,null);
  RegistroUsuarioDTO acomp=new RegistroUsuarioDTO("95000002","Acompañante","Demo",
   "acompanante.it.demo@seniorlink.test","ClaveLocalSolo123",null,"ACOMPANANTE",
   null,null,new RegistroUsuarioDTO.Acompanante("Prueba",5,"Informática",
    new BigDecimal("50.00"),new BigDecimal("50.00"),null));
  registro.registrar(adulto);
  registro.registrar(acomp);
  var verificado=usuarios.findById("95000002").orElseThrow();
  verificado.setFotoPerfilRuta("95000002/perfil-test.png");
  verificado.revisarFoto("VERIFICADO","95000001",null);
  usuarios.saveAndFlush(verificado);

  List<ReservaLocalService.Horario> semana=new ArrayList<>();
  for(short d=1;d<=7;d++)
   semana.add(new ReservaLocalService.Horario(d,LocalTime.of(8,0),LocalTime.of(20,0)));
  reservas.guardarDisponibilidad("95000002",semana);

  LocalDate base=LocalDate.now(ZoneId.of("America/Lima")).plusDays(3);
  Long servicio=reservas.servicios().getFirst().getId();
  var primera=reserva("95000001","95000002",servicio,base);
  assertEquals("PENDIENTE_ACEPTACION",primera.estado());
  assertEquals("PENDIENTE_PAGO",reservas.accion("95000002",primera.id(),"ACEPTAR").estado());
  var tarjeta=pasarela.agregar("95000001",new PasarelaLocalService.NuevaTarjeta(
   provisionarTarjeta(),"Adulto Demo","DEBITO",(short)12,(short)2090,"123"));
  UUID clave=UUID.randomUUID();
  var pago=pasarela.pagar("95000001",primera.id(),
   new PasarelaLocalService.PagoRequest(tarjeta.id(),clave,"123"));
  assertEquals("APROBADO",pago.estado());
  assertEquals("CONFIRMADA",reservas.obtener("95000001",primera.id()).estado());
  assertEquals(pago.transaccionId(),pasarela.pagar("95000001",primera.id(),
   new PasarelaLocalService.PagoRequest(tarjeta.id(),clave,"123")).transaccionId());

  var segunda=reserva("95000001","95000002",servicio,base.plusDays(1));
  reservas.accion("95000002",segunda.id(),"ACEPTAR");
  var rechazado=pasarela.pagar("95000001",segunda.id(),
   new PasarelaLocalService.PagoRequest(tarjeta.id(),UUID.randomUUID(),"123"));
  assertEquals("RECHAZADO",rechazado.estado());
  assertEquals("FONDOS_INSUFICIENTES",rechazado.codigo());
  assertEquals("PENDIENTE_PAGO",reservas.obtener("95000001",segunda.id()).estado());
 }

 private ReservaLocalService.ReservaVista reserva(String adulto,String acomp,Long servicio,LocalDate dia){
  ZoneId lima=ZoneId.of("America/Lima");
  OffsetDateTime inicio=dia.atTime(9,0).atZone(lima).toOffsetDateTime();
  OffsetDateTime fin=dia.atTime(17,0).atZone(lima).toOffsetDateTime();
  return reservas.crear(adulto,new ReservaLocalService.NuevaReserva(adulto,acomp,servicio,
   inicio,fin,"Lima, Perú","Prueba sin fondos reales"));
 }
}
