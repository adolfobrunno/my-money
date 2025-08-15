package br.com.abba.soft.mymoney.infrastructure.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados do usuário retornados após cadastro")
public class UsuarioResponse {
    @Schema(description = "Identificador do usuário", example = "66bdb3a4e4b0f95c1c1f9a22")
    private String id;
    @Schema(description = "Nome do usuário", example = "João Silva")
    private String nome;
    @Schema(description = "Email do usuário", example = "joao@email.com")
    private String email;
    @Schema(description = "Telefone do usuário", example = "+55 11 99999-9999")
    private String telefone;

    public UsuarioResponse(String id, String nome, String email, String telefone) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
    }

    public String getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
}
