package br.com.abba.soft.mymoney.application;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.domain.port.DespesaRepositoryPort;
import br.com.abba.soft.mymoney.infrastructure.security.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DespesaService {

    private final DespesaRepositoryPort repository;

    public DespesaService(DespesaRepositoryPort repository) {
        this.repository = repository;
    }

    public Despesa criar(Despesa despesa) {
        despesa.validate();
        return repository.save(despesa);
    }

    public Despesa atualizar(String id, Despesa despesa) {
        despesa.setId(id);
        despesa.validate();
        var existente = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Despesa nao encontrada"));
        // enforce ownership
        String userId = SecurityUtils.currentUserIdOrNull();
        if (existente.getUserId() == null || !existente.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Despesa nao encontrada");
        }
        despesa.setUserId(userId);
        return repository.save(despesa);
    }

    public void excluir(String id) {
        // enforce ownership
        String userId = SecurityUtils.currentUserIdOrNull();
        var existente = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Despesa nao encontrada"));
        if (existente.getUserId() == null || !existente.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Despesa nao encontrada");
        }
        repository.deleteById(id);
    }

    public Despesa buscar(String id) {
        var despesa = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Despesa nao encontrada"));
        String userId = SecurityUtils.currentUserIdOrNull();
        if (despesa.getUserId() == null || !despesa.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Despesa nao encontrada");
        }
        return despesa;
    }

    public List<Despesa> listar() {
        String userId = SecurityUtils.currentUserIdOrNull();
        return repository.findAll().stream().filter(d -> userId != null && userId.equals(d.getUserId())).toList();
    }

    public List<Despesa> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("Periodo invalido: inicio e fim sao obrigatorios");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Periodo invalido: fim deve ser depois de inicio");
        }
        String userId = SecurityUtils.currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("Usuario nao autenticado");
        }
        return repository.findByUserIdAndDataHoraBetween(userId, inicio, fim);
    }

    public BigDecimal totalizarValor(List<Despesa> despesas) {
        return despesas.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
