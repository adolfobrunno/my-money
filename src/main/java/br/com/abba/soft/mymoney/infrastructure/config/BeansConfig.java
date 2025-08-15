package br.com.abba.soft.mymoney.infrastructure.config;

import br.com.abba.soft.mymoney.application.DespesaService;
import br.com.abba.soft.mymoney.domain.port.DespesaRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class BeansConfig {

    @Bean
    public DespesaService despesaService(DespesaRepositoryPort repositoryPort) {
        return new DespesaService(repositoryPort);
    }

    @Bean
    public Locale appLocale() {
        // Default application locale: Portuguese (Brazil)
        return Locale.forLanguageTag("pt-BR");
    }
}
