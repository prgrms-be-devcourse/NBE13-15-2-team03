package com.kdt.reservation.feature;

import com.kdt.reservation.common.Feature;
import com.kdt.reservation.common.FeatureTarget;
import com.kdt.reservation.feature.dto.FeatureResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureService {

    private final FeatureRepository featureRepository;

    // 편의시설 목록 조회 (명세 6.4, 이슈 #37)
    // target 이 없으면 전체를, 있으면 그 대상용 항목만 돌려준다. 항목 수가 적어(31개) 페이징하지 않는다.
    @Transactional(readOnly = true)
    public List<FeatureResponse> getFeatures(FeatureTarget target) {
        List<Feature> features = (target == null)
                ? featureRepository.findAllByOrderByIdAsc()
                : featureRepository.findByFeatureTargetOrderByIdAsc(target);

        return features.stream()
                .map(FeatureResponse::from)
                .toList();
    }
}
