package br.com.abba.soft.mymoney.infrastructure.job;

import br.com.abba.soft.mymoney.application.DespesaService;
import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.UsuarioDocument;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.UsuarioRepository;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppIncomingMessageDocument;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.WhatsAppIncomingMessageRepository;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.WhatsAppMessageStatus;
import br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp.WhatsAppApiClient;
import br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp.WhatsAppMessageParser;
import br.com.abba.soft.mymoney.infrastructure.ai.OpenAIExpenseExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class WhatsAppMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageProcessor.class);

    private final WhatsAppIncomingMessageRepository messageRepository;
    private final UsuarioRepository usuarioRepository;
    private final DespesaService despesaService;
    private final WhatsAppApiClient whatsappApiClient;
    private final Locale appLocale;
    private final OpenAIExpenseExtractor openAIExpenseExtractor;

    public WhatsAppMessageProcessor(WhatsAppIncomingMessageRepository messageRepository,
                                    UsuarioRepository usuarioRepository,
                                    DespesaService despesaService,
                                    WhatsAppApiClient whatsappApiClient,
                                    Locale appLocale,
                                    OpenAIExpenseExtractor openAIExpenseExtractor) {
        this.messageRepository = messageRepository;
        this.usuarioRepository = usuarioRepository;
        this.despesaService = despesaService;
        this.whatsappApiClient = whatsappApiClient;
        this.appLocale = appLocale;
        this.openAIExpenseExtractor = openAIExpenseExtractor;
    }

    // Run every minute
    @Scheduled(fixedDelay = 60 * 1000L, initialDelay = 10_000L)
    public void processPendingMessages() {

        log.info("[WhatsAppMessageProcessor] Processando mensagens pendentes...");

        List<WhatsAppIncomingMessageDocument> pendings = messageRepository.findTop50ByStatusOrderByReceivedAtAsc(WhatsAppMessageStatus.PENDING);
        if (pendings.isEmpty()) return;
        for (WhatsAppIncomingMessageDocument msg : pendings) {
            String from = msg.getFrom();
            try {
                msg.setAttempts(msg.getAttempts() + 1);
                msg.setLastAttemptAt(LocalDateTime.now());

                String userId = usuarioRepository.findByTelefone(from)
                        .map(UsuarioDocument::getId)
                        .orElse(null);

                if (userId == null) {
                    throw new IllegalStateException("Usuario nao encontrado para telefone: " + from);
                }

                var maybe = openAIExpenseExtractor.extract(msg.getBody(), userId, appLocale);
                if (maybe.isEmpty()) {
                    throw new IllegalArgumentException("Mensagem invalida: " + msg.getBody());
                }
                Despesa despesa = maybe.get();
                despesa.setUserId(userId); // ensure proper ownership
                Despesa criada = despesaService.criar(despesa);

                msg.setStatus(WhatsAppMessageStatus.PROCESSED);
                msg.setErrorMessage(null);
                messageRepository.save(msg);

                log.info("[WhatsAppMessageProcessor] Despesa registrada com sucesso: {}", criada);

                // Friendly confirmation back to user (best-effort)
                try {
                    whatsappApiClient.sendText(from, buildSuccessMessage(criada));
                } catch (Exception sendEx) {
                    log.warn("[WhatsAppMessageProcessor] Falha ao enviar confirmacao ao {}: {}", from, sendEx.getMessage());
                }
            } catch (Exception ex) {
                log.warn("[WhatsAppMessageProcessor] Falha ao processar mensagem {}: {}", msg.getId(), ex.getMessage());
                msg.setStatus(WhatsAppMessageStatus.ERROR);
                msg.setErrorMessage(ex.getMessage());
                messageRepository.save(msg);
                // Friendly error back to user (best-effort)
                try {
                    whatsappApiClient.sendText(from, buildErrorMessage(ex.getMessage()));
                } catch (Exception sendEx) {
                    log.warn("[WhatsAppMessageProcessor] Falha ao enviar erro ao {}: {}", from, sendEx.getMessage());
                }
            }
        }
    }

    private String buildSuccessMessage(Despesa despesa) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(appLocale);
        String valorFmt = despesa.getValor() != null ? nf.format(despesa.getValor()) : "";
        String dataFmt = despesa.getDataHora() != null ? despesa.getDataHora().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        String tipo = despesa.getTipoPagamento() != null ? despesa.getTipoPagamento().name().replace('_', ' ') : "";
        return "✅ Despesa registrada com sucesso!\n" +
                "Descrição: " + safe(despesa.getDescricao()) + "\n" +
                "Valor: " + valorFmt + "\n" +
                "Pagamento: " + tipo + "\n" +
                "Quando: " + dataFmt;
    }

    private String buildErrorMessage(String reason) {
        return "⚠️ Não consegui registrar sua despesa. Motivo: " + safe(reason) + "\n" +
                "Exemplos válidos:\n" +
                "• Despesa: Almoço; Valor: 35,90; Pagamento: PIX\n" +
                "• Mercado | 120.50 | CARTAO CREDITO";
    }

    private String safe(String s) { return s == null ? "" : s; }
}
