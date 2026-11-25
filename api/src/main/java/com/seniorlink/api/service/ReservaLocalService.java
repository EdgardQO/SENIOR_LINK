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
 private static final Set<EstadoReserva> FIRMES=EnumSet.of(EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO);
 public record IntervaloOcupado(OffsetDateTime inicio,OffsetDateTime fin){}
 private List<IntervaloOcupado> intervalos(Reserva r,boolean soloActivas){
  if(r.getJornadas().isEmpty())return List.of(new IntervaloOcupado(r.getFechaHoraInicio(),r.getFechaHoraFin()));
  return r.getJornadas().stream().filter(j->!soloActivas||Set.of("PROGRAMADA","EN_PROGRESO").contains(j.getEstado()))
   .filter(j->!Set.of("CANCELADA","EXPIRADA").contains(j.getEstado()))
   .map(j->new IntervaloOcupado(j.getFechaHoraInicio(),j.getFechaHoraFin())).toList();
 }
 private boolean cruza(IntervaloOcupado j,OffsetDateTime i,OffsetDateTime f){return j.inicio().isBefore(f)&&j.fin().isAfter(i);}
 private List<Reserva> ocupacion(String dni,OffsetDateTime inicio,OffsetDateTime fin,Long excluir){
  return reservas.ocupacion(dni,inicio,fin,excluir==null?-1L:excluir,OffsetDateTime.now(),FIRMES,
   EstadoReserva.PENDIENTE_PAGO,EstadoReserva.EN_DISPUTA,EstadoReserva.COMPLETADA,EstadoReserva.CANCELADA).stream().filter(r->intervalos(r,true).stream().anyMatch(j->cruza(j,inicio,fin))).toList();
 }
 private OffsetDateTime antesDe(OffsetDateTime limite,OffsetDateTime inicio){
  return limite.isBefore(inicio)?limite:inicio;
 }
 public Usuario bloquearAcompanante(String dni){
  return usuarios.bloquear(dni).orElseThrow(()->bad("Acompañante no encontrado"));
 }
 public void bloquearAdulto(String dni){
  usuarios.bloquear(dni).orElseThrow(()->bad("Adulto mayor no encontrado"));
 }
 public void validarLimiteAdulto(String dni,OffsetDateTime inicio,OffsetDateTime fin,Long excluir){
  if(inicio==null||fin==null||!fin.isAfter(inicio))throw bad("Horario inválido");
  ZonedDateTime i=inicio.atZoneSameInstant(LIMA),f=fin.atZoneSameInstant(LIMA);
  if(!i.toLocalDate().equals(f.toLocalDate()))throw bad("La jornada debe estar dentro de un mismo día");
  Duration duracion=Duration.between(inicio,fin);
  if(duracion.compareTo(Duration.ofHours(8))>0)throw bad("El máximo diario es de ocho horas");
  OffsetDateTime desde=i.toLocalDate().atStartOfDay(LIMA).toOffsetDateTime();
  OffsetDateTime hasta=i.toLocalDate().plusDays(1).atStartOfDay(LIMA).toOffsetDateTime();
  OffsetDateTime ahora=OffsetDateTime.now();
  Set<EstadoReserva> estados=EnumSet.of(EstadoReserva.PENDIENTE_ACEPTACION,EstadoReserva.PENDIENTE_PAGO,
   EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,EstadoReserva.PENDIENTE_CONFIRMACION,EstadoReserva.COMPLETADA,EstadoReserva.EN_DISPUTA);
  for(Reserva r:reservas.reservasAdultoEnDia(dni,desde,hasta,excluir==null?-1L:excluir,estados)){
   if(plazoVencido(r,ahora))continue;
   for(IntervaloOcupado j:intervalos(r,false)){
   if(!cruza(j,desde,hasta))continue;
   OffsetDateTime ri=j.inicio(),rf=j.fin();
   if(ri.isBefore(fin)&&rf.isAfter(inicio))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"El adulto mayor ya tiene otra reserva en ese horario");
   duracion=duracion.plus(Duration.between(ri.isBefore(desde)?desde:ri,rf.isAfter(hasta)?hasta:rf));
   }
  }
  if(duracion.compareTo(Duration.ofHours(8))>0)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"El adulto mayor supera las ocho horas acumuladas de acompañamiento del día "+i.toLocalDate());
 }
 public Reserva bloquearParaAgenda(Long id){
  String dni=reservas.dniAcompanante(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Reserva no encontrada"));
  bloquearAcompanante(dni);
  return bloquear(id);
 }
 public void validarHorario(String dni,OffsetDateTime inicio,OffsetDateTime fin,Long excluir){
  if(inicio==null||fin==null||!fin.isAfter(inicio)||!fin.isAfter(OffsetDateTime.now()))throw bad("Horario inválido o vencido");
  ZonedDateTime i=inicio.atZoneSameInstant(LIMA),f=fin.atZoneSameInstant(LIMA);
  if(!i.toLocalDate().equals(f.toLocalDate()))throw bad("El horario debe estar dentro de un mismo día");
  boolean disponible=horarios.findByAcompananteDniAndEstaActivoTrue(dni).stream()
   .anyMatch(h->h.getDiaSemana()==i.getDayOfWeek().getValue()&&!i.toLocalTime().isBefore(h.getHoraInicio())&&!f.toLocalTime().isAfter(h.getHoraFin()));
  if(!disponible||bloqueos.existsByAcompananteDniAndFechaHoraInicioLessThanAndFechaHoraFinGreaterThan(dni,fin,inicio))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Horario fuera de la disponibilidad del acompañante");
  if(!ocupacion(dni,inicio,fin,excluir).isEmpty())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"El acompañante ya tiene una reserva aceptada en ese horario");
 }
 @Transactional(readOnly=true)
 public List<IntervaloOcupado> horariosOcupados(String dni,OffsetDateTime inicio,OffsetDateTime fin){
  if(inicio==null||fin==null||!fin.isAfter(inicio)||Duration.between(inicio,fin).compareTo(Duration.ofDays(31))>0)throw bad("Indica un intervalo de hasta 31 días");
  Usuario u=usuarios.findById(dni).orElseThrow(()->bad("Acompañante no encontrado"));
  if(!"ACOMPANANTE".equals(rol(u))||!Boolean.TRUE.equals(u.getEstaActivo())||!u.isFotoVerificada())throw no();
  List<IntervaloOcupado> resultado=new ArrayList<>();
  ocupacion(dni,inicio,fin,null).forEach(r->intervalos(r,true).stream().filter(j->cruza(j,inicio,fin)).forEach(resultado::add));
  bloqueos.findByAcompananteDni(dni).stream().filter(b->b.getFechaHoraInicio().isBefore(fin)&&b.getFechaHoraFin().isAfter(inicio))
   .forEach(b->resultado.add(new IntervaloOcupado(b.getFechaHoraInicio(),b.getFechaHoraFin())));
  return resultado.stream().sorted(Comparator.comparing(IntervaloOcupado::inicio)).toList();
 }
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
 public record JornadaInput(OffsetDateTime inicio,OffsetDateTime fin){}
 public record JornadaVista(Long id,OffsetDateTime inicio,OffsetDateTime fin,String estado){}
 public record NuevaReserva(String adultoMayorDni,String acompananteDni,Long servicioId,
  OffsetDateTime inicio,OffsetDateTime fin,String direccion,String indicaciones,List<JornadaInput> jornadas){
  public NuevaReserva(String a,String b,Long c,OffsetDateTime i,OffsetDateTime f,String d,String e){this(a,b,c,i,f,d,e,null);}
 }
 public record ReservaVista(Long id,String estado,String adultoMayorDni,String adultoMayorNombre,
  String solicitanteDni,String acompananteDni,String acompananteNombre,
  OffsetDateTime inicio,OffsetDateTime fin,OffsetDateTime venceAceptacionEn,
  OffsetDateTime vencePagoEn,BigDecimal montoTotal,BigDecimal tarifaHora,String direccion,
  String solicitanteNombre,String adultoMayorFotoUrl,String acompananteFotoUrl,String solicitanteFotoUrl,OpinionVista valoracion,List<JornadaVista> jornadas,BigDecimal totalHoras,BigDecimal subtotal,BigDecimal comision){}
 public record AcompananteVista(String dni,String nombre,BigDecimal tarifaHora,String carrera,
  Integer ciclo,String biografia,String institucion,BigDecimal promedioCalificacion,
  Integer totalCalificaciones,String fotoUrl,List<OpinionVista> resenas){}
 public record OpinionVista(short puntuacion,String comentario,OffsetDateTime creadoEn){}
 public record Horario(short diaSemana,LocalTime horaInicio,LocalTime horaFin){}
 public record VinculoVista(Long id,String familiarDni,String familiarNombre,
  String adultoMayorDni,String adultoMayorNombre,String estado,
  boolean permiteReservar,boolean permitePagar,boolean permiteGestionar,String parentescoNombre){}

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
  return new ReservaVista(r.getId(),(plazoVencido(r,OffsetDateTime.now())?EstadoReserva.EXPIRADA:r.getEstadoActual()).name(),a.getDni(),
   a.getNombres()+" "+a.getApellidos(),
   r.getSolicitante()==null?r.getCliente().getDni():r.getSolicitante().getDni(),
   acomp.getDni(),acomp.getNombres()+" "+acomp.getApellidos(),
   r.getFechaHoraInicio(),r.getFechaHoraFin(),r.getVenceAceptacionEn(),
   r.getVencePagoEn(),r.getMontoTotal(),r.getTarifaPorHora(),r.getDireccionEncuentro(),
   solicitante.getNombres()+" "+solicitante.getApellidos(),fotoUrl(r,a),fotoUrl(r,acomp),fotoUrl(r,solicitante),opinionReserva(r.getId()),r.getJornadas().stream().map(j->new JornadaVista(j.getId(),j.getFechaHoraInicio(),j.getFechaHoraFin(),j.getEstado())).toList(),r.getTotalHoras(),r.getSubtotal(),r.getComisionPlataforma());
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
  if(Set.of(EstadoReserva.CANCELADA,EstadoReserva.EXPIRADA,EstadoReserva.RECHAZADA).contains(e))
   r.getJornadas().stream().filter(j->"PROGRAMADA".equals(j.getEstado())||"EN_PROGRESO".equals(j.getEstado())).forEach(j->j.setEstado(e==EstadoReserva.EXPIRADA?"EXPIRADA":"CANCELADA"));
  r.setEstadoActual(e);r.setActualizadoEn(OffsetDateTime.now());reservas.save(r);
  HistorialEstadoReserva h=new HistorialEstadoReserva();
  h.setReserva(r);h.setEstado(e);h.setMotivoODetalle(motivo);h.setCambiadoPor(usuario);historial.save(h);
 }
 private boolean plazoVencido(Reserva r,OffsetDateTime ahora){
  OffsetDateTime limite=switch(r.getEstadoActual()){
   case PENDIENTE_ACEPTACION -> r.getVenceAceptacionEn();
   case PENDIENTE_PAGO -> r.getVencePagoEn();
   default -> null;
  };
  if(r.getEstadoActual()!=EstadoReserva.PENDIENTE_ACEPTACION&&r.getEstadoActual()!=EstadoReserva.PENDIENTE_PAGO)return false;
  return !ahora.isBefore(r.getFechaHoraInicio())||(limite!=null&&!ahora.isBefore(limite));
 }
 private void expirar(Reserva r){
  if(plazoVencido(r,OffsetDateTime.now()))cambiar(r,EstadoReserva.EXPIRADA,"Plazo de aceptación o pago vencido",null);
 }
 public OffsetDateTime plazoPago(OffsetDateTime inicio){
  return antesDe(OffsetDateTime.now().plusMinutes(cfg().getMinutosPago()),inicio);
 }
 public ReservaVista crear(String dni,NuevaReserva input){
  Usuario solicitante=actor(dni);
  if(input==null||input.servicioId()==null
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
  List<JornadaInput> jornadas=input.jornadas()==null||input.jornadas().isEmpty()?new ArrayList<>(List.of(new JornadaInput(input.inicio(),input.fin()))):new ArrayList<>(input.jornadas());
  if(jornadas.size()>365||jornadas.stream().anyMatch(j->j==null||j.inicio()==null||j.fin()==null))throw bad("Jornadas inválidas (máximo 365)");
  jornadas.sort(Comparator.comparing(JornadaInput::inicio));
  bloquearAdulto(adulto.getDni());
  long minutos=0;
  Set<LocalDate> fechas=new HashSet<>();
  for(JornadaInput j:jornadas){
   long m=Duration.between(j.inicio(),j.fin()).toMinutes();
   if(m<c.getMinutosDuracionMinima()||m>Math.min(480,c.getMinutosDuracionMaxima())||!Duration.between(j.inicio(),j.fin()).equals(Duration.ofMinutes(m)))throw bad("Duración inválida por jornada");
   if(j.inicio().isBefore(OffsetDateTime.now().plusMinutes(60)))throw bad("La reserva requiere al menos 1 hora de anticipación");
   if(!fechas.add(j.inicio().atZoneSameInstant(LIMA).toLocalDate()))throw bad("No repitas fechas dentro de la misma contratación");
   validarHorario(acomp.getDni(),j.inicio(),j.fin(),null);
   validarLimiteAdulto(adulto.getDni(),j.inicio(),j.fin(),null);
   minutos+=m;
  }
  OffsetDateTime primero=jornadas.getFirst().inicio(),ultimo=jornadas.getLast().fin();
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
  r.setAcompanante(acomp);r.setFechaHoraInicio(primero);r.setFechaHoraFin(ultimo);
  if(jornadas.size()>1)jornadas.forEach(j->r.agregarJornada(j.inicio(),j.fin()));
  r.setTarifaPorHora(tarifa);r.setTotalHoras(horas);r.setSubtotal(subtotal);
  r.setMontoDescuento(BigDecimal.ZERO);r.setComisionPlataforma(comision);
  r.setMontoTotal(subtotal.add(comision));r.setDireccionEncuentro(input.direccion().trim());
  r.setIndicacionesAdicionales(input.indicaciones());r.setVenceAceptacionEn(antesDe(OffsetDateTime.now().plusMinutes(c.getMinutosAceptacion()),primero));
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
  return lista.stream().filter(r->puedeVer(dni,r))
   .sorted(Comparator.comparing(Reserva::getId).reversed()).map(this::vista).toList();
 }
 public ReservaVista obtener(String dni,Long id){
  Reserva r=buscar(id);if(!puedeVer(dni,r))throw no();return vista(r);
 }
 public ReservaVista accion(String dni,Long id,String accion){
  Usuario u=actor(dni),rActor=u;
  Reserva r=bloquearParaAgenda(id);expirar(r);
  OffsetDateTime ahora=OffsetDateTime.now();
  if(!r.getJornadas().isEmpty()&&Set.of("INICIAR","FINALIZAR","CONFIRMAR").contains(accion))throw bad("Selecciona una jornada individual");
  if(accion.equals("ACEPTAR")||accion.equals("RECHAZAR")){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(r.getEstadoActual()!=EstadoReserva.PENDIENTE_ACEPTACION)throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud no está pendiente");
   if(accion.equals("RECHAZAR"))cambiar(r,EstadoReserva.RECHAZADA,"Rechazada por el acompañante",rActor);
   else{
    if(!r.getFechaHoraInicio().isAfter(ahora))throw new ResponseStatusException(HttpStatus.CONFLICT,"El horario solicitado ya comenzó");
    String adulto=r.getAdultoMayor()!=null?r.getAdultoMayor().getDni():r.getCliente().getDni();
    bloquearAdulto(adulto);
    for(IntervaloOcupado j:intervalos(r,false)){
     validarHorario(dni,j.inicio(),j.fin(),r.getId());
     validarLimiteAdulto(adulto,j.inicio(),j.fin(),r.getId());
    }
    r.setAceptadoEn(ahora);r.setVencePagoEn(antesDe(ahora.plusMinutes(cfg().getMinutosPago()),r.getFechaHoraInicio()));
    cambiar(r,EstadoReserva.PENDIENTE_PAGO,"Aceptada, pendiente de pago",rActor);}
  }else if(accion.equals("INICIAR")){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(r.getEstadoActual()!=EstadoReserva.CONFIRMADA)throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva no está confirmada");
   if(ahora.isBefore(r.getFechaHoraInicio().minusMinutes(cfg().getMinutosInicioAnticipado())))
    throw bad("Aún no se permite iniciar el servicio");
   if(!ahora.isBefore(r.getFechaHoraFin()))throw bad("El horario del servicio ya terminó");
   if(reservas.existeOtroEnEstado(dni,r.getId(),EstadoReserva.EN_PROGRESO))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Finaliza el servicio que está en progreso antes de iniciar otro");
   if(ahora.isBefore(r.getFechaHoraInicio())&&!ocupacion(dni,ahora,r.getFechaHoraInicio(),r.getId()).isEmpty())
    throw new ResponseStatusException(HttpStatus.CONFLICT,"El inicio anticipado coincide con otro servicio");
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
   cambiar(r,EstadoReserva.CANCELADA,
    cobrada?"Cancelación con pago: requiere evaluación de soporte y reembolso":"Cancelación sin cobro",rActor);
  }else throw bad("Acción desconocida");
  return vista(r);
 }
 public ReservaVista accionJornada(String dni,Long id,Long jornadaId,String accion){
  Usuario u=actor(dni);Reserva r=bloquearParaAgenda(id);
  if(!Set.of(EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,EstadoReserva.PENDIENTE_CONFIRMACION).contains(r.getEstadoActual()))throw bad("La contratación no está activa");
  ReservaJornada j=r.getJornadas().stream().filter(x->x.getId().equals(jornadaId)).findFirst().orElseThrow(()->bad("Jornada inexistente"));
  OffsetDateTime ahora=OffsetDateTime.now();
  if("INICIAR".equals(accion)){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(!"PROGRAMADA".equals(j.getEstado())||ahora.isBefore(j.getFechaHoraInicio().minusMinutes(cfg().getMinutosInicioAnticipado()))||!ahora.isBefore(j.getFechaHoraFin()))throw bad("No se puede iniciar esta jornada en este momento");
   if(r.getJornadas().stream().anyMatch(x->"EN_PROGRESO".equals(x.getEstado()))||reservas.existeOtroEnEstado(dni,id,EstadoReserva.EN_PROGRESO))throw bad("Finaliza primero el servicio en progreso");
   if(ahora.isBefore(j.getFechaHoraInicio())&&!ocupacion(dni,ahora,j.getFechaHoraInicio(),id).isEmpty())throw bad("Inicio anticipado incompatible con otro servicio");
   j.iniciar(ahora);if(r.getInicioRealEn()==null)r.setInicioRealEn(ahora);
  }else if("FINALIZAR".equals(accion)){
   if(!dni.equals(r.getAcompanante().getDni()))throw no();
   if(!"EN_PROGRESO".equals(j.getEstado()))throw bad("La jornada no está en progreso");
   j.finalizar(ahora);
  }else if("CONFIRMAR".equals(accion)){
   if(!puedePagar(dni,r)&&!("FAMILIAR".equals(rol(u))&&permisoFamiliar(dni,r.getCliente().getDni(),"GESTIONAR")))throw no();
   if(!"PENDIENTE_CONFIRMACION".equals(j.getEstado()))throw bad("La jornada no espera confirmación");
   j.confirmar(ahora);
  }else throw bad("Acción desconocida");
  actualizarEstadoJornadas(r,ahora,u);
  return vista(r);
 }
 private void actualizarEstadoJornadas(Reserva r,OffsetDateTime ahora,Usuario u){
  List<ReservaJornada> js=r.getJornadas();
  EstadoReserva e;
  if(js.stream().allMatch(j->"COMPLETADA".equals(j.getEstado()))){
   e=EstadoReserva.COMPLETADA;r.setConfirmadoEn(ahora);
  }else if(js.stream().anyMatch(j->"EN_PROGRESO".equals(j.getEstado())))e=EstadoReserva.EN_PROGRESO;
  else if(js.stream().anyMatch(j->"PROGRAMADA".equals(j.getEstado())))e=EstadoReserva.CONFIRMADA;
  else e=EstadoReserva.PENDIENTE_CONFIRMACION;
  if(js.stream().allMatch(j->j.getFinRealEn()!=null))r.setFinRealEn(js.stream().map(ReservaJornada::getFinRealEn).max(Comparator.naturalOrder()).orElse(ahora));
  if(r.getEstadoActual()!=e)cambiar(r,e,"Actualización de jornadas",u);else reservas.save(r);
 }
 public void procesarPlazos(){
  OffsetDateTime ahora=OffsetDateTime.now();
  for(Long id:reservas.idsPorEstados(EnumSet.of(EstadoReserva.PENDIENTE_ACEPTACION,EstadoReserva.PENDIENTE_PAGO,EstadoReserva.PENDIENTE_CONFIRMACION,EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO))){
    Reserva r=bloquear(id);
    expirar(r);
    if(!r.getJornadas().isEmpty()){
     if(Set.of(EstadoReserva.CONFIRMADA,EstadoReserva.EN_PROGRESO,EstadoReserva.PENDIENTE_CONFIRMACION).contains(r.getEstadoActual())){
      boolean cambio=false;
      for(ReservaJornada j:r.getJornadas())if("PENDIENTE_CONFIRMACION".equals(j.getEstado())&&j.getFinRealEn()!=null&&!ahora.isBefore(j.getFinRealEn().plusHours(cfg().getHorasConfirmacionAutomatica()))){j.confirmar(ahora);cambio=true;}
      if(cambio)actualizarEstadoJornadas(r,ahora,null);
     }
     continue;
    }
    if(r.getEstadoActual()==EstadoReserva.PENDIENTE_CONFIRMACION &&
       r.getFinRealEn()!=null &&
       !ahora.isBefore(r.getFinRealEn().plusHours(cfg().getHorasConfirmacionAutomatica()))){
     r.setConfirmadoEn(ahora);
     cambiar(r,EstadoReserva.COMPLETADA,"Confirmación automática tras 48 horas sin disputa",null);
    }
  }
 }
 @Transactional(readOnly=true)
 public OpinionVista opinionReserva(Long id){
  return valoraciones.findByReservaId(id)
   .map(v->new OpinionVista(v.getPuntuacion(),v.getComentario(),v.getCreadoEn()))
   .orElseGet(()->resenas.findByReservaId(id)
    .map(v->new OpinionVista(v.getCalificacion(),v.getComentario(),v.getCreadoEn())).orElse(null));
 }
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
  bloquearAcompanante(dni);
  if(entrada==null||entrada.size()>21)throw bad("Horarios inválidos");
  for(Horario h:entrada)if(h==null||h.diaSemana()<1||h.diaSemana()>7||h.horaInicio()==null
   ||h.horaFin()==null||!h.horaFin().isAfter(h.horaInicio()))throw bad("Horario inválido");
  OffsetDateTime ahora=OffsetDateTime.now();
  for(Reserva r:ocupacion(dni,ahora,ahora.plusYears(10),null)){
   for(IntervaloOcupado j:intervalos(r,true)){
   ZonedDateTime i=j.inicio().atZoneSameInstant(LIMA),f=j.fin().atZoneSameInstant(LIMA);
   if(!entrada.stream().anyMatch(h->h.diaSemana()==i.getDayOfWeek().getValue()&&!i.toLocalTime().isBefore(h.horaInicio())&&!f.toLocalTime().isAfter(h.horaFin())))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"No puedes retirar un horario que contiene una reserva aceptada");
  }
  }
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
  return new VinculoVista(v.getId(),v.getFamiliar().getDni(),v.getFamiliar().getNombres()+" "+v.getFamiliar().getApellidos(),
   v.getAdultoMayor().getDni(),v.getAdultoMayor().getNombres()+" "+v.getAdultoMayor().getApellidos(),v.getEstado(),
   v.isPermiteReservar(),v.isPermitePagar(),v.isPermiteGestionar(),v.getParentesco()==null?null:v.getParentesco().getNombre());
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
