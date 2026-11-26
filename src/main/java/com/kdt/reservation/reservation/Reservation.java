package com.kdt.reservation.reservation;

import com.kdt.reservation.room.Room;
import com.kdt.reservation.user.Users;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id",nullable = false)
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    private Room room;
    @Column(name = "guest_count",nullable = false)
    private int guest_count;
    @Column(name = "total_price",nullable = false)
    private int total_price;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    @Column(name = "check_in_date",nullable = false)
    private LocalDate check_in_date;
    @Column(name = "check_out_date",nullable = false)
    private LocalDate check_out_date;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelled_at;

    public Reservation(Users user, Room room, int guest_count, int total_price, Status status,
                       LocalDate check_in_date, LocalDate check_out_date, LocalDateTime created_at) {
        this.user = user;
        this.room = room;
        this.guest_count = guest_count;
        this.total_price = total_price;
        this.status = status;
        this.check_in_date = check_in_date;
        this.check_out_date = check_out_date;
        this.created_at = created_at;
    }
}
