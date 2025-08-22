package br.com.abba.soft.mymoney.infrastructure.lambda;

import br.com.abba.soft.mymoney.MyMoneyApplication;
import br.com.abba.soft.mymoney.infrastructure.config.WhatsAppProperties;
import br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp.WhatsAppWebhookController;
import br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp.WhatsAppWebhookService;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight AWS Lambda handler that initializes the Spring context (without starting a web server)
 * and delegates WhatsApp webhook verification and message processing to existing beans.
 *
 * Configure your Lambda handler as:
 *   br.com.abba.soft.mymoney.infrastructure.lambda.WhatsAppWebhookLambdaHandler
 */
public class WhatsAppWebhookLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static volatile ApplicationContext context;
    private static final Object lock = new Object();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static void ensureContext() {
        if (context == null) {
            synchronized (lock) {
                if (context == null) {
                    context = new SpringApplicationBuilder(MyMoneyApplication.class)
                            .web(WebApplicationType.NONE)
                            .run();
                }
            }
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context awsContext) {
        ensureContext();
        String httpMethod = request.getHttpMethod();
        String path = request.getPath();
        Map<String, String> headers = defaultHeaders();

        try {
            // Only handle the WhatsApp webhook path; otherwise 404
            if (path == null || !path.endsWith("/webhooks/whatsapp")) {
                return new APIGatewayProxyResponseEvent().withStatusCode(404).withHeaders(headers).withBody("Not Found");
            }

            WhatsAppWebhookService service = context.getBean(WhatsAppWebhookService.class);
            WhatsAppProperties props = context.getBean(WhatsAppProperties.class);

            if ("GET".equalsIgnoreCase(httpMethod)) {
                return handleGET(request, service, headers);
            } else if ("POST".equalsIgnoreCase(httpMethod)) {
                return handlePOST(request, headers, service);
            } else {
                return new APIGatewayProxyResponseEvent().withStatusCode(405).withHeaders(headers).withBody("Method Not Allowed");
            }
        } catch (Exception ex) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withHeaders(headers).withBody("Internal Error: " + ex.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handlePOST(APIGatewayProxyRequestEvent request, Map<String, String> headers, WhatsAppWebhookService service) throws JsonProcessingException {
        String bodyRaw = request.getBody();
        if (bodyRaw == null || bodyRaw.isBlank()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(202).withHeaders(headers).withBody(json(new WhatsAppWebhookController.WebhookProcessResult(0, 0, 0, List.of("Empty body"))));
        }
        Map<String, Object> payload = mapper.readValue(bodyRaw, Map.class);
        WhatsAppWebhookController.WebhookProcessResult result = service.process(payload);
        return new APIGatewayProxyResponseEvent().withStatusCode(202).withHeaders(headers).withBody(json(result));
    }

    private static APIGatewayProxyResponseEvent handleGET(APIGatewayProxyRequestEvent request, WhatsAppWebhookService service, Map<String, String> headers) {
        Map<String, String> qs = request.getQueryStringParameters();
        String mode = qs != null ? qs.get("hub.mode") : null;
        String verifyToken = qs != null ? qs.get("hub.verify_token") : null;
        String challenge = qs != null ? qs.get("hub.challenge") : null;
        boolean ok = service.verify(mode, verifyToken);
        if (ok) {
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(challenge == null ? "" : challenge);
        }
        return new APIGatewayProxyResponseEvent().withStatusCode(403).withHeaders(headers).withBody("Verification failed");
    }

    private Map<String, String> defaultHeaders() {
        Map<String, String> h = new HashMap<>();
        h.put("Content-Type", "application/json; charset=utf-8");
        return h;
    }

    private String json(Object obj) throws JsonProcessingException {
        return mapper.writeValueAsString(obj);
    }
}
