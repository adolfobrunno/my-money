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
}
