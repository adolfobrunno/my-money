package br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp;

import br.com.abba.soft.mymoney.application.DespesaService;
import br.com.abba.soft.mymoney.infrastructure.ai.OpenAIAudioTranscriber;
import br.com.abba.soft.mymoney.infrastructure.config.WhatsAppProperties;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppIncomingMessageDocument;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppMessageStatus;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.WhatsAppIncomingMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WhatsAppWebhookService {

    private final WhatsAppProperties properties;
    private final WhatsAppIncomingMessageRepository messageRepository;
    private final DespesaService despesaService; // kept for potential future use
    private final Locale appLocale;
    private final WhatsAppApiClient whatsappApiClient;
    private final OpenAIAudioTranscriber audioTranscriber;

    public WhatsAppWebhookService(WhatsAppProperties properties,
                                  WhatsAppIncomingMessageRepository messageRepository,
                                  DespesaService despesaService,
                                  Locale appLocale,
                                  WhatsAppApiClient whatsappApiClient,
                                  OpenAIAudioTranscriber audioTranscriber) {
        this.properties = properties;
        this.messageRepository = messageRepository;
        this.despesaService = despesaService;
        this.appLocale = appLocale;
        this.whatsappApiClient = whatsappApiClient;
        this.audioTranscriber = audioTranscriber;
    }

    public boolean verify(String mode, String verifyToken) {
        return "subscribe".equals(mode) && Objects.equals(properties.getVerifyToken(), verifyToken);
    }

    public WhatsAppWebhookController.WebhookProcessResult process(Map<String, Object> payload) {
        int received = 0;
        int queued = 0;
        int valid = 0;
        List<String> errors = new ArrayList<>();

        try {
            Object entryObj = payload.get("entry");
            if (!(entryObj instanceof List<?> entries)) {
                return new WhatsAppWebhookController.WebhookProcessResult(0, 0, 0, List.of("No entry"));
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

                        WhatsAppIncomingMessageDocument doc = new WhatsAppIncomingMessageDocument();
                        doc.setFrom(normalizePhone(from));
                        doc.setBody(body);
                        doc.setReceivedAt(LocalDateTime.now());
                        doc.setStatus(WhatsAppMessageStatus.PENDING);
                        messageRepository.save(doc);
                        queued++;

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
        return new WhatsAppWebhookController.WebhookProcessResult(received, queued, valid, errors);
    }

    private static String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        // Ensure Brazilian WhatsApp numbers (country 55) always include the 9th digit
        // Expected formats (digits only):
        // - With 9th digit: 55 + AA (2) + 9 + XXXXXXXX (8) => 13 digits
        // - Without 9th digit: 55 + AA (2) + XXXXXXXX (8) => 12 digits (needs insertion)
        if (digits.startsWith("55") && digits.length() == 12) {
            // Insert '9' right after country(2) + area(2) => position 4
            digits = digits.substring(0, 4) + "9" + digits.substring(4);
        }
        return digits;
    }
}
