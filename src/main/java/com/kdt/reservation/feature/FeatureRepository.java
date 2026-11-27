package com.kdt.reservation.feature;

import com.kdt.reservation.common.Feature;
import com.kdt.reservation.common.FeatureTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureRepository extends JpaRepository<Feature, Long> {

    // 전체 편의시설을 id 순서로 조회한다. (명세 6.4: 조회할 때마다 순서가 같도록 id 오름차순)
    List<Feature> findAllByOrderByIdAsc();

    // 대상(숙소용/객실용)이 일치하는 편의시설만 id 순서로 조회한다.
    List<Feature> findByFeatureTargetOrderByIdAsc(FeatureTarget featureTarget);
}
