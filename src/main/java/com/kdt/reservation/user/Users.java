package com.kdt.reservation.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 외부에서 new Users 생성자 못하게
@EntityListeners(AuditingEntityListener.class) // 생성날짜, 수정날짜 자동으로 해주는것
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id",unique = true,length = 50,nullable = false)
    private String login_id;

    @Column(length = 255,nullable = false)
    private String password;

    @Column(length = 50,nullable = false)
    private String name;

    @Column(length = 20,nullable = false)
    private String nickname;

    @Column(unique = true,length = 20,nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "birth_date",nullable = false)
    private LocalDate birth_date;

    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updated_at;

    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;

    private Users(String loginId, String password, String name, String nickname,
                  String phone, LocalDate birthDate, Role role) {
        this.login_id = loginId;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.phone = phone;
        this.birth_date = birthDate;
        this.role = role;
    }

    // 일반 회원가입용. password 는 반드시 암호화된 값을 넘겨야 합니다
    public static Users createLocal(String loginId, String encodedPassword, String name, String nickname,
                                    String phone, LocalDate birthDate, Role role) {
        return new Users(loginId, encodedPassword, name, nickname, phone, birthDate, role);
    }

    public boolean isDeleted() {
        return deleted_at != null;
    }


    /**
     * 내 정보 수정 (api-spec 5.2)
     * null 인 값은 "수정 안 함"이라 건너뛴다. 보낸 것만 바뀐다.
     * setter 대신 이 메서드 하나로만 바꾸게 해서, 수정 내용 한눈에 보이게
     */
    public void updateProfile(String name, String nickname, String phone, LocalDate birthDate) {
        if (name != null) {
            this.name = name;
        }
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (birthDate != null) {
            this.birth_date = birthDate;
        }
    }

    /**
     * 비밀번호 변경 (api-spec 5.3)
     * @param encodedPassword 반드시 passwordEncoder.encode() 한 값을 넘길 것
     */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
