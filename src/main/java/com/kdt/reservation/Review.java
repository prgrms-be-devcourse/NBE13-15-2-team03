package com.kdt.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id",unique = true,nullable = false)
    private Reservation reservation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id",nullable = false)
    private Accommodation accommodation;
    @Column(nullable = false)
    private int rating;
    @Column(length = 100)
    private String title;
    @Column(columnDefinition = "text")
    private String body;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;
}
