package com.raizesdonordeste.application.services;

import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.RegisterRequestDto;
import com.raizesdonordeste.api.dtos.response.AuthResponseDto;
import com.raizesdonordeste.api.dtos.response.UsuarioResponseDto;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
import com.raizesdonordeste.domain.exceptions.BusinessValidationException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import com.raizesdonordeste.infrastructure.repositories.UsuarioRepository;
import com.raizesdonordeste.infrastructure.security.CustomUserDetails;
import com.raizesdonordeste.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public UsuarioResponseDto register(RegisterRequestDto request) {
        log.info("Iniciando cadastro de novo cliente: {}", request.getEmail());

        if (Boolean.FALSE.equals(request.getConsentimentoLgpd())) {
            throw new BusinessValidationException(
                    "O consentimento para tratamento de dados pessoais conforme a LGPD é obrigatório para realização do cadastro.",
                    Collections.singletonList("consentimentoLgpd: deve ser verdadeiro (true) para cumprimento da LGPD")
            );
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (usuarioRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessConflictException(
                    "EMAIL_JA_CADASTRADO",
                    "Já existe um usuário cadastrado com o e-mail informado: " + normalizedEmail
            );
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome().trim())
                .email(normalizedEmail)
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .perfil(Perfil.CLIENTE)
                .pontosFidelidade(0)
                .consentimentoLgpd(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Cliente cadastrado com sucesso. ID: {}", salvo.getId());

        return UsuarioResponseDto.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        String normalizedEmail = request.getEmail().toLowerCase().trim();
        log.info("Processando tentativa de login para o e-mail: {}", normalizedEmail);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getSenha())
            );
        } catch (AuthenticationException ex) {
            log.warn("Falha de autenticação para o usuário {}: {}", normalizedEmail, ex.getMessage());
            throw new BadCredentialsException("E-mail ou senha incorretos.");
        }

        Usuario usuario = usuarioRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + normalizedEmail));

        CustomUserDetails userDetails = new CustomUserDetails(usuario);
        String token = jwtService.generateToken(userDetails);

        log.info("Login efetuado com sucesso para o usuário ID: {}, Perfil: {}", usuario.getId(), usuario.getPerfil());

        return AuthResponseDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .usuario(UsuarioResponseDto.fromEntity(usuario))
                .build();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDto getPerfilAutenticado(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado no banco: " + email));
        return UsuarioResponseDto.fromEntity(usuario);
    }
}
