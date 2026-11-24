package com.kdt.reservation.auth;

import com.kdt.reservation.auth.dto.CreateUserRequest;
import com.kdt.reservation.auth.dto.CreateUserResponse;
import com.kdt.reservation.user.Role;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CreateUserResponse signUp(CreateUserRequest request){
        if (request.role() == Role.ADMIN){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ADMIN 으로는 가입할 수 없습니다");
        }

        if (userRepository.existsByLoginId(request.loginId())) {
            // TODO 공통 예외(DUPLICATE_LOGIN_ID, 409)가 나오면 교체
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (userRepository.existsByPhone(request.phone())) {
            // TODO 공통 예외(DUPLICATE_PHONE, 409)가 나오면 교체
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 전화번호입니다.");
        }

        //  비밀번호 암호화 후 회원 객체 만들기
        Users user = Users.createLocal(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.nickname(),
                request.phone(),
                request.birthDate(),
                request.role());

        Users saved = userRepository.save(user);

        return new CreateUserResponse(saved.getId());
    }
}
