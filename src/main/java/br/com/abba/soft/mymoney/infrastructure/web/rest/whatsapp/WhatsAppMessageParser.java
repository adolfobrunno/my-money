package br.com.abba.soft.mymoney.infrastructure.web.rest.whatsapp;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import br.com.abba.soft.mymoney.domain.model.Categoria;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhatsAppMessageParser {

    // Patterns we support, kept simple and resilient
    private static final Pattern PATTERN_SEMICOLON = Pattern.compile(
            "(?i)\\n?\\s*despesa\\s*[:=]\\s*(.+?)\\s*;\\s*valor\\s*[:=]\\s*([0-9.,]+)\\s*;\\s*pagamento\\s*[:=]\\s*([\\n\\r\\t A-ZÇÃÕÁÉÍÓÚÂÊÔ-]+)\\s*");

    private static final Pattern PATTERN_PIPE = Pattern.compile(
            "(?i)\\s*(.+?)\\s*[|]\\s*([0-9.,]+)\\s*[|]\\s*([A-ZÇÃÕÁÉÍÓÚÂÊÔ ]+)\\s*");

    private static final Map<String, TipoPagamento> PAYMENT_ALIASES = new HashMap<>();
    static {
        PAYMENT_ALIASES.put("DINHEIRO", TipoPagamento.DINHEIRO);
        PAYMENT_ALIASES.put("CASH", TipoPagamento.DINHEIRO);
        PAYMENT_ALIASES.put("PIX", TipoPagamento.PIX);
        PAYMENT_ALIASES.put("CARTAO", TipoPagamento.CARTAO_CREDITO);
        PAYMENT_ALIASES.put("CARTAO CREDITO", TipoPagamento.CARTAO_CREDITO);
        PAYMENT_ALIASES.put("CREDITO", TipoPagamento.CARTAO_CREDITO);
        PAYMENT_ALIASES.put("CARTAO DEBITO", TipoPagamento.CARTAO_DEBITO);
        PAYMENT_ALIASES.put("DEBITO", TipoPagamento.CARTAO_DEBITO);
    }

    public static Optional<Despesa> tryParse(String textBody, String userIdFromWhats) {
        // Backward-compatible overload that defaults to PT-BR
        return tryParse(textBody, userIdFromWhats, Locale.forLanguageTag("pt-BR"));
    }

    public static Optional<Despesa> tryParse(String textBody, String userIdFromWhats, Locale locale) {
        if (textBody == null || textBody.isBlank()) return Optional.empty();
        String body = textBody.trim();

        Matcher m = PATTERN_SEMICOLON.matcher(body);
        if (m.matches()) {
            return buildDespesa(m.group(1), m.group(2), m.group(3), userIdFromWhats, locale);
        }
        m = PATTERN_PIPE.matcher(body);
        if (m.matches()) {
            return buildDespesa(m.group(1), m.group(2), m.group(3), userIdFromWhats, locale);
        }
        // fallback: space-separated "descricao valor pagamento"
        String[] parts = body.split("\\s+[|]\\s+|\\s+;");
        // if not split by | or ;, try simple tokens
        if (parts.length < 3) {
            parts = body.split("\\s{2,}");
        }
        if (parts.length >= 3) {
            return buildDespesa(parts[0], parts[1], parts[2], userIdFromWhats, locale);
        }
        return Optional.empty();
    }

    private static Optional<Despesa> buildDespesa(String descricaoRaw, String valorRaw, String pagamentoRaw, String userIdFromWhats, Locale locale) {
        String descricao = descricaoRaw == null ? null : descricaoRaw.trim();
        BigDecimal valor = parseNumber(valorRaw, locale);
        TipoPagamento tipo = parsePagamento(pagamentoRaw);
        if (descricao == null || descricao.isBlank() || valor == null || tipo == null) {
            return Optional.empty();
        }
        Despesa d = new Despesa();
        d.setDescricao(descricao);
        d.setValor(valor);
        d.setTipoPagamento(tipo);
        d.setDataHora(ZonedDateTime.now());
        d.setCategoria(inferCategoria(descricao));
        d.setUserId(userIdFromWhats);
        return Optional.of(d);
    }

    private static BigDecimal parseNumber(String raw, Locale locale) {
        if (raw == null) return null;
        String s = raw.trim();
        // Accept both 12.34 and 12,34; if both separators present, assume last is decimal
        if (s.contains(",") && s.contains(".")) {
            // Remove thousand separators heuristically
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                s = s.replace(".", "");
                s = s.replace(',', '.');
            } else {
                s = s.replace(",", "");
            }
        } else if (s.contains(",")) {
            s = s.replace('.', ' ').replace(" ", "");
            s = s.replace(',', '.');
        }
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            try {
                Number n = NumberFormat.getNumberInstance(locale == null ? Locale.forLanguageTag("pt-BR") : locale).parse(raw);
                return new BigDecimal(n.toString());
            } catch (ParseException ex) {
                return null;
            }
        }
    }

    private static TipoPagamento parsePagamento(String raw) {
        if (raw == null) return null;
        String key = raw.trim().toUpperCase(Locale.ROOT).replaceAll("\\\\s+", " ");
        if (PAYMENT_ALIASES.containsKey(key)) return PAYMENT_ALIASES.get(key);
        try {
            return TipoPagamento.valueOf(key);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Categoria inferCategoria(String descricao) {
        if (descricao == null) return Categoria.OUTRAS;
        String desc = descricao.toLowerCase(Locale.ROOT);
        if (desc.matches(".*\\b(almo(c|ç)o|jantar|comida|restaurante|lanche|hamb(ur|ú)guer|pizza|padaria|refe(i|í)cao|marmita|bar)\\b.*"))
            return Categoria.ALIMENTACAO;
        if (desc.matches(".*\\b(mercado|supermercado|compras|hortifruti|a(c|ç)ougue|sacolão|atacado)\\b.*"))
            return Categoria.MERCADO;
        if (desc.matches(".*\\b(curso|faculdade|escola|mensalidade|material|livro|aluno|ensino|ead|matr(i|í)cula)\\b.*"))
            return Categoria.EDUCACAO;
        if (desc.matches(".*\\b(cinema|lazer|viagem|passeio|parque|show|assinatura|netflix|spotify|game|jogo)\\b.*"))
            return Categoria.LAZER;
        if (desc.matches(".*\\b(luz|energia|agua|internet|telefone|aluguel|condominio|gas|conta|boleto)\\b.*"))
            return Categoria.CONTAS_DO_DIA_A_DIA;
        return Categoria.OUTRAS;
    }
}
