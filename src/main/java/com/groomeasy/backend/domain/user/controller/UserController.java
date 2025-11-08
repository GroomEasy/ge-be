package com.groomeasy.backend.domain.user.controller;

import com.groomeasy.backend.domain.user.dto.request.SignUpRequestDTO;
import com.groomeasy.backend.domain.user.dto.response.SignUpResponseDTO;
import com.groomeasy.backend.domain.user.repository.UserRepository;
import com.groomeasy.backend.domain.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponseDTO> signUp(@Valid @RequestBody SignUpRequestDTO request) {
        SignUpResponseDTO response = userService.signUp(request);

        // TODO: API Response 생성 후 적용
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
