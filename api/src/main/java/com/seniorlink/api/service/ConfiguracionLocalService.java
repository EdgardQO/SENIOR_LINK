package com.seniorlink.api.service;
import com.seniorlink.api.model.entity.ConfiguracionReservas;
import com.seniorlink.api.repository.ConfiguracionReservasRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;

@Service
@Transactional
public class ConfiguracionLocalService {
 private final ConfiguracionReservasRepository repositorio;
 public ConfiguracionLocalService(ConfiguracionReservasRepository repositorio){this.repositorio=repositorio;}
 public record Parametros(int minutosAceptacion,int minutosPago,int minutosAnticipacion,
  int minutosDuracionMinima,int minutosDuracionMaxima,int minutosInicioAnticipado,
  int horasConfirmacionAutomatica,BigDecimal tarifaMinimaHora,
  BigDecimal tarifaMaximaHora,BigDecimal porcentajeComision){}
 public Parametros obtener(){
  ConfiguracionReservas c=repositorio.findById((short)1)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Configuración no encontrada"));
  return new Parametros(c.getMinutosAceptacion(),c.getMinutosPago(),c.getMinutosAnticipacion(),
   c.getMinutosDuracionMinima(),c.getMinutosDuracionMaxima(),
   c.getMinutosInicioAnticipado(),c.getHorasConfirmacionAutomatica(),
   c.getTarifaMinimaHora(),c.getTarifaMaximaHora(),c.getPorcentajeComision());
 }
 public Parametros guardar(Parametros p){
  if(p==null||p.minutosAceptacion()<5||p.minutosAceptacion()>180||
   p.minutosPago()<5||p.minutosPago()>120||
   p.minutosAnticipacion()<60||p.minutosAnticipacion()>1440||
   p.minutosDuracionMinima()<30||p.minutosDuracionMaxima()>1440||
   p.minutosDuracionMaxima()<p.minutosDuracionMinima()||
   p.minutosInicioAnticipado()<0||p.minutosInicioAnticipado()>60||
   p.horasConfirmacionAutomatica()<1||p.horasConfirmacionAutomatica()>168||
   p.tarifaMinimaHora()==null||p.tarifaMaximaHora()==null||p.porcentajeComision()==null||
   p.tarifaMinimaHora().scale()>2||p.tarifaMaximaHora().scale()>2||p.porcentajeComision().scale()>2||
   p.tarifaMinimaHora().compareTo(BigDecimal.ONE)<0||
   p.tarifaMaximaHora().compareTo(p.tarifaMinimaHora())<0||
   p.tarifaMaximaHora().compareTo(new BigDecimal("200"))>0||
   p.porcentajeComision().compareTo(BigDecimal.ZERO)<0||
   p.porcentajeComision().compareTo(new BigDecimal("30"))>0)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Parámetros de reservas fuera de rango");
  ConfiguracionReservas c=repositorio.findById((short)1).orElseThrow();
  c.setMinutosAceptacion(p.minutosAceptacion());c.setMinutosPago(p.minutosPago());
  c.setMinutosAnticipacion(p.minutosAnticipacion());
  c.setMinutosDuracionMinima(p.minutosDuracionMinima());
  c.setMinutosDuracionMaxima(p.minutosDuracionMaxima());
  c.setMinutosInicioAnticipado(p.minutosInicioAnticipado());
  c.setHorasConfirmacionAutomatica(p.horasConfirmacionAutomatica());
  c.setTarifaMinimaHora(p.tarifaMinimaHora());c.setTarifaMaximaHora(p.tarifaMaximaHora());
  c.setPorcentajeComision(p.porcentajeComision());repositorio.save(c);return obtener();
 }
}
