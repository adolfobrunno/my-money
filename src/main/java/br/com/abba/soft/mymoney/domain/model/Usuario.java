package br.com.abba.soft.mymoney.domain.model;

import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Usuario {
    private String id;
    private String nome;
    private String email;
    private String telefone;
    private String senhaHash;

    public Usuario() {}

    public Usuario(String id, String nome, String email, String telefone, String senhaHash) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.telefone = telefone;
        this.senhaHash = senhaHash;
    }

    public void validateForRegister() {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("Nome obrigatorio");
        if ((email == null || email.isBlank()) && (telefone == null || telefone.isBlank())) {
            throw new IllegalArgumentException("Email ou telefone obrigatorio");
        }
        if (senhaHash == null || senhaHash.isBlank()) throw new IllegalArgumentException("Senha obrigatoria");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
