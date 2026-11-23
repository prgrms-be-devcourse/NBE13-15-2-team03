package com.kdt.reservation.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class Users {

    @Id
    @Setter(AccessLevel.NONE)
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
    private role role;
    @Column(name = "birth_date",nullable = false)
    private LocalDate birth_date;
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;


}
