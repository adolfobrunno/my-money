package br.com.abba.soft.mymoney.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

import br.com.abba.soft.mymoney.infrastructure.persistence.entity.UsuarioDocument;

public interface UsuarioRepository extends MongoRepository<UsuarioDocument, String> {
    Optional<UsuarioDocument> findByEmail(String email);
    Optional<UsuarioDocument> findByTelefone(String telefone);
    Optional<UsuarioDocument> findByEmailOrTelefone(String email, String telefone);
}
