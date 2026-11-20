package com.raizesdonordeste.api.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Payload para cadastro de novos clientes com consentimento LGPD obrigatório")
public class RegisterRequestDto {

    @NotBlank(message = "O nome é obrigatório.")
    @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres.")
    @Schema(description = "Nome completo do cliente", example = "Maria Clara Silva")
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail informado é inválido.")
    @Size(max = 150, message = "O e-mail não pode exceder 150 caracteres.")
    @Schema(description = "E-mail de acesso e contato", example = "maria.clara@email.com")
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 6, max = 100, message = "A senha deve ter no mínimo 6 caracteres.")
    @Schema(description = "Senha de acesso", example = "SenhaSegura@123")
    private String senha;

    @NotNull(message = "O campo consentimentoLgpd é obrigatório.")
    @Schema(description = "Termo de consentimento e aceite para tratamento de dados pessoais conforme a LGPD (Lei 13.709/2018)", example = "true")
    private Boolean consentimentoLgpd;
}
