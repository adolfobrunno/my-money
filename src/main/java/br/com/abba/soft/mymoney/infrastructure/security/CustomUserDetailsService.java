package br.com.abba.soft.mymoney.infrastructure.security;

import br.com.abba.soft.mymoney.infrastructure.persistence.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var userDoc = usuarioRepository.findByEmailOrTelefone(username, username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        return new CustomUserDetails(userDoc.getId(), username, userDoc.getSenhaHash());
    }
}
