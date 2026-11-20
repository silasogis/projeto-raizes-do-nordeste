package com.raizesdonordeste.application.services;

import com.raizesdonordeste.domain.entities.AuditoriaLog;
import com.raizesdonordeste.domain.entities.Usuario;
import com.raizesdonordeste.infrastructure.repositories.AuditoriaLogRepository;
import com.raizesdonordeste.infrastructure.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaService {

    private final AuditoriaLogRepository auditoriaLogRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void registrarLog(String acao, String entidade, Long entidadeId, String detalhes) {
        String ator = "SISTEMA";
        Usuario usuario = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            ator = authentication.getName();
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(ator);
            if (usuarioOpt.isPresent()) {
                usuario = usuarioOpt.get();
            }
        }

        AuditoriaLog logEntry = AuditoriaLog.builder()
                .usuario(usuario)
                .ator(ator)
                .acao(acao)
                .entidade(entidade)
                .entidadeId(entidadeId)
                .detalhes(detalhes)
                .build();

        auditoriaLogRepository.save(logEntry);
        log.info("Log de auditoria registrado: [Ação: {}, Entidade: {}, ID: {}, Ator: {}]", acao, entidade, entidadeId, ator);
    }
}
