package br.com.abba.soft.mymoney.infrastructure.ai;

import br.com.abba.soft.mymoney.infrastructure.config.OpenAIProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class OpenAIAudioTranscriber {

    private static final Logger log = LoggerFactory.getLogger(OpenAIAudioTranscriber.class);

    private final OpenAIProperties props;
    private final RestTemplate restTemplate;

    public OpenAIAudioTranscriber(OpenAIProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Transcribe audio bytes to text using OpenAI's transcription API.
     * Returns null on failure.
     */
    public String transcribe(byte[] audioBytes, String filename, String mimeType, Locale locale) {
        if (audioBytes == null || audioBytes.length == 0) return null;
        if (props.isFakeKey()) {
            // Dev fallback: return a simple stub text so the flow can be tested offline
            return "[transcricao-dev]";
        }
        try {
            String url = props.getBaseUrl().replaceAll("/+$/", "") + "/audio/transcriptions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.setBearerAuth(props.getApiKey());

            // multipart form
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // model: use whisper-1 by default
            body.add("model", textPart("whisper-1"));

            // optional language hint (pt for Portuguese) - OpenAI whisper accepts 'language'
            String lang = (locale != null ? locale.getLanguage() : "pt");
            if (lang != null && !lang.isBlank()) {
                body.add("language", textPart(lang));
            }

            // file part
            body.add("file", new InMemoryFileSystemResource(audioBytes, filename != null ? filename : "audio.ogg", mimeType != null ? mimeType : "audio/ogg"));

            HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, req, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("[OpenAIAudioTranscriber] Non-2xx from transcription API: status={} body={} ", resp.getStatusCode(), resp.getBody());
                return null;
            }
            // Response format: {"text": "..."} (for whisper-1) or may include other fields; extract naively
            String bodyStr = resp.getBody();
            String text = extractTextField(bodyStr);
            if (text == null || text.isBlank()) {
                // Some implementations return plain text; fallback
                return bodyStr;
            }
            return text;
        } catch (Exception e) {
            log.warn("[OpenAIAudioTranscriber] Failed to transcribe: {}", e.toString());
            return null;
        }
    }

    private HttpEntity<byte[]> textPart(String value) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.TEXT_PLAIN);
        return new HttpEntity<>(value.getBytes(StandardCharsets.UTF_8), h);
    }

    // Minimal JSON extractor for {"text":"..."}
    private String extractTextField(String json) {
        if (json == null) return null;
        int i = json.indexOf("\"text\"");
        if (i < 0) return null;
        int colon = json.indexOf(':', i);
        if (colon < 0) return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return null;
        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int j = firstQuote + 1; j < json.length(); j++) {
            char c = json.charAt(j);
            if (escaping) {
                if (c == 'n') sb.append('\n');
                else if (c == 't') sb.append('\t');
                else if (c == 'r') sb.append('\r');
                else sb.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // Simple in-memory Resource for multipart without touching FS
    static class InMemoryFileSystemResource extends org.springframework.core.io.AbstractResource {
        private final byte[] bytes;
        private final String filename;
        private final String contentType;
        InMemoryFileSystemResource(byte[] bytes, String filename, String contentType) {
            this.bytes = bytes; this.filename = filename; this.contentType = contentType;
        }
        @Override public String getDescription() { return "InMemoryFileSystemResource(" + filename + ")"; }
        @Override public String getFilename() { return filename; }
        @Override public java.io.InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public long contentLength() { return bytes.length; }
        @Override public boolean exists() { return true; }
    }
}
