package com.raizesdonordeste.api.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Credenciais para autenticação no sistema")
public class LoginRequestDto {

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    @Schema(description = "E-mail cadastrado do usuário", example = "admin@raizesdonordeste.com.br")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Schema(description = "Senha do usuário", example = "Senha@123")
    private String senha;
}
