package br.com.abba.soft.mymoney.infrastructure.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Resumo de despesas em um período")
public class RelatorioDespesasResponse {
    @Schema(description = "Soma total dos valores no período", example = "250.00")
    private BigDecimal total;
    @Schema(description = "Quantidade de despesas no período", example = "5")
    private int quantidade;
    @Schema(description = "Lista de despesas no período")
    private List<DespesaResponse> despesas;
    @Schema(description = "Totais por categoria (para gráfico de pizza)")
    private Map<String, BigDecimal> totalPorCategoria;

    public RelatorioDespesasResponse() {}

    public RelatorioDespesasResponse(BigDecimal total, int quantidade, List<DespesaResponse> despesas) {
        this.total = total;
        this.quantidade = quantidade;
        this.despesas = despesas;
    }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public List<DespesaResponse> getDespesas() { return despesas; }
    public void setDespesas(List<DespesaResponse> despesas) { this.despesas = despesas; }

    public Map<String, BigDecimal> getTotalPorCategoria() { return totalPorCategoria; }
    public void setTotalPorCategoria(Map<String, BigDecimal> totalPorCategoria) { this.totalPorCategoria = totalPorCategoria; }
}
