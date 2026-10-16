package com.needlos.tipoprenda;

import com.needlos.common.tenant.TenantContext;
import com.needlos.tenant.SastreriaRegistrada;
import com.needlos.tipoprenda.domain.TipoPrenda;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Catalogo base con el que arranca toda sastreria nueva (decision 2026-10-16): tipos comunes sin
 * precio, que el SASTRE_ADMIN puede renombrar, desactivar o ampliar; asi puede crear pedidos desde
 * el primer dia.
 *
 * <p>Se crea DESPUES del commit del registro y en una transaccion propia: Hibernate fija la
 * sastreria (@TenantId) al abrir la sesion, y durante el registro la sesion ya esta abierta sin
 * sastreria. Por eso primero se pone la sastreria nueva en el contexto y luego se abre la
 * transaccion.
 */
@Component
public class CatalogoInicial {

    private static final Logger log = LoggerFactory.getLogger(CatalogoInicial.class);

    static final List<String> TIPOS_BASE =
            List.of("Camisa", "Pantalón", "Saco", "Vestido", "Chaqueta");

    private final TipoPrendaRepository repo;
    private final TransactionTemplate transaccionNueva;

    public CatalogoInicial(TipoPrendaRepository repo, PlatformTransactionManager txManager) {
        this.repo = repo;
        this.transaccionNueva = new TransactionTemplate(txManager);
        this.transaccionNueva.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void crear(SastreriaRegistrada evento) {
        UUID tenantPrevio = TenantContext.getTenantId();
        UUID usuarioPrevio = TenantContext.getUsuarioId();
        TenantContext.set(evento.tenantId(), usuarioPrevio);
        try {
            transaccionNueva.executeWithoutResult(
                    estado -> {
                        for (String nombre : TIPOS_BASE) {
                            TipoPrenda tipo = new TipoPrenda();
                            tipo.setNombre(nombre);
                            tipo.setActivo(true);
                            repo.save(tipo);
                        }
                    });
        } catch (RuntimeException ex) {
            // No tumba el registro (ya confirmado): el admin puede crear su catalogo a mano.
            log.error(
                    "No se pudo crear el catalogo base de la sastreria {}", evento.tenantId(), ex);
        } finally {
            if (TenantContext.SIN_TENANT.equals(tenantPrevio)) {
                TenantContext.clear();
            } else {
                TenantContext.set(tenantPrevio, usuarioPrevio);
            }
        }
    }
}
