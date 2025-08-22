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
    private final WhatsAppIncomingMessageRepository messageRepository;
    // keeping service reference in case of future use, but no immediate persistence now
    private final DespesaService despesaService;
    private final Locale appLocale;
    private final WhatsAppApiClient whatsappApiClient;
    private final OpenAIAudioTranscriber audioTranscriber;

    public WhatsAppWebhookController(WhatsAppProperties properties, WhatsAppIncomingMessageRepository messageRepository, DespesaService despesaService, Locale appLocale, WhatsAppApiClient whatsappApiClient, OpenAIAudioTranscriber audioTranscriber) {
        this.properties = properties;
        this.messageRepository = messageRepository;
        this.despesaService = despesaService;
        this.appLocale = appLocale;
        this.whatsappApiClient = whatsappApiClient;
        this.audioTranscriber = audioTranscriber;
    }

    @GetMapping
    @Operation(summary = "Verificação do Webhook (Meta)", description = "Responde ao desafio de verificação do Meta quando o webhook é configurado")
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        if ("subscribe".equals(mode) && Objects.equals(properties.getVerifyToken(), verifyToken)) {
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
        int received = 0;
        int queued = 0;
        int valid = 0; // quantas mensagens parecem válidas (parse ok), embora não persistamos agora
        List<String> errors = new ArrayList<>();

        try {
            Object entryObj = payload.get("entry");
            if (!(entryObj instanceof List<?> entries)) {
                return ResponseEntity.accepted().body(new WebhookProcessResult(0, 0, 0, List.of("No entry")));
            }
            for (Object e : entries) {
                Map<?,?> entry = (Map<?,?>) e;
                Object changesObj = entry.get("changes");
                if (!(changesObj instanceof List<?> changes)) continue;
                for (Object c : changes) {
                    Map<?,?> change = (Map<?,?>) c;
                    Map<?,?> value = (Map<?,?>) change.get("value");
                    if (value == null) continue;
                    Object messagesObj = value.get("messages");
                    if (!(messagesObj instanceof List<?> messages)) continue;
                    for (Object m : messages) {
                        received++;
                        Map<?,?> message = (Map<?,?>) m;
                        String type = Objects.toString(message.get("type"), "");
                        String from = Objects.toString(message.get("from"), "unknown");
                        String body = null;
                        if ("text".equals(type)) {
                            Map<?,?> text = (Map<?,?>) message.get("text");
                            body = text == null ? null : Objects.toString(text.get("body"), null);
                        } else if ("audio".equals(type)) {
                            // WhatsApp audio message: must fetch media and transcribe
                            Map<?,?> audio = (Map<?,?>) message.get("audio");
                            String mediaId = audio == null ? null : Objects.toString(audio.get("id"), null);
                            String mimeType = audio == null ? null : Objects.toString(audio.get("mime_type"), null);
                            if (mediaId == null) {
                                errors.add("Audio message without media id");
                                continue;
                            }
                            String mediaUrl = whatsappApiClient.getMediaUrl(mediaId);
                            if (mediaUrl == null) {
                                errors.add("Failed to get media URL for id=" + mediaId);
                                continue;
                            }
                            byte[] bytes = whatsappApiClient.downloadMedia(mediaUrl);
                            if (bytes == null || bytes.length == 0) {
                                errors.add("Failed to download media for id=" + mediaId);
                                continue;
                            }
                            String ext = ".bin";
                            if (mimeType != null) {
                                if (mimeType.contains("ogg")) ext = ".ogg";
                                else if (mimeType.contains("mpeg")) ext = ".mp3";
                                else if (mimeType.contains("aac")) ext = ".aac";
                                else if (mimeType.contains("wav")) ext = ".wav";
                                else if (mimeType.contains("amr")) ext = ".amr";
                            }
                            String filename = "audio-" + mediaId + ext;
                            String transcript = audioTranscriber.transcribe(bytes, filename, mimeType != null ? mimeType : "application/octet-stream", appLocale);
                            if (transcript == null || transcript.isBlank()) {
                                errors.add("Failed to transcribe audio for id=" + mediaId);
                                continue;
                            }
                            body = transcript;
                        } else {
                            errors.add("Ignoring unsupported message type: " + type);
                            continue;
                        }

                        // queue message for async processing
                        WhatsAppIncomingMessageDocument doc = new WhatsAppIncomingMessageDocument();
                        doc.setFrom(normalizePhone(from));
                        doc.setBody(body);
                        doc.setReceivedAt(LocalDateTime.now());
                        doc.setStatus(WhatsAppMessageStatus.PENDING);
                        messageRepository.save(doc);
                        queued++;

                        // Quick validation feedback (optional): try parse to inform if looks valid
                        var maybe = WhatsAppMessageParser.tryParse(body, "whatsapp:" + from, appLocale);
                        if (maybe.isPresent()) {
                            valid++;
                        } else {
                            errors.add("Could not parse text: " + body);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            errors.add("Unexpected error: " + ex.getMessage());
        }
        return ResponseEntity.accepted().body(new WebhookProcessResult(received, queued, valid, errors));
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[^0-9]", "");
    }
}
