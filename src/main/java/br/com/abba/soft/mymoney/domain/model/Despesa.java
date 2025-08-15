package br.com.abba.soft.mymoney.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Despesa {
    private String id;
    private String descricao;
    private BigDecimal valor;
    private LocalDateTime dataHora;
    private TipoPagamento tipoPagamento;
    private String userId;

    public Despesa() {}

    public Despesa(String id, String descricao, BigDecimal valor, LocalDateTime dataHora, TipoPagamento tipoPagamento) {
        this.id = id;
        this.descricao = descricao;
        this.valor = valor;
        this.dataHora = dataHora;
        this.tipoPagamento = tipoPagamento;
    }

    public void validate() {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descricao obrigatoria");
        }
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser positivo");
        }
        if (dataHora == null) {
            throw new IllegalArgumentException("Data e hora obrigatorias");
        }
        if (tipoPagamento == null) {
            throw new IllegalArgumentException("Tipo de pagamento obrigatorio");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Despesa despesa = (Despesa) o;
        return Objects.equals(id, despesa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
