package com.raizesdonordeste.api.handlers;

import com.raizesdonordeste.api.dtos.response.ErrorResponseDto;
import com.raizesdonordeste.domain.exceptions.BusinessConflictException;
import com.raizesdonordeste.domain.exceptions.BusinessValidationException;
import com.raizesdonordeste.domain.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 422 - Erros de Bean Validation em @RequestBody
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ErrorResponseDto response = buildError(
                "VALIDACAO_FALHOU",
                "Um ou mais campos contêm erros de validação.",
                details,
                request
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // 422 - Violações de Regras de Validação de Domínio
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessValidation(
            BusinessValidationException ex, HttpServletRequest request) {

        ErrorResponseDto response = buildError(
                "REGRA_NEGOCIO_INVALIDA",
                ex.getMessage(),
                ex.getDetails(),
                request
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // 400 - Violações de Constraint em Parâmetros
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDto> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        List<String> details = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation ->
                details.add(violation.getPropertyPath() + ": " + violation.getMessage()));

        ErrorResponseDto response = buildError(
                "PARAMETRO_INVALIDO",
                "Parâmetro de requisição com formato ou valor inválido.",
                details,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 400 - JSON Malformado ou Valores Inválidos para Enums/Tipos
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        String message = "Corpo da requisição ausente ou malformado. Verifique os tipos de dados e valores aceitos.";
        List<String> details = Collections.singletonList(ex.getMostSpecificCause().getMessage());

        ErrorResponseDto response = buildError(
                "REQUISICAO_INVALIDA",
                message,
                details,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 400 - Parâmetro Obrigatório Ausente
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        String message = String.format("O parâmetro obrigatório '%s' não foi informado.", ex.getParameterName());
        List<String> details = Collections.singletonList("Tipo esperado: " + ex.getParameterType());

        ErrorResponseDto response = buildError(
                "PARAMETRO_OBRIGATORIO_AUSENTE",
                message,
                details,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 400 - Tipo de Parâmetro Incompatível
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        String message = String.format("O parâmetro '%s' recebeu valor de tipo incompatível.", ex.getName());
        Class<?> requiredType = ex.getRequiredType();
        String typeName = requiredType != null ? requiredType.getSimpleName() : "desconhecido";
        List<String> details = Collections.singletonList("Valor recebido: '" + ex.getValue() + "'. Tipo esperado: " + typeName);

        ErrorResponseDto response = buildError(
                "TIPO_PARAMETRO_INVALIDO",
                message,
                details,
                request
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 404 - Recurso Não Encontrado
    @ExceptionHandler({ResourceNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFound(
            Exception ex, HttpServletRequest request) {

        ErrorResponseDto response = buildError(
                "RECURSO_NAO_ENCONTRADO",
                ex.getMessage(),
                Collections.emptyList(),
                request
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 409 - Conflito de Regra de Negócio (ex: Estoque Insuficiente, Idempotência duplicada)
    @ExceptionHandler(BusinessConflictException.class)
    public ResponseEntity<ErrorResponseDto> handleBusinessConflict(
            BusinessConflictException ex, HttpServletRequest request) {

        ErrorResponseDto response = buildError(
                ex.getErrorCode(),
                ex.getMessage(),
                Collections.emptyList(),
                request
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // 401 - Não Autorizado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        ErrorResponseDto response = buildError(
                "NAO_AUTORIZADO",
                "Autenticação necessária para acessar este recurso.",
                Collections.singletonList(ex.getMessage()),
                request
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // 403 - Acesso Negado / Proibido
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        ErrorResponseDto response = buildError(
                "ACESSO_NEGADO",
                "Seu perfil não possui permissão para executar esta operação.",
                Collections.emptyList(),
                request
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // 500 - Erro Interno Não Tratado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Erro interno não tratado ao processar requisição: {}", request.getRequestURI(), ex);

        ErrorResponseDto response = buildError(
                "ERRO_INTERNO",
                "Ocorreu um erro interno inesperado no servidor. Tente novamente mais tarde.",
                Collections.singletonList(ex.getClass().getSimpleName() + ": " + ex.getMessage()),
                request
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private ErrorResponseDto buildError(
            String error, String message, List<String> details, HttpServletRequest request) {

        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        return ErrorResponseDto.builder()
                .error(error)
                .message(message)
                .details(details != null ? details : Collections.emptyList())
                .timestamp(Instant.now())
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();
    }
}
