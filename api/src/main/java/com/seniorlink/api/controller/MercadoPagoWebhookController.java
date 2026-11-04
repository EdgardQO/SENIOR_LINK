package com.seniorlink.api.controller;

import com.seniorlink.api.service.PagoMercadoPagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos/mercadopago")
public class MercadoPagoWebhookController {

    private final PagoMercadoPagoService pagoMercadoPagoService;

    public MercadoPagoWebhookController(PagoMercadoPagoService pagoMercadoPagoService) {
        this.pagoMercadoPagoService = pagoMercadoPagoService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> recibirNotificacion(
            @RequestBody Map<String, Object> payload,
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) Long id) {

        // Mercado Pago envía notificaciones por webhooks con tipo "payment"
        if ("payment".equalsIgnoreCase((String) payload.get("type")) || "payment".equalsIgnoreCase(topic)) {
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            String mpPaymentId = data != null ? String.valueOf(data.get("id")) : String.valueOf(id);

            // Simulación del procesamiento del pago (se completa con la llamada a la SDK de Mercado Pago)
            pagoMercadoPagoService.procesarWebhook(
                    mpPaymentId,
                    "approved",
                    "accredited",
                    "credit_card",
                    payload
            );
        }
        return ResponseEntity.ok().build();
    }
}