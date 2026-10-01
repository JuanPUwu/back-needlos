package com.needlos.security.cuenta;

import com.needlos.common.exception.CodigoError;
import com.needlos.common.exception.NoAutenticadoException;
import com.needlos.common.exception.ReglaNegocioException;
import com.needlos.security.cuenta.dto.CuentaDtos.CambiarContrasenaRequest;
import com.needlos.security.sesion.SesionService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepo;
    private final PasswordEncoder passwordEncoder;
    private final SesionService sesionService;
    private final ApplicationEventPublisher eventos;

    public CuentaService(CuentaRepository cuentaRepo, PasswordEncoder passwordEncoder,
                         SesionService sesionService, ApplicationEventPublisher eventos) {
        this.cuentaRepo = cuentaRepo;
        this.passwordEncoder = passwordEncoder;
        this.sesionService = sesionService;
        this.eventos = eventos;
    }

    /**
     * Cambia la contrasena y cierra las demas sesiones de la cuenta (si alguien
     * mas la conocia, pierde el acceso). La sesion actual sigue abierta.
     */
    @Transactional
    public void cambiarContrasena(UUID cuentaId, UUID sesionActual, CambiarContrasenaRequest req) {
        Cuenta cuenta = cuentaRepo.findById(cuentaId)
                .orElseThrow(() -> new NoAutenticadoException(CodigoError.SESION_INVALIDA, "Tu sesion ya no es valida."));

        if (cuenta.getPasswordHash() == null) {
            throw new ReglaNegocioException(CodigoError.CUENTA_SIN_CONTRASENA,
                    "Tu cuenta inicia sesion con Google y no tiene contrasena.");
        }
        if (!passwordEncoder.matches(req.contrasenaActual(), cuenta.getPasswordHash())) {
            throw new ReglaNegocioException(CodigoError.CONTRASENA_ACTUAL_INCORRECTA,
                    "La contrasena actual no es correcta.");
        }

        cuenta.setPasswordHash(passwordEncoder.encode(req.contrasenaNueva()));
        sesionService.cerrarOtras(cuentaId, sesionActual);
        eventos.publishEvent(new ContrasenaCambiada(cuenta.getEmail(), cuenta.getNombre()));
    }
}
