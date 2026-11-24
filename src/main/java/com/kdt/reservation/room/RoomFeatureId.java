package com.kdt.reservation.room;

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
public class RoomFeatureId implements Serializable {
    @Column(name = "room_id")
    private Long room_id;
    @Column(name = "feature_id")
    private Long feature_id;

    public RoomFeatureId(Long feature_id, Long room_id) {
        this.feature_id = feature_id;
        this.room_id = room_id;
    }
}
