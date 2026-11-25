package com.kdt.reservation.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
