package br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp;

import br.com.abba.soft.mymoney.application.DespesaService;
import br.com.abba.soft.mymoney.infrastructure.ai.OpenAIAudioTranscriber;
import br.com.abba.soft.mymoney.infrastructure.config.WhatsAppProperties;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppIncomingMessageDocument;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.WhatsAppIncomingMessageRepository;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppMessageStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/webhooks/whatsapp")
@Tag(name = "WhatsApp Webhook", description = "Integração com WhatsApp Cloud API (Meta) para receber mensagens contendo despesas")
public class WhatsAppWebhookController {

    private final WhatsAppProperties properties;
    private final WhatsAppWebhookService webhookService;

    public WhatsAppWebhookController(WhatsAppProperties properties, WhatsAppWebhookService webhookService) {
        this.properties = properties;
        this.webhookService = webhookService;
    }

    @GetMapping
    @Operation(summary = "Verificação do Webhook (Meta)", description = "Responde ao desafio de verificação do Meta quando o webhook é configurado")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if (webhookService.verify(mode, verifyToken)) {
            return ResponseEntity.ok(challenge == null ? "" : challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    // Minimal DTOs to parse WhatsApp webhook JSON without bringing extra libs
    public record WhatsMetaRoot(List<Entry> entry) {
        public record Entry(List<Change> changes) {}
        public record Change(Value value) {}
        public record Value(Metadata metadata, List<Message> messages) {}
        public record Metadata(String display_phone_number, String phone_number_id) {}
        public record Message(String from, String id, String timestamp, Text text, String type) {}
        public record Text(String body) {}
    }

    public record WebhookProcessResult(int received, int queued, int valid, List<String> errors) {}

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Receber mensagens do WhatsApp", description = "Recebe mensagens e agenda processamento assíncrono da despesa.")
    @ApiResponse(responseCode = "202", description = "Processado", content = @Content(schema = @Schema(implementation = WebhookProcessResult.class)))
    public ResponseEntity<WebhookProcessResult> receive(@RequestBody Map<String, Object> payload) {
        WebhookProcessResult result = webhookService.process(payload);
        return ResponseEntity.accepted().body(result);
    }

}
