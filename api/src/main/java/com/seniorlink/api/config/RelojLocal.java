package com.seniorlink.api.config;
import com.seniorlink.api.service.ReservaLocalService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
@Configuration
@Profile("local")
@EnableScheduling
public class RelojLocal {
 private final ReservaLocalService reservas;
 public RelojLocal(ReservaLocalService reservas){this.reservas=reservas;}
 @Scheduled(fixedDelay=60_000)
 public void comprobar(){reservas.procesarPlazos();}
}
