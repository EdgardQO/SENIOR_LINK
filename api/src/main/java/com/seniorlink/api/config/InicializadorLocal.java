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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component
@Profile("local")
public class InicializadorLocal implements ApplicationRunner {
 private final EntityManager em;
 private final ConfiguracionReservasRepository configuracion;
 private final PasswordEncoder passwordEncoder;
 @Value("${LOCAL_ADMIN_DNI:}") private String adminDni;
 @Value("${LOCAL_ADMIN_EMAIL:}") private String adminEmail;
 @Value("${LOCAL_ADMIN_PASSWORD:}") private String adminPassword;
 @Value("${LOCAL_SOPORTE_DNI:}") private String soporteDni;
 @Value("${LOCAL_SOPORTE_EMAIL:}") private String soporteEmail;
 @Value("${LOCAL_SOPORTE_PASSWORD:}") private String soportePassword;
 public InicializadorLocal(EntityManager em, ConfiguracionReservasRepository configuracion,PasswordEncoder passwordEncoder) {
  this.em=em; this.configuracion=configuracion;this.passwordEncoder=passwordEncoder;
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
  String[] parentescos={"HIJO/A","NIETO/A","HERMANO/A","SOBRINO/A","CONYUGE","PADRE/MADRE","TIO/A","PRIMO/A","YERNO/NUERA","CUIDADOR/A","AMIGO/A","TUTOR/A","OTRO"};
  for(short i=1;i<=parentescos.length;i++){
   if(em.find(Parentesco.class,i)==null){
    Parentesco p=new Parentesco();p.setId(i);p.setNombre(parentescos[i-1]);em.persist(p);
   }
  }
  if(em.createQuery("select count(s) from Servicio s",Long.class).getSingleResult()==0){
   for(String nombre:new String[]{"Acompañamiento en casa","Paseos y actividades","Acompañamiento a citas"}){
    Servicio s=new Servicio();s.setNombre(nombre);s.setDescripcion("Servicio de acompañamiento seguro");em.persist(s);
   }
  }
  if(em.find(InstitucionAliada.class,"20000000001")==null){
   InstitucionAliada i=new InstitucionAliada();i.setRuc("20000000001");i.setNombre("Institución Demo SeniorLink");i.setDetallesConvenio("Entidad ficticia local");em.persist(i);
  }
  crearInterno(adminDni,adminEmail,adminPassword,(short)1);
  crearInterno(soporteDni,soporteEmail,soportePassword,(short)4);
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
 private void crearInterno(String dni,String correo,String password,short rolId){
  if(dni.isBlank()&&correo.isBlank()&&password.isBlank())return;
  if(!dni.matches("[0-9]{8}")||correo.isBlank()||password.length()<12)
   throw new IllegalStateException("Credenciales locales internas incompletas: configura DNI, correo y contraseña de al menos 12 caracteres");
  if(em.find(Usuario.class,dni)!=null)return;
  Usuario usuario=new Usuario();usuario.setDni(dni);usuario.setNombres(rolId==1?"Admin":"Soporte");
  usuario.setApellidos("Local");usuario.setCorreoElectronico(correo.trim().toLowerCase());
  usuario.setContrasenaHash(passwordEncoder.encode(password));usuario.setRol(em.getReference(Rol.class,rolId));
  em.persist(usuario);em.flush();
  if(rolId==1)em.persist(new PerfilAdmin(usuario,"Administración local",(short)1));
  else em.persist(new PerfilSoporte(usuario,"Flexible",null));
 }
}
