package br.com.abba.soft.mymoney.infrastructure.web.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados para registrar um novo usuário")
public class UsuarioRegisterRequest {
    @NotBlank
    @Schema(description = "Nome completo do usuário", example = "João Silva")
    private String nome;
    @Schema(description = "Email do usuário (obrigatório se não houver telefone)", example = "joao@email.com")
    private String email; // can be null if telefone provided
    @Schema(description = "Telefone do usuário (obrigatório se não houver email)", example = "+55 11 99999-9999")
    private String telefone; // can be null if email provided
    @NotBlank
    @Schema(description = "Senha do usuário", example = "S3nh@F0rte!")
    private String senha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
