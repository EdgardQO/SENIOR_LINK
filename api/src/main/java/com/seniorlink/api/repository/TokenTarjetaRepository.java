package com.seniorlink.api.repository;
import com.seniorlink.api.model.entity.TokenTarjeta;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface TokenTarjetaRepository extends JpaRepository<TokenTarjeta,UUID> {}
