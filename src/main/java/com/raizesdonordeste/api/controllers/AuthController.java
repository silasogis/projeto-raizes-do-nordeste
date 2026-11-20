package com.raizesdonordeste.api.controllers;

import com.raizesdonordeste.api.dtos.request.LoginRequestDto;
import com.raizesdonordeste.api.dtos.request.RegisterRequestDto;
import com.raizesdonordeste.api.dtos.response.AuthResponseDto;
import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.api.dtos.response.UsuarioResponseDto;
import com.raizesdonordeste.application.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação & LGPD", description = "Endpoints públicos de auto-cadastro com consentimento LGPD e autenticação via JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Cadastrar novo cliente (com consentimento LGPD)",
            description = "Permite o auto-cadastro de novos clientes. Exige obrigatoriamente a aceitação expressa do termo de consentimento LGPD (consentimentoLgpd = true)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida ou parâmetros ausentes",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "409", description = "Conflito: e-mail já cadastrado no sistema",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "422", description = "Falha de validação dos campos ou consentimento LGPD recusado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<UsuarioResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        UsuarioResponseDto response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Autenticar usuário e emitir token JWT",
            description = "Valida as credenciais (e-mail e senha) e retorna o token JWT stateless para utilização no cabeçalho Authorization Bearer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso",
                    content = @Content(schema = @Schema(implementation = AuthResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Dados da requisição inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais incorretas ou usuário não autorizado",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "Consultar dados do usuário autenticado",
            description = "Retorna os dados públicos do usuário logado baseado no token JWT informado no cabeçalho Authorization.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado recuperados com sucesso",
                    content = @Content(schema = @Schema(implementation = UsuarioResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente, expirado ou inválido",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
    })
    public ResponseEntity<UsuarioResponseDto> me(@AuthenticationPrincipal UserDetails userDetails) {
        UsuarioResponseDto response = authService.getPerfilAutenticado(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Verificação de perfil administrativo (RBAC)",
            description = "Endpoint restrito ao perfil ADMIN para validação de controle de acesso baseado em funções. Retorna 403 Forbidden para outros perfis.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Acesso autorizado com perfil ADMIN"),
            @ApiResponse(responseCode = "401", description = "Não autenticado / token ausente"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: perfil sem permissão")
    })
    public ResponseEntity<Map<String, String>> adminCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "AUTORIZADO",
                "message", "Acesso concedido com sucesso para o perfil ADMIN."
        ));
    }
}
