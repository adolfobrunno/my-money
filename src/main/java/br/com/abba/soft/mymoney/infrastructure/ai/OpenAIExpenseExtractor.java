package br.com.abba.soft.mymoney.infrastructure.ai;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import br.com.abba.soft.mymoney.infrastructure.config.OpenAIProperties;
import br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp.WhatsAppMessageParser;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OpenAIExpenseExtractor {

    private static final Logger log = LoggerFactory.getLogger(OpenAIExpenseExtractor.class);

    private final OpenAIProperties props;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    public OpenAIExpenseExtractor(OpenAIProperties props) {
        this.props = props;
        this.restTemplate = new RestTemplate();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public Optional<Despesa> extract(String rawMessage, String userId, Locale locale) {
        if (rawMessage == null || rawMessage.isBlank()) return Optional.empty();
        // If using fake key, fallback to existing parser to keep dev/test working offline
        if (props.isFakeKey()) {
            log.debug("[OpenAIExpenseExtractor] Fake API key detected, using local parser fallback");
            return WhatsAppMessageParser.tryParse(rawMessage, userId, locale);
        }
        try {
            Map<String, Object> request = buildOpenAIRequest(rawMessage, locale);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(props.getApiKey());
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            String url = props.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("OpenAI response inválida: " + response.getStatusCode());
            }
            Map<String, Object> resp = mapper.readValue(response.getBody(), new TypeReference<Map<String, Object>>(){});
            Map<String, Object> choice0 = firstChoice(resp);
            if (choice0 == null) {
                throw new IllegalStateException("OpenAI não retornou choices");
            }
            String content = extractContent(choice0);
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("OpenAI não retornou conteúdo");
            }
            Despesa d = parseDespesaJson(content, userId);
            return Optional.ofNullable(d);
        } catch (Exception e) {
            log.warn("[OpenAIExpenseExtractor] Falha na extração via OpenAI: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildJsonSchemaMap() {
        Map<String, Object> propsMap = new LinkedHashMap<>();
        propsMap.put("descricao", Map.of("type", "string"));
        propsMap.put("valor", Map.of("type", "number"));
        propsMap.put("tipoPagamento", Map.of(
                "type", "string",
                "enum", List.of("DINHEIRO", "PIX", "CARTAO_CREDITO", "CARTAO_DEBITO")
        ));
        propsMap.put("dataHora", Map.of(
                "type", "string",
                "format", "date-time"
        ));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", List.of("descricao", "valor", "tipoPagamento"));
        schema.put("properties", propsMap);

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", "DespesaSchema");
        jsonSchema.put("schema", schema);
        return jsonSchema;
    }

    private Map<String, Object> buildOpenAIRequest(String rawMessage, Locale locale) {
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_schema");
        responseFormat.put("json_schema", buildJsonSchemaMap());

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "Você é um assistente que extrai uma Despesa a partir de mensagens de WhatsApp em linguagem natural. Sempre responda apenas com JSON válido aderente ao schema. Campos: descricao (string), valor (number), tipoPagamento (DINHEIRO|PIX|CARTAO_CREDITO|CARTAO_DEBITO), dataHora (ISO-8601). Se a mensagem não for uma despesa, responda apenas com um JSON com campos mínimos faltando que fará a validação falhar. Idioma: pt-BR."
        ));
        String localeTag = locale == null ? "pt-BR" : locale.toLanguageTag();
        messages.add(Map.of(
                "role", "user",
                "content", "Locale=" + localeTag + "\nMensagem=\n" + rawMessage
        ));

        Map<String, Object> req = new HashMap<>();
        req.put("model", props.getModel());
        req.put("messages", messages);
        req.put("temperature", 0);
        req.put("response_format", responseFormat);
        return req;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstChoice(Map<String, Object> resp) {
        Object choices = resp.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty()) return null;
        Object first = list.get(0);
        if (!(first instanceof Map<?, ?> map)) return null;
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> choice) {
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> msg)) return null;
        Object content = ((Map<String, Object>) msg).get("content");
        return content == null ? null : content.toString();
    }

    private Despesa parseDespesaJson(String content, String userId) throws Exception {
        Map<String, Object> json = mapper.readValue(content, new TypeReference<Map<String, Object>>(){});
        Despesa d = new Despesa();
        d.setDescricao(asString(json.get("descricao")));
        d.setValor(asBigDecimal(json.get("valor")));
        d.setTipoPagamento(asTipoPagamento(asString(json.get("tipoPagamento"))));
        String dt = asString(json.get("dataHora"));
        d.setDataHora(dt == null || dt.isBlank() ? LocalDateTime.now() : LocalDateTime.parse(dt));
        d.setUserId(userId);
        // Validate to ensure correctness
        d.validate();
        return d;
    }

    private String asString(Object o) { return o == null ? null : String.valueOf(o); }
    private BigDecimal asBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        try { return new BigDecimal(String.valueOf(o)); } catch (Exception e) { return null; }
    }
    private TipoPagamento asTipoPagamento(String s) {
        if (s == null) return null;
        try { return TipoPagamento.valueOf(s.trim().toUpperCase(Locale.ROOT)); } catch (Exception e) { return null; }
    }
}
