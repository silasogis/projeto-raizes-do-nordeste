package com.raizesdonordeste.api.dtos.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.domain.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dados públicos do usuário (sem campos sensíveis como hash de senha)")
public class UsuarioResponseDto {

    @Schema(description = "Identificador único do usuário", example = "1")
    private Long id;

    @Schema(description = "Nome completo do usuário", example = "Maria Clara Silva")
    private String nome;

    @Schema(description = "E-mail cadastrado", example = "maria.clara@email.com")
    private String email;

    @Schema(description = "Perfil de acesso atribuído no sistema", example = "CLIENTE")
    private Perfil perfil;

    @Schema(description = "Saldo acumulado no programa de fidelidade", example = "0")
    private Integer pontosFidelidade;

    @Schema(description = "Registro de aceite do termo de consentimento LGPD", example = "true")
    private Boolean consentimentoLgpd;

    @Schema(description = "Data e hora de criação do cadastro", example = "2026-11-20T10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataCriacao;

    public static UsuarioResponseDto fromEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        return UsuarioResponseDto.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil())
                .pontosFidelidade(usuario.getPontosFidelidade())
                .consentimentoLgpd(usuario.getConsentimentoLgpd())
                .dataCriacao(usuario.getDataCriacao())
                .build();
    }
}
