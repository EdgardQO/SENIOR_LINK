package com.seniorlink.api.config;
import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.repository.ConfiguracionReservasRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Component
@Profile("local")
public class InicializadorLocal implements ApplicationRunner {
 private final EntityManager em;
 private final ConfiguracionReservasRepository configuracion;
 public InicializadorLocal(EntityManager em, ConfiguracionReservasRepository configuracion) {
  this.em=em; this.configuracion=configuracion;
 }
 @Override @Transactional
 public void run(ApplicationArguments args) {
  String[] roles={"ADMINISTRADOR","ACOMPANANTE","ADULTO_MAYOR","SOPORTE","FAMILIAR"};
  for(short i=1;i<=roles.length;i++) {
   Rol existente=em.find(Rol.class,i);
   if(existente==null) {
    Rol rol=new Rol(); rol.setId(i); rol.setNombre(roles[i-1]); em.persist(rol);
   } else if(!roles[i-1].equals(existente.getNombre())) {
    throw new IllegalStateException("Catalogo de roles incompatible: "+i);
   }
  }
  if(!configuracion.existsById((short)1)) {
   ConfiguracionReservas c=new ConfiguracionReservas();
   c.setId((short)1); c.setMinutosAceptacion(60); c.setMinutosPago(30);
   c.setMinutosAnticipacion(120); c.setMinutosDuracionMinima(60);
   c.setMinutosDuracionMaxima(480); c.setMinutosInicioAnticipado(15);
   c.setHorasConfirmacionAutomatica(48);
   c.setTarifaMinimaHora(new BigDecimal("10.00"));
   c.setTarifaMaximaHora(new BigDecimal("50.00"));
   c.setPorcentajeComision(new BigDecimal("10.00"));
   configuracion.save(c);
  }
 }
}
