package br.com.abba.soft.mymoney.infrastructure.persistence.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

import br.com.abba.soft.mymoney.infrastructure.persistence.entity.DespesaDocument;

public interface DespesaRepository extends MongoRepository<DespesaDocument, String> {
    List<DespesaDocument> findByUserIdAndDataHoraBetween(String userId, LocalDateTime inicio, LocalDateTime fim);
}
