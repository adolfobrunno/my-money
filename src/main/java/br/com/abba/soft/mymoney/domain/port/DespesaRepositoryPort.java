package br.com.abba.soft.mymoney.domain.port;

import br.com.abba.soft.mymoney.domain.model.Despesa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DespesaRepositoryPort {
    Despesa save(Despesa despesa);
    Optional<Despesa> findById(String id);
    List<Despesa> findAll();
    void deleteById(String id);

    List<Despesa> findByUserIdAndDataHoraBetween(String userId, LocalDateTime inicio, LocalDateTime fim);
}
