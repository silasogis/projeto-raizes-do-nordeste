package com.raizesdonordeste.api.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Resposta de autenticação bem-sucedida contendo o token JWT")
public class AuthResponseDto {

    @Schema(description = "Token de acesso JWT stateless", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Tipo do token emitido", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Tempo restante de validade do token em segundos", example = "28800")
    private Long expiresIn;

    @Schema(description = "Dados resumidos do usuário autenticado")
    private UsuarioResponseDto usuario;
}
