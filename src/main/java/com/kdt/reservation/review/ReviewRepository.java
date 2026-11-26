package com.kdt.reservation.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query(
            value = """
                    select r from Review r
                    join fetch r.user
                    join fetch r.reservation res
                    join fetch res.room
                    where r.accommodation.id = :accommodationId
                      and r.deleted_at is null
                    """,
            countQuery = """
                    select count(r) from Review r
                    where r.accommodation.id = :accommodationId
                      and r.deleted_at is null
                    """
    )
    Page<Review> findByAccommodationId(@Param("accommodationId") Long accommodationId, Pageable pageable);

    @Query("""
            select avg(r.rating) from Review r
            where r.accommodation.id = :accommodationId
              and r.deleted_at is null
            """)
    Double findAverageRatingByAccommodationId(@Param("accommodationId") Long accommodationId);

    @Query("""
            select new com.kdt.reservation.review.ReviewSummary(r.accommodation.id, count(r), avg(r.rating))
            from Review r
            where r.accommodation.id in :accommodationIds
              and r.deleted_at is null
            group by r.accommodation.id
            """)
    List<ReviewSummary> findSummariesByAccommodationIds(@Param("accommodationIds") List<Long> accommodationIds);

    // reservation/ReservatoinService.java API-9.2-내 페이지조회 에 사용
    //r.reservatoin.id =:reservatoinId하면 단일 데이터 조회
    @Query("""
            SELECT r.reservation.id
            FROM Review r
            WHERE r.reservation.id  in :reservationIds
            AND r.deleted_at is null
            """)
    Set<Long> findByReservationId(@Param("reservationIds") Set<Long> reservationId);

}
