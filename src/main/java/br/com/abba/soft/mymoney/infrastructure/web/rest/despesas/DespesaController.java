package br.com.abba.soft.mymoney.infrastructure.web.rest.despesas;

import br.com.abba.soft.mymoney.application.DespesaService;
import br.com.abba.soft.mymoney.domain.model.Despesa;
import br.com.abba.soft.mymoney.infrastructure.security.SecurityUtils;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.DespesaRequest;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.DespesaResponse;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.RelatorioDespesasResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
@Tag(name = "Despesas", description = "Operações para gerenciar despesas do usuário autenticado")
public class DespesaController {

    private final DespesaService service;

    public DespesaController(DespesaService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Criar uma nova despesa", description = "Cria uma despesa vinculada ao usuário autenticado e retorna o recurso criado")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Despesa criada com sucesso", content = @Content(schema = @Schema(implementation = DespesaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autorizado", content = @Content)
    })
    public ResponseEntity<DespesaResponse> criar(@Valid @RequestBody DespesaRequest request) {
        Despesa despesa = DespesaDtoMapper.toDomain(request);
        // associate to authenticated user
        String userId = SecurityUtils.currentUserIdOrNull();
        if (userId == null) {
            throw new IllegalStateException("Usuario nao autenticado");
        }
        despesa.setUserId(userId);
        Despesa criada = service.criar(despesa);
        return ResponseEntity.created(URI.create("/api/despesas/" + criada.getId()))
                .body(DespesaDtoMapper.toResponse(criada));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar despesa", description = "Atualiza os dados de uma despesa existente pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Despesa atualizada", content = @Content(schema = @Schema(implementation = DespesaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Despesa não encontrada", content = @Content)
    })
    public ResponseEntity<DespesaResponse> atualizar(
            @Parameter(description = "ID da despesa", required = true)
            @PathVariable String id,
            @Valid @RequestBody DespesaRequest request) {
        Despesa despesa = DespesaDtoMapper.toDomain(request);
        Despesa atualizada = service.atualizar(id, despesa);
        return ResponseEntity.ok(DespesaDtoMapper.toResponse(atualizada));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar despesa por ID", description = "Retorna os dados de uma despesa pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Despesa encontrada", content = @Content(schema = @Schema(implementation = DespesaResponse.class))),
            @ApiResponse(responseCode = "404", description = "Despesa não encontrada", content = @Content)
    })
    public ResponseEntity<DespesaResponse> buscar(
            @Parameter(description = "ID da despesa", required = true)
            @PathVariable String id) {
        return ResponseEntity.ok(DespesaDtoMapper.toResponse(service.buscar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar despesas", description = "Lista as despesas do usuário autenticado com paginação")
    @ApiResponse(responseCode = "200", description = "Lista de despesas", content = @Content(schema = @Schema(implementation = DespesaResponse.class)))
    public ResponseEntity<List<DespesaResponse>> listar(
            @Parameter(description = "Número da página (0-based)") @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Parameter(description = "Tamanho da página") @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        List<DespesaResponse> all = service.listar().stream().map(DespesaDtoMapper::toResponse).toList();
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<DespesaResponse> pageItems = all.subList(from, to);
        return ResponseEntity.ok(pageItems);
    }

    @GetMapping("/relatorio")
    @Operation(summary = "Relatório de despesas por período", description = "Retorna resumo com total e itens dentro do período informado. Informe apenas as datas (dia) de início e fim.")
    @ApiResponse(responseCode = "200", description = "Relatório gerado", content = @Content(schema = @Schema(implementation = RelatorioDespesasResponse.class)))
    public ResponseEntity<RelatorioDespesasResponse> relatorio(
            @Parameter(description = "Data inicial (ISO YYYY-MM-DD)", required = true)
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate inicio,
            @Parameter(description = "Data final (ISO YYYY-MM-DD)", required = true)
            @RequestParam("fim") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fim
    ) {
        LocalDateTime start = inicio.atStartOfDay();
        LocalDateTime end = fim.plusDays(1).atStartOfDay().minusNanos(1);
        var despesas = service.listarPorPeriodo(start, end);
        List<DespesaResponse> itens = despesas.stream().map(DespesaDtoMapper::toResponse).toList();
        BigDecimal total = service.totalizarValor(despesas);
        RelatorioDespesasResponse resp = new RelatorioDespesasResponse(total, itens.size(), itens);
        // Totais por categoria
        java.util.Map<String, java.math.BigDecimal> porCat = new java.util.LinkedHashMap<>();
        for (var d : despesas) {
            String key = d.getCategoria() == null ? "OUTRAS" : d.getCategoria().name();
            porCat.merge(key, d.getValor(), java.math.BigDecimal::add);
        }
        resp.setTotalPorCategoria(porCat);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir despesa", description = "Exclui a despesa pelo seu identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Excluída com sucesso"),
            @ApiResponse(responseCode = "404", description = "Despesa não encontrada", content = @Content)
    })
    public void excluir(
            @Parameter(description = "ID da despesa", required = true)
            @PathVariable String id) {
        service.excluir(id);
    }
}
