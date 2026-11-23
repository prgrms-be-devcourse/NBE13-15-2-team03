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
@Table(name = "room")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id",nullable = false)
    private Accommodation accommodation;
    @Column(length = 50,nullable = false)
    private String name;
    @Column(length = 255)
    private String description;
    @Column(nullable = false)
    private int price;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Grade grade;
    @Column(nullable = false)
    private int standard_capacity;
    @Column(nullable = false)
    private int max_capacity;
    @Column(nullable = false)
    private int extra_person_fee=0;
    @Column(name = "is_available",nullable = false)
    private boolean available=true;
    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime created_at;
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;




}
