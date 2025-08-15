package br.com.abba.soft.mymoney.infrastructure.persistence.mapper;

import br.com.abba.soft.mymoney.domain.model.Usuario;
import br.com.abba.soft.mymoney.infrastructure.persistence.entity.UsuarioDocument;

public class UsuarioMapper {
    public static UsuarioDocument toDocument(Usuario u) {
        if (u == null) return null;
        return new UsuarioDocument(
                u.getId(),
                u.getNome(),
                u.getEmail(),
                u.getTelefone(),
                u.getSenhaHash()
        );
    }

    public static Usuario toDomain(UsuarioDocument doc) {
        if (doc == null) return null;
        return new Usuario(
                doc.getId(),
                doc.getNome(),
                doc.getEmail(),
                doc.getTelefone(),
                doc.getSenhaHash()
        );
    }
}
