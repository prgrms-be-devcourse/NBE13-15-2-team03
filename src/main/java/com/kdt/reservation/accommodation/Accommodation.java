package com.kdt.reservation.accommodation;

import com.kdt.reservation.user.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Accommodation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id",nullable = false)
    private Users user;
    @Column(length = 50,nullable = false)
    private String name;
    @Column(length = 255)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;
    @Column(length = 20,nullable = false)
    private String region;
    @Column(length = 255,nullable = false)
    private String address;
    @Column(length = 255,nullable = false)
    private String address_detail;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;
}
