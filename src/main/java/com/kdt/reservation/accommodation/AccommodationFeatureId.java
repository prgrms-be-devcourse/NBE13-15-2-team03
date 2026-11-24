package com.kdt.reservation.accommodation;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class AccommodationFeatureId implements Serializable {
    @Column(name = "accommodation_id")
    private Long accommodation_id;
    @Column(name = "feature_id")
    private Long feature_id;

    public AccommodationFeatureId(Long feature_id, Long accommodation_id) {
        this.feature_id = feature_id;
        this.accommodation_id = accommodation_id;
    }
}
