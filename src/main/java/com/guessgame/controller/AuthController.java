package com.guessgame.controller;

import com.guessgame.dto.LoginRequest;
import com.guessgame.dto.RegisterRequest;
import com.guessgame.entity.User;
import com.guessgame.repository.UserRepository;
import com.guessgame.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Xử lý đăng nhập người dùng.
     *
     * @param loginRequest Chứa thông tin đăng nhập của người dùng (username và password).
     * @return ResponseEntity chứa token JWT nếu đăng nhập thành công, hoặc thông báo lỗi nếu thất bại.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Thông tin đăng nhập không hợp lệ. Vui lòng kiểm tra lại tên người dùng và mật khẩu.");
        }

        Optional<User> optUser = userRepository.findByUsername(loginRequest.getUsername());
        User user = optUser.orElseThrow(() ->
                new UsernameNotFoundException("Tên người dùng không tìm thấy: " + loginRequest.getUsername()));

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok().body(Collections.singletonMap("token", token));
    }

    /**
     * Xử lý đăng ký người dùng mới.
     *
     * @param registerRequest Chứa thông tin đăng ký của người dùng (username, password, email).
     * @return ResponseEntity chứa thông báo thành công hoặc lỗi nếu đăng ký không thành công.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Tên người dùng đã tồn tại. Vui lòng chọn tên khác.");
        }
        User user = User.builder()
                .username(registerRequest.getUsername())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .role("USER") // Role mặc định, có thể thay đổi nếu cần
                .build();
        userRepository.save(user);
        return ResponseEntity.ok("Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.");
    }
}