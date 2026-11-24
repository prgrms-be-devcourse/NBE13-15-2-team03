package com.kdt.reservation.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "social_user",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_user_provider",
                columnNames = {"provider","provider_user_id"})
)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class SocialUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private Users user;
    @Column(length = 20,nullable = false)
    private String provider;
    @Column(length = 255,nullable = false)
    private String provider_user_id;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;

}
