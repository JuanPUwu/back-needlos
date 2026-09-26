package com.needlos.security.auth;

import com.needlos.security.auth.dto.AuthDtos.LoginRequest;
import com.needlos.security.auth.dto.AuthDtos.RefreshRequest;
import com.needlos.security.auth.dto.AuthDtos.RegistrarSastreriaRequest;
import com.needlos.security.auth.dto.AuthDtos.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticacion")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Registra una nueva sastreria (plan DEMO) y su usuario dueno")
    @PostMapping("/registrar-sastreria")
    public ResponseEntity<TokenResponse> registrar(@Valid @RequestBody RegistrarSastreriaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarSastreria(req));
    }

    @Operation(summary = "Inicia sesion y devuelve access + refresh token")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @Operation(summary = "Rota el refresh token y devuelve uno nuevo")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @Operation(summary = "Cierra sesion revocando el refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
