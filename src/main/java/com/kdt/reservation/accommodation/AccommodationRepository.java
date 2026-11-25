package com.kdt.reservation.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    @Query("""
            select count(a) > 0 from Accommodation a
            where a.id = :id
              and a.deleted_at is null
            """)
    boolean existsActiveById(@Param("id") Long id);

}
