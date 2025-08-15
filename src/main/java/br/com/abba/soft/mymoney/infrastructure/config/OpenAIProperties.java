package br.com.abba.soft.mymoney.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAIProperties {
    /** API key for OpenAI. Use a fake default in application.yml for dev. */
    private String apiKey;
    /** Base URL for OpenAI API (e.g., https://api.openai.com/v1) */
    private String baseUrl = "https://api.openai.com/v1";
    /** Model to use (e.g., gpt-4o-mini) */
    private String model = "gpt-4o-mini";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public boolean isFakeKey() {
        if (apiKey == null || apiKey.isBlank()) return true;
        String k = apiKey.trim().toUpperCase();
        return k.startsWith("FAKE") || k.contains("PLACEHOLDER") || k.equals("TEST") || k.equals("DUMMY");
    }
}
