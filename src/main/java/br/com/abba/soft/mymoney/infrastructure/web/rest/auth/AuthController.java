package br.com.abba.soft.mymoney.infrastructure.web.rest.auth;

import br.com.abba.soft.mymoney.domain.model.Usuario;
import br.com.abba.soft.mymoney.infrastructure.persistence.mapper.UsuarioMapper;
import br.com.abba.soft.mymoney.infrastructure.persistence.repository.UsuarioRepository;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.UsuarioRegisterRequest;
import br.com.abba.soft.mymoney.infrastructure.web.rest.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Operações de autenticação e cadastro de usuários")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar um novo usuário", description = "Cria um novo usuário com nome e credenciais informados")
    @ApiResponse(responseCode = "201", description = "Usuário registrado", content = @Content(schema = @Schema(implementation = UsuarioResponse.class)))
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody UsuarioRegisterRequest request) {
        // Check duplicates by email or telefone (minimal guard)
        if (request.getEmail() != null && usuarioRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email ja cadastrado");
        }
        if (request.getTelefone() != null && usuarioRepository.findByTelefone(request.getTelefone()).isPresent()) {
            throw new IllegalArgumentException("Telefone ja cadastrado");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setTelefone(request.getTelefone());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.validateForRegister();

        var saved = usuarioRepository.save(UsuarioMapper.toDocument(usuario));
        var response = new UsuarioResponse(saved.getId(), saved.getNome(), saved.getEmail(), saved.getTelefone());
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId())).body(response);
    }
}
