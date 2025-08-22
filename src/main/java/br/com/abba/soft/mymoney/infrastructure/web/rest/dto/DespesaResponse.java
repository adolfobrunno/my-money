package br.com.abba.soft.mymoney.infrastructure.web.rest.dto;

import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import br.com.abba.soft.mymoney.domain.model.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Schema(description = "Informações da despesa retornadas pela API")
public class DespesaResponse {
    @Schema(description = "Identificador da despesa", example = "66bdb3a4e4b0f95c1c1f9a22")
    private String id;
    @Schema(description = "Descrição da despesa", example = "Almoço no restaurante")
    private String descricao;
    @Schema(description = "Valor da despesa", example = "59.90")
    private BigDecimal valor;
    @Schema(description = "Data e hora da despesa (ISO 8601)", example = "2025-08-10T12:30:00")
    private ZonedDateTime dataHora;
    @Schema(description = "Tipo de pagamento", example = "CARTAO_CREDITO")
    private TipoPagamento tipoPagamento;
    @Schema(description = "Categoria da despesa", example = "ALIMENTACAO")
    private Categoria categoria;

    public DespesaResponse() {}

    public DespesaResponse(String id, String descricao, BigDecimal valor, ZonedDateTime dataHora, TipoPagamento tipoPagamento, Categoria categoria) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.dataHora = dataHora;
        this.tipoPagamento = tipoPagamento;
        this.categoria = categoria;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public ZonedDateTime getDataHora() { return dataHora; }
    public void setDataHora(ZonedDateTime dataHora) { this.dataHora = dataHora; }
    public TipoPagamento getTipoPagamento() { return tipoPagamento; }
    public void setTipoPagamento(TipoPagamento tipoPagamento) { this.tipoPagamento = tipoPagamento; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}
