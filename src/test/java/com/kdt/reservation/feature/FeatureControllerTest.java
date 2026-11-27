package com.kdt.reservation.feature;

import com.kdt.reservation.common.FeatureCategory;
import com.kdt.reservation.common.FeatureTarget;
import com.kdt.reservation.feature.dto.FeatureResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// FeatureController 테스트 (편의시설 목록 조회, api-spec 6.4)
@WebMvcTest(FeatureController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeatureControllerTest {

    private static final String URL = "/api/features";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeatureService featureService;

    // 명세 예시와 같은 값을 주면 모든 항목이 명세의 이름으로 나가는지 확인
    @Test
    @DisplayName("target 없이 조회하면 200과 전체 편의시설 목록을 돌려준다")
    void getFeatures_all() throws Exception {
        given(featureService.getFeatures(null)).willReturn(List.of(
                new FeatureResponse(1L, "PARKING", "주차장", FeatureCategory.FACILITY, FeatureTarget.ACCOMMODATION),
                new FeatureResponse(21L, "BATHTUB", "욕조", FeatureCategory.FACILITY, FeatureTarget.ROOM)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].featureId").value(1))
                .andExpect(jsonPath("$.data[0].code").value("PARKING"))
                .andExpect(jsonPath("$.data[0].name").value("주차장"))
                .andExpect(jsonPath("$.data[0].category").value("FACILITY"))
                .andExpect(jsonPath("$.data[0].target").value("ACCOMMODATION"))
                .andExpect(jsonPath("$.data[1].featureId").value(21))
                .andExpect(jsonPath("$.data[1].target").value("ROOM"));
    }

    // 쿼리 파라미터 target 이 enum 으로 변환되어 서비스에 그대로 전달되는지 확인
    @Test
    @DisplayName("target 을 보내면 그 값으로 서비스를 호출한다")
    void getFeatures_byTarget() throws Exception {
        given(featureService.getFeatures(FeatureTarget.ROOM)).willReturn(List.of(
                new FeatureResponse(21L, "BATHTUB", "욕조", FeatureCategory.FACILITY, FeatureTarget.ROOM)));

        mockMvc.perform(get(URL).param("target", "ROOM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].code").value("BATHTUB"));

        verify(featureService).getFeatures(FeatureTarget.ROOM);
    }

    // 결과가 없을 때 에러가 아니라 빈 배열로 응답하는지 확인
    @Test
    @DisplayName("편의시설이 없으면 빈 배열을 돌려준다")
    void getFeatures_empty() throws Exception {
        given(featureService.getFeatures(null)).willReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // 허용되지 않은 target 값은 컨트롤러 진입 전에 막히는지 확인
    @Test
    @DisplayName("target 값이 올바르지 않으면 400 INVALID_INPUT을 돌려주고 서비스를 부르지 않는다")
    void getFeatures_invalidTarget() throws Exception {
        mockMvc.perform(get(URL).param("target", "HOTEL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("target"));

        verify(featureService, never()).getFeatures(any());
    }
}
