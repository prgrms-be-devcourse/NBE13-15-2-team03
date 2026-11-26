package com.kdt.reservation.point;

import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.user.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "point_history")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class PointHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = true)
    private Reservation reservation;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PointType pointType;
    @Column(nullable = false)
    private int amount;
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;


    public PointHistory(Reservation reservation, PointType pointType, int amount) {
        this.user = reservation.getUser();
        this.reservation = reservation;
        this.pointType = pointType;
        this.amount = amount;
    }
}
