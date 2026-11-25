package com.seniorlink.api.service;

import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.EstadoReserva;
import com.seniorlink.api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class ReservaLocalService {
 private static final ZoneId LIMA=ZoneId.of("America/Lima");
 private static final Set<EstadoReserva> OCUPADAS=EnumSet.of(
  EstadoReserva.PENDIENTE_ACEPTACION,EstadoReserva.PENDIENTE_PAGO,
  EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,
  EstadoReserva.PENDIENTE_CONFIRMACION,EstadoReserva.EN_DISPUTA);
 private final ReservaRepository reservas;
 private final UsuarioRepository usuarios;
 private final PerfilAcompananteRepository perfiles;
 private final ServicioRepository servicios;
 private final ConfiguracionReservasRepository configuraciones;
 private final VinculacionFamiliarAdultoRepository vinculos;
 private final ParentescoRepository parentescos;
 private final DisponibilidadAcompananteRepository horarios;
 private final BloqueoDisponibilidadRepository bloqueos;
 private final HistorialEstadoReservaRepository historial;
 private final ValoracionRepository valoraciones;
 private final ResenaRepository resenas;

 public ReservaLocalService(ReservaRepository reservas,UsuarioRepository usuarios,
  PerfilAcompananteRepository perfiles,ServicioRepository servicios,
  ConfiguracionReservasRepository configuraciones,VinculacionFamiliarAdultoRepository vinculos,
  ParentescoRepository parentescos,DisponibilidadAcompananteRepository horarios,BloqueoDisponibilidadRepository bloqueos,
  HistorialEstadoReservaRepository historial,ValoracionRepository valoraciones,ResenaRepository resenas){
  this.reservas=reservas;this.usuarios=usuarios;this.perfiles=perfiles;
  this.servicios=servicios;this.configuraciones=configuraciones;this.vinculos=vinculos;this.parentescos=parentescos;
  this.horarios=horarios;this.bloqueos=bloqueos;this.historial=historial;this.valoraciones=valoraciones;this.resenas=resenas;
 }
 public record NuevaReserva(String adultoMayorDni,String acompananteDni,Long servicioId,
  OffsetDateTime inicio,OffsetDateTime fin,String direccion,String indicaciones){}
 public record ReservaVista(Long id,String estado,String adultoMayorDni,String adultoMayorNombre,
  String solicitanteDni,String acompananteDni,String acompananteNombre,
  OffsetDateTime inicio,OffsetDateTime fin,OffsetDateTime venceAceptacionEn,
  OffsetDateTime vencePagoEn,BigDecimal montoTotal,BigDecimal tarifaHora,String direccion,
  String solicitanteNombre,String adultoMayorFotoUrl,String acompananteFotoUrl,String solicitanteFotoUrl){}
 public record AcompananteVista(String dni,String nombre,BigDecimal tarifaHora,String carrera,
  Integer ciclo,String biografia,String institucion,BigDecimal promedioCalificacion,
  Integer totalCalificaciones,String fotoUrl,List<OpinionVista> resenas){}
 public record OpinionVista(short puntuacion,String comentario,OffsetDateTime creadoEn){}
 public record Horario(short diaSemana,LocalTime horaInicio,LocalTime horaFin){}
 public record VinculoVista(Long id,String familiarDni,String familiarNombre,
  String adultoMayorDni,String adultoMayorNombre,String estado,
  boolean permiteReservar,boolean permitePagar,boolean permiteGestionar){}

 private ResponseStatusException bad(String msg){return new ResponseStatusException(HttpStatus.BAD_REQUEST,msg);}
 private ResponseStatusException no(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"Sin autorización");}
 private Usuario actor(String dni){
  Usuario u=usuarios.findById(dni).orElseThrow(this::no);
  if(!Boolean.TRUE.equals(u.getEstaActivo()))throw no();
  return u;
 }
 private String rol(Usuario u){return u.getRol().getNombre();}
 private ConfiguracionReservas cfg(){return configuraciones.findById((short)1)
  .orElseThrow(()->bad("Configuración de reservas no disponible"));}
 private Reserva buscar(Long id){return reservas.findById(id)
  .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));}
 private Reserva bloquear(Long id){return reservas.bloquear(id)
  .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));}
 private boolean permisoFamiliar(String familiar,String adulto,String accion){
  return vinculos.findByFamiliarDni(familiar).stream().anyMatch(v->
   v.getAdultoMayor().getDni().equals(adulto) && "ACEPTADO".equals(v.getEstado()) &&
   (accion.equals("RESERVAR")?v.isPermiteReservar():
    accion.equals("PAGAR")?v.isPermitePagar():v.isPermiteGestionar()));
 }
 public boolean puedePagar(String dni,Reserva r){
  Usuario u=actor(dni);
  String adulto=r.getAdultoMayor()!=null?r.getAdultoMayor().getDni():r.getCliente().getDni();
  return ("ADULTO_MAYOR".equals(rol(u)) && dni.equals(adulto)) ||
   ("FAMILIAR".equals(rol(u)) && permisoFamiliar(dni,adulto,"PAGAR"));
 }
 private boolean puedeVer(String dni,Reserva r){
  Usuario u=actor(dni);
  if("ADMINISTRADOR".equals(rol(u)))return true;
  if(dni.equals(r.getAcompanante().getDni()))return true;
  if(r.getSolicitante()!=null && dni.equals(r.getSolicitante().getDni()))return true;
  String adulto=r.getAdultoMayor()!=null?r.getAdultoMayor().getDni():r.getCliente().getDni();
  return dni.equals(adulto) || ("FAMILIAR".equals(rol(u)) && permisoFamiliar(dni,adulto,"GESTIONAR"));
 }
 public ReservaVista vista(Reserva r){
  Usuario a=r.getAdultoMayor()!=null?r.getAdultoMayor():r.getCliente();
  Usuario acomp=r.getAcompanante();
  Usuario solicitante=r.getSolicitante()==null?r.getCliente():r.getSolicitante();
  return new ReservaVista(r.getId(),r.getEstadoActual().name(),a.getDni(),
   a.getNombres()+" "+a.getApellidos(),
   r.getSolicitante()==null?r.getCliente().getDni():r.getSolicitante().getDni(),
   acomp.getDni(),acomp.getNombres()+" "+acomp.getApellidos(),
   r.getFechaHoraInicio(),r.getFechaHoraFin(),r.getVenceAceptacionEn(),
   r.getVencePagoEn(),r.getMontoTotal(),r.getTarifaPorHora(),r.getDireccionEncuentro(),
   solicitante.getNombres()+" "+solicitante.getApellidos(),fotoUrl(r,a),fotoUrl(r,acomp),fotoUrl(r,solicitante));
 }
 private String fotoUrl(Reserva r,Usuario u){
  return u.getFotoPerfilRuta()==null?null:"/api/v1/reservas/"+r.getId()+"/participantes/"+u.getDni()+"/foto";
 }
 @Transactional(readOnly=true)
 public void autorizarFotoParticipante(String dni,Long id,String participante){
  Reserva r=buscar(id);
  if(!puedeVer(dni,r))throw no();
  Usuario adulto=r.getAdultoMayor()!=null?r.getAdultoMayor():r.getCliente();
  Usuario solicitante=r.getSolicitante()!=null?r.getSolicitante():r.getCliente();
  Usuario acompanante=r.getAcompanante();
  if(participante.equals(adulto.getDni())||participante.equals(acompanante.getDni()))return;
  if(participante.equals(solicitante.getDni()) &&
   (dni.equals(solicitante.getDni())||dni.equals(adulto.getDni())||dni.equals(acompanante.getDni())||
    "ADMINISTRADOR".equals(rol(actor(dni)))))return;
  throw no();
 }
 public void cambiar(Reserva r,EstadoReserva e,String motivo,Usuario usuario){
  r.setEstadoActual(e);r.setActualizadoEn(OffsetDateTime.now());reservas.save(r);
  HistorialEstadoReserva h=new HistorialEstadoReserva();
  h.setReserva(r);h.setEstado(e);h.setMotivoODetalle(motivo);h.setCambiadoPor(usuario);historial.save(h);
 }
 private void expirar(Reserva r){
  OffsetDateTime ahora=OffsetDateTime.now();
  if((r.getEstadoActual()==EstadoReserva.PENDIENTE_ACEPTACION && r.getVenceAceptacionEn()!=null && !ahora.isBefore(r.getVenceAceptacionEn()))
   ||(r.getEstadoActual()==EstadoReserva.PENDIENTE_PAGO && r.getVencePagoEn()!=null && !ahora.isBefore(r.getVencePagoEn()))){
   cambiar(r,EstadoReserva.EXPIRADA,"Plazo de aceptación o pago vencido",null);
  }
 }
 public ReservaVista crear(String dni,NuevaReserva input){
  Usuario solicitante=actor(dni);
  if(input==null||input.inicio()==null||input.fin()==null||input.servicioId()==null
   ||input.adultoMayorDni()==null||input.acompananteDni()==null||input.direccion()==null
   ||input.direccion().isBlank()||input.direccion().length()>1000)throw bad("Datos de reserva incompletos");
  Usuario adulto=usuarios.findById(input.adultoMayorDni()).orElseThrow(()->bad("Adulto mayor inexistente"));
  if(!"ADULTO_MAYOR".equals(rol(adulto))||!Boolean.TRUE.equals(adulto.getEstaActivo()))throw bad("Adulto mayor inválido");
  if("ADULTO_MAYOR".equals(rol(solicitante))){
   if(!dni.equals(adulto.getDni()))throw no();
  }else if("FAMILIAR".equals(rol(solicitante))){
   if(!permisoFamiliar(dni,adulto.getDni(),"RESERVAR"))throw no();
  }else throw no();
  // Bloquear la cuenta del acompañante antes de comprobar cruces, serializando reservas simultáneas.
  Usuario acomp=usuarios.bloquear(input.acompananteDni()).orElseThrow(()->bad("Acompañante no encontrado"));
  if(!"ACOMPANANTE".equals(rol(acomp))||!Boolean.TRUE.equals(acomp.getEstaActivo())||!acomp.isFotoVerificada())throw bad("Acompañante no disponible");
  ConfiguracionReservas c=cfg();
  long minutos=Duration.between(input.inicio().toInstant(),input.fin().toInstant()).toMinutes();
  if(minutos<c.getMinutosDuracionMinima()||minutos>c.getMinutosDuracionMaxima()||
   Duration.between(input.inicio().toInstant(),input.fin().toInstant()).toSeconds()!=minutos*60)
   throw bad("Duración fuera de los límites configurados");
  if(Duration.between(Instant.now(),input.inicio().toInstant()).toMinutes()<c.getMinutosAnticipacion())
   throw bad("La reserva requiere al menos 2 horas de anticipación");
  ZonedDateTime i=input.inicio().atZoneSameInstant(LIMA),f=input.fin().atZoneSameInstant(LIMA);
  if(!i.toLocalDate().equals(f.toLocalDate()))throw bad("El horario debe estar dentro de un mismo día");
  int dia=i.getDayOfWeek().getValue();
  boolean disponible=horarios.findByAcompananteDniAndEstaActivoTrue(acomp.getDni()).stream()
   .anyMatch(h->h.getDiaSemana()==dia && !i.toLocalTime().isBefore(h.getHoraInicio())
    && !f.toLocalTime().isAfter(h.getHoraFin()));
  if(!disponible || bloqueos.existsByAcompananteDniAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(
   acomp.getDni(),input.fin(),input.inicio()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Horario no disponible");
  for(Reserva previa:reservas.findByAcompananteDni(acomp.getDni()))expirar(previa);
  if(reservas.hayCruce(acomp.getDni(),input.inicio(),input.fin(),OCUPADAS))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe una reserva en ese horario");
  PerfilAcompanante perfil=perfiles.findById(acomp.getDni()).orElseThrow(()->bad("Perfil incompleto"));
  BigDecimal tarifa=perfil.getTarifaActualPorHora();
  if(tarifa==null||tarifa.compareTo(c.getTarifaMinimaHora())<0||tarifa.compareTo(c.getTarifaMaximaHora())>0)
   throw bad("Tarifa fuera de los límites permitidos");
  Servicio servicio=servicios.findById(input.servicioId()).orElseThrow(()->bad("Servicio inexistente"));
  if(!Boolean.TRUE.equals(servicio.getEstaActivo()))throw bad("Servicio inactivo");
  BigDecimal horas=BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP);
  BigDecimal subtotal=tarifa.multiply(horas).setScale(2,RoundingMode.HALF_UP);
  BigDecimal comision=subtotal.multiply(c.getPorcentajeComision()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
  Reserva r=new Reserva();
  r.setCliente(adulto);r.setAdultoMayor(adulto);r.setSolicitante(solicitante);
  r.setAcompanante(acomp);r.setFechaHoraInicio(input.inicio());r.setFechaHoraFin(input.fin());
  r.setTarifaPorHora(tarifa);r.setTotalHoras(horas);r.setSubtotal(subtotal);
  r.setMontoDescuento(BigDecimal.ZERO);r.setComisionPlataforma(comision);
  r.setMontoTotal(subtotal.add(comision));r.setDireccionEncuentro(input.direccion().trim());
  r.setIndicacionesAdicionales(input.indicaciones());r.setVenceAceptacionEn(OffsetDateTime.now().plusMinutes(c.getMinutosAceptacion()));
  r.setEstadoActual(EstadoReserva.PENDIENTE_ACEPTACION);
  r.agregarServicio(servicio,horas);
  reservas.saveAndFlush(r);cambiar(r,EstadoReserva.PENDIENTE_ACEPTACION,"Solicitud creada",solicitante);
  return vista(r);
 }
 public List<ReservaVista> listar(String dni){
  Usuario u=actor(dni);List<Reserva> lista;
  if("ADMINISTRADOR".equals(rol(u)))lista=reservas.findAll();
  else if("ACOMPANANTE".equals(rol(u)))lista=reservas.findByAcompananteDni(dni);
  else if("FAMILIAR".equals(rol(u)))lista=reservas.findAll();
  else lista=reservas.visiblesCliente(dni);
  return lista.stream().filter(r->puedeVer(dni,r)).peek(this::expirar)
   .sorted(Comparator.comparing(Reserva::getId).reversed()).map(this::vista).toList();
 }
 public ReservaVista obtener(String dni,Long id){
  Reserva r=buscar(id);if(!puedeVer(dni,r))throw no();expirar(r);return vista(r);
 }
 public ReservaVista accion(String dni,Long id,String accion){
  Usuario u=actor(dni),rActor=u;
  Reserva r=bloquear(id);expirar(r);
  OffsetDateTime ahora=OffsetDateTime.now();
  if(accion.equals("ACEPTAR")||accion.equals("RECHAZAR")){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(r.getEstadoActual()!=EstadoReserva.PENDIENTE_ACEPTACION)throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud no está pendiente");
   if(accion.equals("RECHAZAR"))cambiar(r,EstadoReserva.RECHAZADA,"Rechazada por el acompañante",rActor);
   else{r.setAceptadoEn(ahora);r.setVencePagoEn(ahora.plusMinutes(cfg().getMinutosPago()));
    cambiar(r,EstadoReserva.PENDIENTE_PAGO,"Aceptada, pendiente de pago",rActor);}
  }else if(accion.equals("INICIAR")){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(r.getEstadoActual()!=EstadoReserva.CONFIRMADA)throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva no está confirmada");
   if(ahora.isBefore(r.getFechaHoraInicio().minusMinutes(cfg().getMinutosInicioAnticipado())))
    throw bad("Aún no se permite iniciar el servicio");
   r.setInicioRealEn(ahora);cambiar(r,EstadoReserva.EN_PROGRESO,"Servicio iniciado",rActor);
  }else if(accion.equals("FINALIZAR")){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(r.getEstadoActual()!=EstadoReserva.EN_PROGRESO)throw new ResponseStatusException(HttpStatus.CONFLICT,"El servicio no está en progreso");
   r.setFinRealEn(ahora);cambiar(r,EstadoReserva.PENDIENTE_CONFIRMACION,"A la espera de confirmación del cliente",rActor);
  }else if(accion.equals("CONFIRMAR")){
   if(!puedePagar(dni,r) && !(dni.equals(r.getSolicitante()==null?"":r.getSolicitante().getDni()) &&
     "FAMILIAR".equals(rol(u)) && permisoFamiliar(dni,r.getCliente().getDni(),"GESTIONAR")))throw no();
   if(r.getEstadoActual()!=EstadoReserva.PENDIENTE_CONFIRMACION)throw new ResponseStatusException(HttpStatus.CONFLICT,"El servicio no espera confirmación");
   r.setConfirmadoEn(ahora);cambiar(r,EstadoReserva.COMPLETADA,"Servicio confirmado",rActor);
  }else if(accion.equals("CANCELAR")){
   String adulto=r.getAdultoMayor()!=null?r.getAdultoMayor().getDni():r.getCliente().getDni();
   boolean esAcomp=dni.equals(r.getAcompanante().getDni());
   boolean puede=dni.equals(adulto)||esAcomp||
    ("FAMILIAR".equals(rol(u))&&permisoFamiliar(dni,adulto,"GESTIONAR"));
   if(!puede)throw no();
   if(EnumSet.of(EstadoReserva.COMPLETADA,EstadoReserva.CANCELADA,
     EstadoReserva.RECHAZADA,EstadoReserva.EXPIRADA).contains(r.getEstadoActual()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva ya terminó");
   boolean cobrada=EnumSet.of(EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,
     EstadoReserva.PENDIENTE_CONFIRMACION,EstadoReserva.EN_DISPUTA).contains(r.getEstadoActual());
   cambiar(r,cobrada?EstadoReserva.EN_DISPUTA:EstadoReserva.CANCELADA,
    cobrada?"Cancelación con pago: requiere evaluación de soporte":"Cancelación sin cobro",rActor);
  }else throw bad("Acción desconocida");
  return vista(r);
 }
 public void procesarPlazos(){
  OffsetDateTime ahora=OffsetDateTime.now();
  for(Reserva item:reservas.findAll()){
   if(item.getEstadoActual()==EstadoReserva.PENDIENTE_ACEPTACION ||
      item.getEstadoActual()==EstadoReserva.PENDIENTE_PAGO ||
      item.getEstadoActual()==EstadoReserva.PENDIENTE_CONFIRMACION){
    Reserva r=bloquear(item.getId());
    expirar(r);
    if(r.getEstadoActual()==EstadoReserva.PENDIENTE_CONFIRMACION &&
       r.getFinRealEn()!=null &&
       !ahora.isBefore(r.getFinRealEn().plusHours(cfg().getHorasConfirmacionAutomatica()))){
     r.setConfirmadoEn(ahora);
     cambiar(r,EstadoReserva.COMPLETADA,"Confirmación automática tras 48 horas sin disputa",null);
    }
   }
  }
 }
 @Transactional(readOnly=true)
 public List<OpinionVista> opiniones(String dni){
  Map<Long,OpinionVista> porReserva=new LinkedHashMap<>();
  for(Valoracion v:valoraciones.resenasDeReservas(dni,EstadoReserva.COMPLETADA))
   porReserva.put(v.getReserva().getId(),new OpinionVista(v.getPuntuacion(),v.getComentario(),v.getCreadoEn()));
  for(Resena r:resenas.resenasDeReservas(dni,EstadoReserva.COMPLETADA))
   porReserva.putIfAbsent(r.getReserva().getId(),new OpinionVista(r.getCalificacion(),r.getComentario(),r.getCreadoEn()));
  return porReserva.values().stream().sorted(Comparator.comparing(OpinionVista::creadoEn).reversed()).toList();
 }
 @Transactional(readOnly=true)
 public List<AcompananteVista> acompanantes(){
  return perfiles.findAll().stream().filter(p->Boolean.TRUE.equals(p.getUsuario().getEstaActivo()) && p.getUsuario().isFotoVerificada() && "ACOMPANANTE".equals(rol(p.getUsuario())))
   .map(p->{
    String dni=p.getUsuario().getDni();
    List<OpinionVista> opiniones=opiniones(dni);
    BigDecimal promedio=opiniones.isEmpty()?null:BigDecimal.valueOf(opiniones.stream().mapToInt(OpinionVista::puntuacion).sum()).divide(BigDecimal.valueOf(opiniones.size()),2,RoundingMode.HALF_UP);
    return new AcompananteVista(dni,p.getUsuario().getNombres()+" "+p.getUsuario().getApellidos(),
     p.getTarifaActualPorHora(),p.getCarrera(),p.getCiclo(),p.getBiografia(),
     p.getInstitucionAliada()==null?null:p.getInstitucionAliada().getNombre(),
     promedio,opiniones.size(),"/api/v1/usuarios/"+dni+"/foto",opiniones);
   }).toList();
 }
 public List<Servicio> servicios(){return servicios.findAll().stream().filter(s->Boolean.TRUE.equals(s.getEstaActivo())).toList();}
 public List<Horario> disponibilidad(String dni){return horarios.findByAcompananteDniAndEstaActivoTrue(dni).stream()
  .map(h->new Horario(h.getDiaSemana(),h.getHoraInicio(),h.getHoraFin())).toList();}
 public List<Horario> guardarDisponibilidad(String dni,List<Horario> entrada){
  if(!"ACOMPANANTE".equals(rol(actor(dni))))throw no();
  if(entrada==null||entrada.size()>21)throw bad("Horarios inválidos");
  for(Horario h:entrada)if(h==null||h.diaSemana()<1||h.diaSemana()>7||h.horaInicio()==null
   ||h.horaFin()==null||!h.horaFin().isAfter(h.horaInicio()))throw bad("Horario inválido");
  horarios.deleteByAcompananteDni(dni);horarios.flush();
  Usuario u=usuarios.getReferenceById(dni);
  for(Horario h:entrada){DisponibilidadAcompanante d=new DisponibilidadAcompanante();
   d.setAcompanante(u);d.setDiaSemana(h.diaSemana());d.setHoraInicio(h.horaInicio());
   d.setHoraFin(h.horaFin());horarios.save(d);}
  return disponibilidad(dni);
 }
 public BigDecimal tarifa(String dni,BigDecimal valor){
  if(!"ACOMPANANTE".equals(rol(actor(dni))))throw no();
  ConfiguracionReservas c=cfg();
  if(valor==null||valor.scale()>2||valor.compareTo(c.getTarifaMinimaHora())<0||
   valor.compareTo(c.getTarifaMaximaHora())>0)throw bad("Tarifa fuera de los límites");
  PerfilAcompanante p=perfiles.findById(dni).orElseThrow(()->bad("Perfil no encontrado"));
  p.setTarifaActualPorHora(valor);perfiles.save(p);return valor;
 }
 public List<VinculoVista> vinculos(String dni){
  Usuario u=actor(dni);List<VinculacionFamiliarAdulto> lista;
  if("ADULTO_MAYOR".equals(rol(u)))lista=vinculos.findByAdultoMayorDni(dni);
  else if("FAMILIAR".equals(rol(u)))lista=vinculos.findByFamiliarDni(dni);
  else throw no();
  return lista.stream().map(this::vistaVinculo).toList();
 }
 private VinculoVista vistaVinculo(VinculacionFamiliarAdulto v){
  return new VinculoVista(v.getId(),v.getFamiliar().getDni(),v.getFamiliar().getNombres(),
   v.getAdultoMayor().getDni(),v.getAdultoMayor().getNombres(),v.getEstado(),
   v.isPermiteReservar(),v.isPermitePagar(),v.isPermiteGestionar());
 }
 public VinculoVista solicitarVinculo(String dni,String otroDni,Short parentescoId){
  Usuario actor=actor(dni);
  Usuario otro=usuarios.findById(otroDni).orElseThrow(()->bad("Usuario no encontrado"));
  boolean familiar="FAMILIAR".equals(rol(actor));
  if(!familiar && !"ADULTO_MAYOR".equals(rol(actor)))throw no();
  Usuario f=familiar?actor:otro,a=familiar?otro:actor;
  if(!"FAMILIAR".equals(rol(f))||!"ADULTO_MAYOR".equals(rol(a)))throw bad("Roles incompatibles");
  if(vinculos.existsByFamiliarDniAndAdultoMayorDni(f.getDni(),a.getDni()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe una solicitud");
  VinculacionFamiliarAdulto v=new VinculacionFamiliarAdulto();
  v.setFamiliar(f);v.setAdultoMayor(a);
  // Parentesco definido en el catálogo; el adulto autoriza permisos después.
  v.setParentesco(parentescos.findById(parentescoId==null?(short)13:parentescoId)
   .orElseThrow(()->bad("Parentesco inexistente")));v.setEstado("PENDIENTE");vinculos.save(v);
  return vistaVinculo(v);
 }
 public VinculoVista responderVinculo(String dni,Long id,boolean aprobar){
  Usuario u=actor(dni);
  VinculacionFamiliarAdulto v=vinculos.findByIdAndAdultoMayorDni(id,dni).orElseThrow(this::no);
  if(!"ADULTO_MAYOR".equals(rol(u)))throw no();
  if(!"PENDIENTE".equals(v.getEstado()))throw bad("Solicitud ya respondida");
  v.setEstado(aprobar?"ACEPTADO":"RECHAZADO");v.setPermiteReservar(aprobar);
  v.setPermitePagar(aprobar);v.setPermiteGestionar(aprobar);
  v.setRespondidoEn(OffsetDateTime.now());vinculos.save(v);return vistaVinculo(v);
 }
}
