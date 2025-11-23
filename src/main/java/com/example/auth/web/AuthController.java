package com.example.auth.web;

import com.example.auth.domain.AuthResponse;
import com.example.auth.facade.AuthFacade;
import com.example.auth.security.JwtProperties;
import com.example.auth.web.dto.AuthRequestLogin;
import com.example.auth.web.dto.AuthRequestRegister;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthFacade facade;

  public AuthController(JwtProperties jwtProps,
                        BCryptPasswordEncoder encoder,
                        com.example.auth.SupabaseProperties supabaseProps) {
    this.facade = new AuthFacade(jwtProps, encoder, supabaseProps);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequestRegister req) {
    var res = facade.register(req);
    return ResponseEntity.status(201).body(res);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequestLogin req) {
    var res = facade.login(req);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/users")
  public ResponseEntity<String> listUsers(@RequestParam(name = "page", defaultValue = "1") int page,
                                          @RequestParam(name = "perPage", defaultValue = "50") int perPage) {
    var result = facade.listUsers(page, perPage);
    return ResponseEntity.status(result.getStatus())
      .header("Content-Type", "application/json")
      .body(result.getBody());
  }
}
