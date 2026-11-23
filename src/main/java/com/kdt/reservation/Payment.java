package com.kdt.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id",nullable = false)
    private Reservation reservation;
    @Enumerated(EnumType.STRING)
    @Column(name="method",nullable = false)
    private PayMethod payMethod;
    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private PayStatus payStatus;
    @Column(nullable = false)
    private int amount;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @Column(name = "paid_at")
    private LocalDateTime paid_at;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelled_at;



}
