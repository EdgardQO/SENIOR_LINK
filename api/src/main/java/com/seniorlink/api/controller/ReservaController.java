package com.seniorlink.api.controller;

import com.seniorlink.api.dto.CrearReservaDTO;
import com.seniorlink.api.model.entity.HistorialEstadoReserva;
import com.seniorlink.api.model.entity.Reserva;
import com.seniorlink.api.model.enums.EstadoReserva;
import com.seniorlink.api.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody CrearReservaDTO dto) {
        Reserva nuevaReserva = reservaService.crearReserva(dto);
        return new ResponseEntity<>(nuevaReserva, HttpStatus.CREATED);
    }

    @GetMapping("/{codigoIdentificador}")
    public ResponseEntity<Reserva> obtenerPorId(@PathVariable UUID codigoIdentificador) {
        return ResponseEntity.ok(reservaService.obtenerPorId(codigoIdentificador));
    }

    @GetMapping("/{codigoIdentificador}/historial")
    public ResponseEntity<List<HistorialEstadoReserva>> obtenerHistorial(@PathVariable UUID codigoIdentificador) {
        return ResponseEntity.ok(reservaService.obtenerHistorial(codigoIdentificador));
    }

    @PatchMapping("/{codigoIdentificador}/estado")
    public ResponseEntity<Reserva> cambiarEstado(
            @PathVariable UUID codigoIdentificador,
            @RequestParam EstadoReserva nuevoEstado) {
        return ResponseEntity.ok(reservaService.actualizarEstado(codigoIdentificador, nuevoEstado));
    }

    //todas las reservas
    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }
}