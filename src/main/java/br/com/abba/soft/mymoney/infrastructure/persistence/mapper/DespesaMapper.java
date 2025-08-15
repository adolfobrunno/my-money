package br.com.abba.soft.mymoney.infrastructure.persistence.mapper;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.DespesaDocument;

public class DespesaMapper {
    public static DespesaDocument toDocument(Despesa d) {
        if (d == null) return null;
        return new DespesaDocument(
                d.getId(),
                d.getDescricao(),
                d.getValor(),
                d.getDataHora(),
                d.getTipoPagamento(),
                d.getUserId()
        );
    }

    public static Despesa toDomain(DespesaDocument doc) {
        if (doc == null) return null;
        Despesa d = new Despesa(
                doc.getId(),
                doc.getDescricao(),
                doc.getValor(),
                doc.getDataHora(),
                doc.getTipoPagamento()
        );
        d.setUserId(doc.getUserId());
        return d;
    }
}
