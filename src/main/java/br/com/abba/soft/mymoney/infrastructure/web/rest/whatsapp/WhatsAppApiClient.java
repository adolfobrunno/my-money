package br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp;

import br.com.abba.soft.mymoney.infrastructure.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class WhatsAppApiClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppApiClient.class);

    private final WhatsAppProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppApiClient(WhatsAppProperties props) {
        this.props = props;
    }

    public void sendText(String toPhoneDigits, String body) {
        try {
            String url = "https://graph.facebook.com/v20.0/" + props.getPhoneNumberId() + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getAccessToken());

            Map<String, Object> payload = new HashMap<>();
            payload.put("messaging_product", "whatsapp");
            payload.put("to", toPhoneDigits);
            payload.put("type", "text");
            Map<String, Object> text = new HashMap<>();
            text.put("body", body);
            payload.put("text", text);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[WhatsAppApiClient] Non-2xx response sending WhatsApp message: status={}, body={}",
                        response.getStatusCode(), response.getBody());
            }
        } catch (Exception ex) {
            log.warn("[WhatsAppApiClient] Failed to send WhatsApp message: {}", ex.toString());
        }
    }

    /**
     * Returns a temporary media URL for the given media id, or null.
     */
    public String getMediaUrl(String mediaId) {
        try {
            String url = "https://graph.facebook.com/v20.0/" + mediaId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(props.getAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[WhatsAppApiClient] Non-2xx fetching media metadata: status={} body={}", resp.getStatusCode(), resp.getBody());
                return null;
            }
            String body = resp.getBody();
            // naive extraction of "url":"..."
            int i = body.indexOf("\"url\"");
            if (i < 0) return null;
            int colon = body.indexOf(':', i);
            int q1 = body.indexOf('"', colon + 1);
            int q2 = q1 >= 0 ? body.indexOf('"', q1 + 1) : -1;
            if (q1 >= 0 && q2 > q1) {
                String raw = body.substring(q1 + 1, q2);
                String sanitized = sanitizeMediaUrl(raw);
                if (sanitized == null) {
                    log.warn("[WhatsAppApiClient] Extracted media URL is invalid: {}", raw);
                }
                return sanitized;
            }
            return null;
        } catch (Exception e) {
            log.warn("[WhatsAppApiClient] Failed to get media URL: {}", e.toString());
            return null;
        }
    }

    private String sanitizeMediaUrl(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        // Unescape common JSON escapes produced by Meta: "\/" and "\u0026"
        s = s.replace("\\/", "/");
        s = s.replace("\\u0026", "&");
        // Remove any stray backslashes left by naive extraction
        if (s.indexOf('\\') >= 0) {
            s = s.replace("\\", "");
        }
        // Normalize https scheme slashes (e.g., https:/lookaside -> https://lookaside)
        if (s.startsWith("https:/") && !s.startsWith("https://")) {
            s = s.replaceFirst("^https:/+", "https://");
        }
        if (s.startsWith("http:/") && !s.startsWith("http://")) {
            s = s.replaceFirst("^http:/+", "http://");
        }
        // If scheme missing but looks like a host, prepend https://
        if (!s.startsWith("http://") && !s.startsWith("https://") && s.matches("^[A-Za-z0-9.-]+/.*")) {
            s = "https://" + s;
        }
        // Final light validation
        if (!s.startsWith("http://") && !s.startsWith("https://")) return null;
        return s;
    }

    /**
     * Downloads media bytes from an URL using auth header.
     */
    public byte[] downloadMedia(String mediaUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(props.getAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> resp = restTemplate.exchange(mediaUrl, HttpMethod.GET, entity, byte[].class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[WhatsAppApiClient] Non-2xx downloading media: status={}", resp.getStatusCode());
                return null;
            }
            return resp.getBody();
        } catch (Exception e) {
            log.warn("[WhatsAppApiClient] Failed to download media: {}", e.toString());
            return null;
        }
    }
}
