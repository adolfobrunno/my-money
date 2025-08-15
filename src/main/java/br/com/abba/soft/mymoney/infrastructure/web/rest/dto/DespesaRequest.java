package br.com.abba.soft.mymoney.infrastructure.web.rest.dto;

import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Payload para criação/atualização de despesa")
public class DespesaRequest {
    @NotBlank
    @Schema(description = "Descrição da despesa", example = "Almoço no restaurante")
    private String descricao;
    @NotNull @DecimalMin(value = "0.01")
    @Schema(description = "Valor da despesa", example = "59.90", minimum = "0.01")
    private BigDecimal valor;
    @NotNull
    @Schema(description = "Data e hora da despesa (ISO 8601)", example = "2025-08-10T12:30:00")
    private LocalDateTime dataHora;
    @NotNull
    @Schema(description = "Tipo de pagamento", example = "CREDITO")
    private TipoPagamento tipoPagamento;

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public TipoPagamento getTipoPagamento() { return tipoPagamento; }
    public void setTipoPagamento(TipoPagamento tipoPagamento) { this.tipoPagamento = tipoPagamento; }
}
