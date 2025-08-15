package br.com.abba.soft.mymoney.infrastructure.web.rest.despesas;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.DespesaRequest;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.DespesaResponse;

public class DespesaDtoMapper {
    public static Despesa toDomain(DespesaRequest req) {
        var d = new Despesa();
        d.setDescricao(req.getDescricao());
        d.setValor(req.getValor());
        d.setDataHora(req.getDataHora());
        d.setTipoPagamento(req.getTipoPagamento());
        return d;
    }

    public static DespesaResponse toResponse(Despesa d) {
        return new DespesaResponse(
                d.getId(),
                d.getDescricao(),
                d.getValor(),
                d.getDataHora(),
                d.getTipoPagamento()
        );
    }
}
