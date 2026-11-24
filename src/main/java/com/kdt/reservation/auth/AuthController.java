package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<CreateUserResponse> signup(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = authService.signUp(request);
        // TODO 공통 응답 포맷({ "status": 201, "data": ... })이 나오면 감싸기
        return ResponseEntity.status(HttpStatus.CREATED).body(response);  // 201 Created
    }
}
