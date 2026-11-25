package com.seniorlink.api.controller;

import com.seniorlink.api.service.ReservaLocalService;
import com.seniorlink.api.service.FotoPerfilService;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservas/{id}/participantes")
public class FotoReservaController {
    private final ReservaLocalService reservas;
    private final FotoPerfilService fotos;

    public FotoReservaController(ReservaLocalService reservas, FotoPerfilService fotos) {
        this.reservas = reservas;
        this.fotos = fotos;
    }

    @GetMapping("/{dni}/foto")
    public ResponseEntity<byte[]> foto(Authentication auth, @PathVariable Long id, @PathVariable String dni) {
        reservas.autorizarFotoParticipante(auth.getName(), id, dni);
        var foto = fotos.obtener(dni);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(foto.tipo()))
            .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
            .header("X-Content-Type-Options", "nosniff")
            .body(foto.bytes());
    }
}
