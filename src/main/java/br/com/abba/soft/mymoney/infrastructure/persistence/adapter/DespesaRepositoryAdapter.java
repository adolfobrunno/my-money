package br.com.abba.soft.mymoney.infrastructure.persistence.adapter;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.domain.port.DespesaRepositoryPort;
import br.com.abba.soft.mymoney.infrastructure.persistence.mapper.DespesaMapper;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.DespesaRepository;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class DespesaRepositoryAdapter implements DespesaRepositoryPort {

    private final DespesaRepository repository;

    public DespesaRepositoryAdapter(DespesaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Despesa save(Despesa despesa) {
        var saved = repository.save(DespesaMapper.toDocument(despesa));
        return DespesaMapper.toDomain(saved);
    }

    @Override
    public Optional<Despesa> findById(String id) {
        return repository.findById(id).map(DespesaMapper::toDomain);
    }

    @Override
    public List<Despesa> findAll() {
        return repository.findAll().stream().map(DespesaMapper::toDomain).toList();
    }

    @Override
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    public List<Despesa> findByUserIdAndDataHoraBetween(String userId, LocalDateTime inicio, LocalDateTime fim) {
        return repository.findByUserIdAndDataHoraBetween(userId, inicio, fim)
                .stream()
                .map(DespesaMapper::toDomain)
                .toList();
    }
}
