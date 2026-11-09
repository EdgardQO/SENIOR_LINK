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

@RestController
@RequestMapping("/api/v1/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Reserva> crear(@RequestBody CrearReservaDTO dto) {
        return new ResponseEntity<>(reservaService.crearReserva(dto), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodas() {
        return ResponseEntity.ok(reservaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerPorId(id));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialEstadoReserva>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerHistorial(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Reserva> cambiarEstado(@PathVariable Long id, @RequestParam EstadoReserva nuevoEstado) {
        return ResponseEntity.ok(reservaService.actualizarEstado(id, nuevoEstado));
    }
}