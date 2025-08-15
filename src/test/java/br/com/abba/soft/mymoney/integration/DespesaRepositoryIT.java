package br.com.abba.soft.mymoney.integration;

import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.domain.model.TipoPagamento;
import br.com.abba.soft.mymoney.domain.port.DespesaRepositoryPort;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true")
public class DespesaRepositoryIT extends MongoIntegrationTest {

    @Autowired
    private DespesaRepositoryPort repositoryPort;

    @Disabled("Integration test with Testcontainers. Run with -Dtestcontainers.enabled=true and remove @Disabled to execute.")
    @Test
    void shouldSaveAndLoadDespesa() {
        assumeTrue(Boolean.getBoolean("testcontainers.enabled"),
                "Set -Dtestcontainers.enabled=true to run Testcontainers integration tests");
        Despesa despesa = new Despesa(null, "Mercado", new BigDecimal("123.45"), LocalDateTime.now(), TipoPagamento.CARTAO_CREDITO);
        despesa.validate();

        Despesa saved = repositoryPort.save(despesa);
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getDescricao()).isEqualTo("Mercado");

        var loadedOpt = repositoryPort.findById(saved.getId());
        assertThat(loadedOpt).isPresent();
        var loaded = loadedOpt.get();
        assertThat(loaded.getValor()).isEqualByComparingTo("123.45");
        assertThat(loaded.getTipoPagamento()).isEqualTo(TipoPagamento.CARTAO_CREDITO);

        var all = repositoryPort.findAll();
        assertThat(all).extracting(Despesa::getId).contains(saved.getId());

        repositoryPort.deleteById(saved.getId());
        assertThat(repositoryPort.findById(saved.getId())).isEmpty();
    }
}
