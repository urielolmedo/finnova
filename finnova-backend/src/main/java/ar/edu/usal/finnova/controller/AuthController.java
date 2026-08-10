package ar.edu.usal.finnova.controller;

import ar.edu.usal.finnova.dto.AuthResponse;
import ar.edu.usal.finnova.dto.LoginRequest;
import ar.edu.usal.finnova.dto.RegistroRequest;
import ar.edu.usal.finnova.model.Usuario;
import ar.edu.usal.finnova.repository.UsuarioRepository;
import ar.edu.usal.finnova.security.JwtService;
import ar.edu.usal.finnova.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ar.edu.usal.finnova.dto.RecuperarPasswordRequest;
import ar.edu.usal.finnova.dto.ResetearPasswordRequest;
import ar.edu.usal.finnova.model.PasswordResetToken;
import ar.edu.usal.finnova.repository.PasswordResetTokenRepository;
import ar.edu.usal.finnova.security.EmailService;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    // CU-001: Registrar nuevo usuario
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setActivo(true);

        usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, usuario.getEmail(), usuario.getNombre()));
    }

    // CU-002: Iniciar sesion
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail());
        String token = jwtService.generateToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, usuario.getEmail(), usuario.getNombre()));
    }

    // CU-003: Cerrar sesion
    @PostMapping("/logout")
    public ResponseEntity<?> logout(jakarta.servlet.http.HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            tokenBlacklistService.blacklist(token);
        }
        return ResponseEntity.ok("Sesión cerrada correctamente");
    }

    // CU-004 (parte 1): Solicitar recuperacion de contraseña
    @PostMapping("/recuperar-password")
    public ResponseEntity<?> recuperarPassword(@RequestBody RecuperarPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Nota de seguridad: respondemos OK aunque el email no exista,
        // para no revelar que emails estan registrados en el sistema.
        if (usuario == null) {
            return ResponseEntity.ok("Si el email existe, vas a recibir un correo con instrucciones.");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                null, token, usuario, LocalDateTime.now().plusMinutes(15), false
        );
        passwordResetTokenRepository.save(resetToken);

        emailService.enviarMailRecuperacion(usuario.getEmail(), token);

        return ResponseEntity.ok("Si el email existe, vas a recibir un correo con instrucciones.");
    }

    // CU-004 (parte 2): Resetear la contraseña con el token recibido por mail
    @PostMapping("/resetear-password")
    public ResponseEntity<?> resetearPassword(@RequestBody ResetearPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElse(null);

        if (resetToken == null || resetToken.isUsado() || resetToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPasswordHash(passwordEncoder.encode(request.getNuevaPassword()));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);

        return ResponseEntity.ok("Contraseña actualizada correctamente");
    }
}