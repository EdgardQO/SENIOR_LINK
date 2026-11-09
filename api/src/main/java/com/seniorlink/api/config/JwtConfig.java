package com.seniorlink.api.config;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
@Configuration
public class JwtConfig {
    @Bean
    public SecretKey jwtSecretKey(@Value("${seniorlink.jwt.secret-base64}") String encoded) {
        byte[] bytes;
        try { bytes = Base64.getDecoder().decode(encoded); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("JWT_SECRET_BASE64 debe ser Base64 válido", e); }
        if (bytes.length < 32) throw new IllegalStateException("JWT_SECRET_BASE64 requiere al menos 32 bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
