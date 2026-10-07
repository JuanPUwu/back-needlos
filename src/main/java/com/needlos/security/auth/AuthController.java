package com.needlos.security.auth;

import com.needlos.security.auth.AuthService.ResultadoLogin;
import com.needlos.security.auth.AuthService.SesionEmitida;
import com.needlos.security.auth.dto.AuthDtos.GoogleLoginRequest;
import com.needlos.security.auth.dto.AuthDtos.LoginRequest;
import com.needlos.security.auth.dto.AuthDtos.LoginResponse;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaGoogleRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistroPendienteResponse;
import com.needlos.security.auth.dto.AuthDtos.SeleccionarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.SesionResponse;
import com.needlos.security.sesion.ContextoCliente;
import com.needlos.security.verificacion.dto.VerificacionDtos.ReenviarCodigoRequest;
import com.needlos.security.verificacion.dto.VerificacionDtos.VerificarCorreoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints publicos de autenticacion. El refresh token viaja SOLO en la cookie HttpOnly {@value
 * CookieRefresh#NOMBRE}; el cuerpo lleva el access token, que el cliente guarda unicamente en
 * memoria.
 */
@Tag(name = "Autenticacion")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieRefresh cookieRefresh;

    public AuthController(AuthService authService, CookieRefresh cookieRefresh) {
        this.authService = authService;
        this.cookieRefresh = cookieRefresh;
    }

    @Operation(
            summary = "Inicia sesión con correo y contraseña",
            description =
                    "Si la cuenta tiene varias sastrerias devuelve requiereSeleccion=true y un preauthToken.")
    @ApiResponse(responseCode = "200", description = "Sesión iniciada o selección pendiente")
    @ApiResponse(responseCode = "401", description = "CREDENCIALES_INVALIDAS")
    @ApiResponse(responseCode = "403", description = "SIN_ACCESO_SASTRERIA")
    @ApiResponse(
            responseCode = "429",
            description = "DEMASIADAS_SOLICITUDES (por IP) o CUENTA_BLOQUEADA_TEMPORALMENTE")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return conCookie(authService.login(req, ContextoCliente.desde(request)));
    }

    @Operation(summary = "Inicia sesión con Google (solo cuentas ya existentes)")
    @ApiResponse(responseCode = "401", description = "CREDENCIALES_INVALIDAS")
    @ApiResponse(responseCode = "403", description = "SIN_ACCESO_SASTRERIA")
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> google(
            @Valid @RequestBody GoogleLoginRequest req, HttpServletRequest request) {
        return conCookie(authService.loginGoogle(req.idToken(), ContextoCliente.desde(request)));
    }

    @Operation(summary = "Elige la sastrería cuando la cuenta tiene acceso a varias")
    @ApiResponse(responseCode = "401", description = "SESION_INVALIDA (preauthToken vencido)")
    @ApiResponse(responseCode = "403", description = "SIN_ACCESO_SASTRERIA")
    @PostMapping("/seleccionar-sastreria")
    public ResponseEntity<SesionResponse> seleccionar(
            @Valid @RequestBody SeleccionarSastreriaRequest req, HttpServletRequest request) {
        return conCookie(
                HttpStatus.OK,
                authService.seleccionarSastreria(req, ContextoCliente.desde(request)));
    }

    @Operation(
            summary = "Registra una sastrería nueva (plan DEMO) y su cuenta dueno (SASTRE_ADMIN)",
            description =
                    "Solo se pide el nombre de la sastrería; el identificador interno se genera solo. "
                            + "No entra todavia: hay que confirmar el código enviado al correo (ver /verificar-correo).")
    @ApiResponse(responseCode = "201", description = "Sastrería creada; falta verificar el correo")
    @ApiResponse(responseCode = "409", description = "CORREO_EN_USO")
    @PostMapping("/registrar-sastreria")
    public ResponseEntity<RegistroPendienteResponse> registrar(
            @Valid @RequestBody RegistrarSastreriaRequest req, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registrarSastreria(req, ContextoCliente.desde(request)));
    }

    @Operation(summary = "Registra una sastrería nueva usando la cuenta de Google (SASTRE_ADMIN)")
    @ApiResponse(responseCode = "201", description = "Sastrería creada y sesión iniciada")
    @ApiResponse(responseCode = "409", description = "CORREO_EN_USO")
    @PostMapping("/registrar-sastreria-google")
    public ResponseEntity<SesionResponse> registrarGoogle(
            @Valid @RequestBody RegistrarSastreriaGoogleRequest req, HttpServletRequest request) {
        return conCookie(
                HttpStatus.CREATED,
                authService.registrarSastreriaGoogle(req, ContextoCliente.desde(request)));
    }

    @Operation(
            summary = "Renueva la sesión con la cookie del refresh (la rota)",
            description =
                    "Se llama al cargar la app y cuando el access token vence. Exige cabecera Origin permitida.")
    @ApiResponse(responseCode = "401", description = "SESION_INVALIDA")
    @ApiResponse(responseCode = "403", description = "ORIGEN_NO_PERMITIDO o SIN_ACCESO_SASTRERIA")
    @PostMapping("/refresh")
    public ResponseEntity<SesionResponse> refresh(
            @CookieValue(name = CookieRefresh.NOMBRE, required = false) String refreshToken,
            HttpServletRequest request) {
        return conCookie(
                HttpStatus.OK, authService.renovar(refreshToken, ContextoCliente.desde(request)));
    }

    @Operation(
            summary = "Confirma el código de 6 dígitos del registro con correo y contrasena",
            description = "Si el código es correcto, entra directo con una sesion nueva.")
    @ApiResponse(responseCode = "200", description = "Correo verificado; sesión iniciada")
    @ApiResponse(responseCode = "400", description = "VALIDACION o CODIGO_VERIFICACION_INVALIDO")
    @PostMapping("/verificar-correo")
    public ResponseEntity<SesionResponse> verificarCorreo(
            @Valid @RequestBody VerificarCorreoRequest req, HttpServletRequest request) {
        return conCookie(
                HttpStatus.OK,
                authService.verificarCorreo(
                        req.email(), req.codigo(), ContextoCliente.desde(request)));
    }

    @Operation(
            summary = "Reenvía el código de verificación de correo",
            description = "Responde siempre 202, exista o no la cuenta, o ya este verificada.")
    @ApiResponse(responseCode = "202", description = "Solicitud recibida")
    @ApiResponse(responseCode = "429", description = "DEMASIADAS_SOLICITUDES")
    @PostMapping("/reenviar-codigo")
    public ResponseEntity<Void> reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequest req) {
        authService.reenviarCodigoVerificacion(req.email());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Cierra la sesión: la revoca en el servidor y borra la cookie")
    @ApiResponse(responseCode = "204", description = "Sesión cerrada")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = CookieRefresh.NOMBRE, required = false) String refreshToken) {
        authService.cerrarSesion(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieRefresh.borrar().toString())
                .build();
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ResponseEntity<LoginResponse> conCookie(ResultadoLogin resultado) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (resultado.refreshToken() != null) {
            respuesta.header(
                    HttpHeaders.SET_COOKIE,
                    cookieRefresh.crear(resultado.refreshToken()).toString());
        }
        return respuesta.body(resultado.respuesta());
    }

    private ResponseEntity<SesionResponse> conCookie(HttpStatus status, SesionEmitida emitida) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(status);
        if (emitida.refreshToken() != null) {
            respuesta.header(
                    HttpHeaders.SET_COOKIE, cookieRefresh.crear(emitida.refreshToken()).toString());
        }
        return respuesta.body(emitida.respuesta());
    }
}
