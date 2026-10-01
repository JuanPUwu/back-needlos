package com.needlos.ordenes;

import com.needlos.clientes.ClienteService;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.common.tenant.TenantContext;
import com.needlos.ordenes.dto.OrdenDtos.CrearOrdenRequest;
import com.needlos.ordenes.dto.OrdenDtos.PrendaRequest;
import com.needlos.soporte.IntegracionTest;
import com.needlos.tipoprenda.TipoPrendaRepository;
import com.needlos.tipoprenda.domain.TipoPrenda;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Reglas §10.4: el consecutivo nunca se repite, ni con pedidos simultaneos. */
class ConsecutivoConcurrenteTest extends IntegracionTest {

    private static final int PEDIDOS = 20;

    @Autowired
    private OrdenService ordenService;
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private TipoPrendaRepository tipoPrendaRepo;

    @Test
    void pedidosSimultaneos_recibenNumerosUnicosYCorrelativosPorSastreria() throws Exception {
        UUID tenant = datos.crearSastreria();
        CrearOrdenRequest pedido = prepararPedido(tenant);

        List<Long> numeros = crearEnParalelo(tenant, pedido);

        assertThat(numeros).containsExactlyInAnyOrderElementsOf(
                LongStream.rangeClosed(1, PEDIDOS).boxed().toList());

        UUID otraSastreria = datos.crearSastreria();
        // El pedido se prepara ANTES de entrar al conTenant de abajo: conTenant limpia el
        // contexto del hilo en su "finally", y aqui anidarlo borraria el contexto del externo.
        CrearOrdenRequest pedidoOtra = prepararPedido(otraSastreria);
        long primeroDeOtra = conTenant(otraSastreria, () -> ordenService.crear(pedidoOtra).numero());
        assertThat(primeroDeOtra).isEqualTo(1);
    }

    private List<Long> crearEnParalelo(UUID tenant, CrearOrdenRequest pedido) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PEDIDOS);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Long>> futuros = new ArrayList<>();
            for (int i = 0; i < PEDIDOS; i++) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return conTenant(tenant, () -> ordenService.crear(pedido).numero());
                }));
            }
            salida.countDown();
            List<Long> numeros = new ArrayList<>();
            for (Future<Long> futuro : futuros) {
                numeros.add(futuro.get());
            }
            return numeros;
        } finally {
            pool.shutdownNow();
        }
    }

    private CrearOrdenRequest prepararPedido(UUID tenant) {
        return conTenant(tenant, () -> {
            UUID clienteId = clienteService.crear(new CrearClienteRequest("Ana", "Ruiz", null)).id();
            TipoPrenda tipo = new TipoPrenda();
            tipo.setNombre("Camisa");
            UUID tipoId = tipoPrendaRepo.save(tipo).getId();
            return new CrearOrdenRequest(clienteId, LocalDate.now().plusDays(5), BigDecimal.ZERO,
                    List.of(new PrendaRequest(tipoId, null, 1, "Ajuste de mangas", new BigDecimal("15000"))));
        });
    }

    private static <T> T conTenant(UUID tenant, Callable<T> accion) {
        TenantContext.set(tenant, null);
        try {
            return accion.call();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
