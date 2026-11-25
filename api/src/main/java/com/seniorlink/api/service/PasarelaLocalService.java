package com.seniorlink.api.service;

import com.seniorlink.api.model.entity.*;
import com.seniorlink.api.model.enums.*;
import com.seniorlink.api.repository.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.*;
import java.time.*;
import java.util.*;

@Service
@Profile("local")
@ConditionalOnProperty(name="seniorlink.pagos.simulacion-habilitada",havingValue="true")
@Transactional
public class PasarelaLocalService {
 private final UsuarioRepository usuarios;
 private final TarjetaRepository tarjetas;
 private final TokenTarjetaRepository tokens;
 private final MetodoPagoRepository metodos;
 private final ReservaRepository reservas;
 private final TransaccionRepository transacciones;
 private final MovimientoTarjetaRepository movimientos;
 private final PagoRepository pagos;
 private final ReservaLocalService motor;
 private final PasswordEncoder encoder;
 public PasarelaLocalService(UsuarioRepository usuarios,TarjetaRepository tarjetas,
  TokenTarjetaRepository tokens,MetodoPagoRepository metodos,ReservaRepository reservas,
  TransaccionRepository transacciones,MovimientoTarjetaRepository movimientos,
  PagoRepository pagos,ReservaLocalService motor,PasswordEncoder encoder){
  this.usuarios=usuarios;this.tarjetas=tarjetas;this.tokens=tokens;this.metodos=metodos;
  this.reservas=reservas;this.transacciones=transacciones;this.movimientos=movimientos;
  this.pagos=pagos;this.motor=motor;this.encoder=encoder;
 }
 public record NuevaTarjeta(String numeroDemo,String nombreTitular,String tipo,
  Short mesVencimiento,Short anioVencimiento,String cvv){}
 public record MetodoVista(UUID id,String numeroEnmascarado,String tipo,int mesVencimiento,int anioVencimiento){}
 public record PagoRequest(UUID metodoId,UUID claveIdempotencia,String cvv){}
 public record Resultado(UUID transaccionId,String estado,String codigo,String mensaje,BigDecimal monto){}
 private ResponseStatusException bad(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 private ResponseStatusException no(){return new ResponseStatusException(HttpStatus.FORBIDDEN,"No autorizado");}
 private Usuario actor(String dni){
  Usuario u=usuarios.findById(dni).orElseThrow(this::no);
  if(!Boolean.TRUE.equals(u.getEstaActivo())||!(u.getRol().getNombre().equals("FAMILIAR")||u.getRol().getNombre().equals("ADULTO_MAYOR")))throw no();
  return u;
 }
 private MetodoVista vista(MetodoPago m){
  Tarjeta tarjeta=tokens.findById(m.getToken()).orElseThrow().getTarjeta();
  return new MetodoVista(m.getId(),"•••• "+m.getUltimosCuatro(),m.getTipo(),tarjeta.getMesVencimiento(),tarjeta.getAnioVencimiento());
 }
 public List<MetodoVista> listar(String dni){
  actor(dni);
  return metodos.findByUsuarioDniAndActivoTrue(dni).stream().map(this::vista).toList();
 }
 private String normalizarTitular(String nombre){
  return java.text.Normalizer.normalize(nombre.trim(),java.text.Normalizer.Form.NFD)
   .replaceAll("\\p{M}+","").replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);
 }
 public MetodoVista agregar(String dni,NuevaTarjeta req){
  Usuario u=actor(dni);
  if(req==null||req.numeroDemo()==null||!req.numeroDemo().trim().matches("DEMO-[0-9]{8}")||
   req.nombreTitular()==null||req.nombreTitular().isBlank()||req.nombreTitular().length()>150||
   req.tipo()==null||!Set.of("DEBITO","CREDITO").contains(req.tipo())||
   req.mesVencimiento()==null||req.anioVencimiento()==null||req.mesVencimiento()<1||req.mesVencimiento()>12||
   req.anioVencimiento()<2026||req.anioVencimiento()>2199||req.cvv()==null||!req.cvv().matches("[0-9]{3,4}"))
   throw bad("Introduce únicamente los datos de una tarjeta de simulación registrada");
  Tarjeta tarjeta=tarjetas.bloquearPorNumero(req.numeroDemo().trim()).orElseThrow(()->bad("No se pudo validar la tarjeta"));
  // El catálogo de la pasarela es independiente de los métodos guardados.
  if(!dni.equals(tarjeta.getTitularDni())||
   !normalizarTitular(req.nombreTitular()).equals(normalizarTitular(tarjeta.getNombreTitular()))||
   !req.tipo().equals(tarjeta.getTipoTarjeta())||
   !req.mesVencimiento().equals(tarjeta.getMesVencimiento())||
   !req.anioVencimiento().equals(tarjeta.getAnioVencimiento())||
   !encoder.matches(req.cvv(),tarjeta.getCvvHash()))
   throw bad("No se pudo validar la tarjeta");
  if(!"ACTIVA".equals(tarjeta.getEstado()))throw bad("La tarjeta no está activa");
  if(YearMonth.of(tarjeta.getAnioVencimiento(),tarjeta.getMesVencimiento()).isBefore(YearMonth.now(ZoneId.of("America/Lima"))))
   throw bad("La tarjeta está vencida");
  for(TokenTarjeta existente:tokens.findByTarjetaIdAndActivoTrue(tarjeta.getId())){
   if(metodos.findByToken(existente.getToken()).filter(MetodoPago::isActivo).isPresent())
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Esta tarjeta ya está guardada");
   existente.setActivo(false);tokens.save(existente);
  }
  UUID token=UUID.randomUUID();
  TokenTarjeta vinculo=new TokenTarjeta();vinculo.setToken(token);
  vinculo.setTarjeta(tarjeta);tokens.saveAndFlush(vinculo);
  MetodoPago m=new MetodoPago();m.setUsuario(u);m.setToken(token);
  String numero=tarjeta.getNumeroTarjeta();
  m.setUltimosCuatro(numero.substring(numero.length()-4));m.setTipo(tarjeta.getTipoTarjeta());
  metodos.saveAndFlush(m);return vista(m);
 }
 public void quitar(String dni,UUID metodoId){
  actor(dni);
  MetodoPago m=metodos.findByIdAndUsuarioDniAndActivoTrue(metodoId,dni).orElseThrow(this::no);
  TokenTarjeta vinculo=tokens.findById(m.getToken()).orElseThrow(this::no);
  tarjetas.bloquear(vinculo.getTarjeta().getId()).orElseThrow(this::no);
  m.setActivo(false);metodos.save(m);
  vinculo.setActivo(false);tokens.save(vinculo);
 }
 public Resultado pagar(String dni,Long reservaId,PagoRequest req){
  actor(dni);
  if(req==null||req.metodoId()==null||req.claveIdempotencia()==null||
   req.cvv()==null||!req.cvv().matches("[0-9]{3,4}"))throw bad("Solicitud de pago incompleta");
  // Orden único: acompañante, reserva y tarjeta.
  Reserva r=motor.bloquearParaAgenda(reservaId);
  if(!motor.puedePagar(dni,r))throw no();
  MetodoPago m=metodos.findByIdAndUsuarioDniAndActivoTrue(req.metodoId(),dni).orElseThrow(this::no);
  TokenTarjeta vinculo=tokens.findById(m.getToken()).orElseThrow(this::no);
  if(!vinculo.isActivo())throw no();
  Optional<Transaccion> repetida=transacciones.findByClaveIdempotencia(req.claveIdempotencia());
  if(repetida.isPresent()){
   Transaccion t=repetida.get();
   if(!t.getReservaId().equals(reservaId)||!t.getTarjeta().getId().equals(vinculo.getTarjeta().getId()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Clave de idempotencia reutilizada");
   return resultado(t);
  }
  if(r.getEstadoActual()!=EstadoReserva.PENDIENTE_PAGO||
   r.getVencePagoEn()==null||!OffsetDateTime.now().isBefore(r.getVencePagoEn())||!OffsetDateTime.now().isBefore(r.getFechaHoraInicio()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"La reserva no está habilitada para pago");
  if(transacciones.existsByReservaIdAndEstado(reservaId,"APROBADO")||
   pagos.findByReservaId(reservaId).filter(p->p.getMpEstado()==EstadoPasarelaPago.APROBADO).isPresent())
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Reserva ya pagada");
  Tarjeta t=tarjetas.bloquear(vinculo.getTarjeta().getId()).orElseThrow(this::no);
  if(!dni.equals(t.getTitularDni())||!t.getNumeroTarjeta().matches("DEMO-[0-9]{8}"))throw no();
  if(r.getMontoTotal()==null||r.getMontoTotal().signum()<=0)throw bad("Importe de reserva inválido");
  String rechazo=null;
  if(!"ACTIVA".equals(t.getEstado()))rechazo="TARJETA_INACTIVA";
  else if(YearMonth.of(t.getAnioVencimiento(),t.getMesVencimiento()).isBefore(YearMonth.now(ZoneId.of("America/Lima"))))
   rechazo="TARJETA_VENCIDA";
  else if(!encoder.matches(req.cvv(),t.getCvvHash()))rechazo="CVV_INVALIDO";
  BigDecimal monto=r.getMontoTotal();
  BigDecimal anterior=t.getTipoTarjeta().equals("DEBITO")?t.getSaldo():t.getLimiteCredito().subtract(t.getCreditoUtilizado());
  if(rechazo==null && anterior.compareTo(monto)<0)rechazo="FONDOS_INSUFICIENTES";
  Transaccion tr=new Transaccion();tr.setTarjeta(t);tr.setReservaId(reservaId);
  tr.setClaveIdempotencia(req.claveIdempotencia());tr.setMonto(monto);
  tr.setEstado(rechazo==null?"APROBADO":"RECHAZADO");tr.setCodigo(rechazo==null?"OK":rechazo);
  transacciones.saveAndFlush(tr);
  if(rechazo!=null)return resultado(tr);
  if("DEBITO".equals(t.getTipoTarjeta()))t.setSaldo(t.getSaldo().subtract(monto));
  else t.setCreditoUtilizado(t.getCreditoUtilizado().add(monto));
  tarjetas.save(t);
  BigDecimal posterior=anterior.subtract(monto);
  movimiento(t,tr,"PAGO",monto,anterior,posterior);
  Pago pago=pagos.findByReservaId(reservaId).orElseGet(Pago::new);
  pago.setReserva(r);pago.setMonto(monto);pago.setMoneda("PEN");
  pago.setMpEstado(EstadoPasarelaPago.APROBADO);pago.setMpMetodoPago("SIMULADO");
  pago.setMpPagoId("SIM-"+tr.getId());pago.setMpDetalleEstado("acreditado_local");
  pago.setActualizadoEn(OffsetDateTime.now());pagos.save(pago);
  motor.cambiar(r,EstadoReserva.CONFIRMADA,"Pago simulado acreditado",usuarios.getReferenceById(dni));
  return resultado(tr);
 }
 public record Recibo(UUID referencia,Long reservaId,String estado,BigDecimal monto,String moneda,OffsetDateTime fecha,String metodo,String ultimosCuatro){}
 @Transactional(readOnly=true)
 public List<Recibo> recibos(String dni){
  actor(dni);
  return motor.listar(dni).stream().flatMap(r->transacciones.findByReservaIdAndEstadoInOrderByCreadoEnDesc(r.id(),List.of("APROBADO","REEMBOLSADO")).stream())
   .filter(t->dni.equals(t.getTarjeta().getTitularDni()))
   .map(t->{String numero=t.getTarjeta().getNumeroTarjeta();return new Recibo(t.getId(),t.getReservaId(),t.getEstado(),t.getMonto(),"PEN",t.getCreadoEn(),t.getTarjeta().getTipoTarjeta(),numero.substring(numero.length()-4));})
   .toList();
 }
 private void movimiento(Tarjeta tarjeta,Transaccion tr,String tipo,BigDecimal monto,BigDecimal antes,BigDecimal despues){
  MovimientoTarjeta mv=new MovimientoTarjeta();mv.setTarjeta(tarjeta);mv.setTransaccion(tr);
  mv.setTipoMovimiento(tipo);mv.setMonto(monto);mv.setSaldoAnterior(antes);
  mv.setSaldoPosterior(despues);movimientos.save(mv);
 }
 private Resultado resultado(Transaccion t){
  return new Resultado(t.getId(),t.getEstado(),t.getCodigo(),
   "APROBADO".equals(t.getEstado())?"Pago procesado correctamente":"No se pudo procesar el pago",t.getMonto());
 }
 public Resultado reembolsar(String adminDni,Long reservaId){
  Usuario admin=usuarios.findById(adminDni).orElseThrow(this::no);
  if(!Boolean.TRUE.equals(admin.getEstaActivo())||!"ADMINISTRADOR".equals(admin.getRol().getNombre()))throw no();
  Reserva r=motor.bloquearParaAgenda(reservaId);
  if(r.getEstadoActual()!=EstadoReserva.CANCELADA && r.getEstadoActual()!=EstadoReserva.EN_DISPUTA)
   throw new ResponseStatusException(HttpStatus.CONFLICT,"El reembolso requiere cancelación o disputa");
  Transaccion tr=transacciones.findAll().stream().filter(x->x.getReservaId().equals(reservaId)&&
   ("APROBADO".equals(x.getEstado())||"REEMBOLSADO".equals(x.getEstado()))).findFirst()
   .orElseThrow(()->bad("No existe un cobro aprobado"));
  if("REEMBOLSADO".equals(tr.getEstado()))return resultado(tr);
  Tarjeta t=tarjetas.bloquear(tr.getTarjeta().getId()).orElseThrow();
  BigDecimal antes="DEBITO".equals(t.getTipoTarjeta())?t.getSaldo():t.getLimiteCredito().subtract(t.getCreditoUtilizado());
  if("DEBITO".equals(t.getTipoTarjeta()))t.setSaldo(t.getSaldo().add(tr.getMonto()));
  else t.setCreditoUtilizado(t.getCreditoUtilizado().subtract(tr.getMonto()));
  tarjetas.save(t);movimiento(t,tr,"REEMBOLSO",tr.getMonto(),antes,antes.add(tr.getMonto()));
  tr.setEstado("REEMBOLSADO");tr.setCodigo("REEMBOLSADO");transacciones.save(tr);
  pagos.findByReservaId(reservaId).ifPresent(p->{p.setMpEstado(EstadoPasarelaPago.REEMBOLSADO);pagos.save(p);});
  motor.cambiar(r,EstadoReserva.CANCELADA,"Reembolso local aprobado por administración",admin);
  return resultado(tr);
 }
}
