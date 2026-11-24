package com.kdt.reservation.room;

import com.kdt.reservation.common.Feature;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="room_feature",
        indexes = @Index(name = "idx_feature_room",
        columnList = "feature_id,room_id"))
@Getter
@NoArgsConstructor
public class RoomFeature {
    @EmbeddedId
    private RoomFeatureId id;

    @MapsId("room_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id",nullable = false)
    private Room room;

    @MapsId("feature_id")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id",nullable = false)
    private Feature feature;

    public RoomFeature(RoomFeatureId id, Room room, Feature feature) {
        this.id = id;
        this.room = room;
        this.feature = feature;
    }
}
