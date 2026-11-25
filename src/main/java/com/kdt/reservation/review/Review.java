package com.kdt.reservation.review;

import com.kdt.reservation.user.Users;
import com.kdt.reservation.accommodation.Accommodation;
import com.kdt.reservation.reservation.Reservation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    @JoinColumn(name = "reservation_id", unique = true, nullable = false)
    private Reservation reservation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accommodation_id", nullable = false)
    private Accommodation accommodation;
    @Column(nullable = false)
    private int rating;
    @Column(length = 100)
    private String title;
    @Column(columnDefinition = "text")
    private String body;
    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime created_at;
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updated_at;
    @Column(name = "deleted_at")
    private LocalDateTime deleted_at;

    // @BatchSize: 리뷰 목록 조회 시 N+1 문제 방지
    // 리뷰마다 이미지를 개별 조회하지 않고, size개 리뷰의 이미지를 IN 쿼리 한 번으로 조회해 매핑한다.
    // 예) size = 100, 리뷰 100건 조회 → 이미지 조회 쿼리 1번
    // review하나에 review_image가 여러 행 달리는 구조기 때문에
    // 기존 fetch join 방식을 사용하면 페이징이 불가능함.
    // 전략은 여전히 LAZY이지만, 호출하면 묶어서 조회
    @OneToMany(mappedBy = "review")
    @BatchSize(size = 100)
    @OrderBy("id asc")
    private List<ReviewImage> images = new ArrayList<>();
}
