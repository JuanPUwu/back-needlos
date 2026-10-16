package com.needlos.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.needlos.clientes.ClienteService;
import com.needlos.clientes.dto.ClienteDtos.CrearClienteRequest;
import com.needlos.common.tenant.TenantContext;
import com.needlos.pedidos.dto.PedidoDtos.CrearPedidoRequest;
import com.needlos.pedidos.dto.PedidoDtos.PrendaRequest;
import com.needlos.soporte.IntegracionTest;
import com.needlos.tipoprenda.TipoPrendaRepository;
import com.needlos.tipoprenda.domain.TipoPrenda;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Reglas §10.4: el consecutivo de la sastreria nunca se repite ni deja huecos, ni con pedidos
 * simultaneos; el numero global tampoco se repite.
 */
class ConsecutivoConcurrenteTest extends IntegracionTest {

    private static final int PEDIDOS = 20;

    @Autowired private PedidoService pedidoService;
    @Autowired private ClienteService clienteService;
    @Autowired private TipoPrendaRepository tipoPrendaRepo;

    @Test
    void pedidosSimultaneos_recibenNumerosUnicosYCorrelativosPorSastreria() throws Exception {
        UUID tenant = datos.crearSastreria();
        CrearPedidoRequest pedido = prepararPedido(tenant);

        List<Long> numeros = crearEnParalelo(tenant, pedido);

        assertThat(numeros)
                .containsExactlyInAnyOrderElementsOf(
                        LongStream.rangeClosed(1, PEDIDOS).boxed().toList());

        UUID otraSastreria = datos.crearSastreria();
        // El pedido se prepara ANTES de entrar al conTenant de abajo: conTenant limpia el
        // contexto del hilo en su "finally", y aqui anidarlo borraria el contexto del externo.
        CrearPedidoRequest pedidoOtra = prepararPedido(otraSastreria);
        long primeroDeOtra =
                conTenant(
                        otraSastreria,
                        () -> pedidoService.crear(pedidoOtra, UUID.randomUUID()).numeroLocal());
        assertThat(primeroDeOtra).isEqualTo(1);
    }

    private List<Long> crearEnParalelo(UUID tenant, CrearPedidoRequest pedido) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PEDIDOS);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            List<Future<Long>> futuros = new ArrayList<>();
            for (int i = 0; i < PEDIDOS; i++) {
                futuros.add(
                        pool.submit(
                                () -> {
                                    salida.await();
                                    return conTenant(
                                            tenant,
                                            () ->
                                                    pedidoService
                                                            .crear(pedido, UUID.randomUUID())
                                                            .numeroLocal());
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

    private CrearPedidoRequest prepararPedido(UUID tenant) {
        return conTenant(
                tenant,
                () -> {
                    UUID clienteId =
                            clienteService
                                    .crear(new CrearClienteRequest("Ana", "Ruiz", "3001234567"))
                                    .id();
                    TipoPrenda tipo = new TipoPrenda();
                    tipo.setNombre("Camisa");
                    UUID tipoId = tipoPrendaRepo.save(tipo).getId();
                    return new CrearPedidoRequest(
                            clienteId,
                            null,
                            LocalDate.now().plusDays(5),
                            BigDecimal.ZERO,
                            List.of(
                                    new PrendaRequest(
                                            null,
                                            tipoId,
                                            null,
                                            1,
                                            "Ajuste de mangas",
                                            new BigDecimal("15000"))),
                            null);
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
