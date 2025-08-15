package br.com.abba.soft.mymoney.infrastructure.persistence.entity;

import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "despesas")
public class DespesaDocument {
    @Id
    private String id;
    private String descricao;
    private BigDecimal valor;
    private LocalDateTime dataHora;
    private TipoPagamento tipoPagamento;
    private String userId;

    public DespesaDocument() {}

    public DespesaDocument(String id, String descricao, BigDecimal valor, LocalDateTime dataHora, TipoPagamento tipoPagamento, String userId) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.dataHora = dataHora;
        this.tipoPagamento = tipoPagamento;
        this.userId = userId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public TipoPagamento getTipoPagamento() { return tipoPagamento; }
    public void setTipoPagamento(TipoPagamento tipoPagamento) { this.tipoPagamento = tipoPagamento; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
