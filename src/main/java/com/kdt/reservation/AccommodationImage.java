package com.kdt.reservation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accommodation_image")
@NoArgsConstructor
@Getter
@Setter
public class AccommodationImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id",nullable = false)
    private Accommodation accommodation;
    //JPA가 String을 기본적으로 varchar(255)로 취급하기 때문에 길이를 알려주는 설정이 필요합니다
    @Column(columnDefinition = "TEXT",nullable = false)
    private String url;
}
