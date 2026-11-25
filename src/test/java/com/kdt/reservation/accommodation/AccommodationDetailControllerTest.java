package com.kdt.reservation.accommodation;

import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse;
import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse.FeatureInfo;
import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse.HostInfo;
import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse.RoomInfo;
import com.kdt.reservation.common.FeatureCategory;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.config.JacksonConfig;
import com.kdt.reservation.review.ReviewService;
import com.kdt.reservation.room.Grade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 숙소 상세 조회(GET /api/accommodations/{id}, 명세 6.2) 컨트롤러 테스트. (이슈 #27)
 *
 * [테스트를 만드는 이유]
 * 브라우저로 직접 확인하는 것은 그때 한 번뿐이다. 테스트로 적어 두면 버튼 한 번으로 언제든 다시 확인할 수 있어서,
 * 나중에 누가 코드를 고치다가 응답 모양을 깨뜨려도 바로 알 수 있다.
 *
 * [숙소 리뷰 목록 테스트(AccommodationControllerTest)와 파일을 나눈 이유]
 * 같은 컨트롤러를 검사하지만 담당자가 다르다. 한 파일을 두 사람이 같이 고치면 합칠 때 충돌이 나기 쉬워서 따로 두었다.
 *
 * [@WebMvcTest(AccommodationController.class) 의 뜻]
 * 앱 전체를 켜지 않고 "이 컨트롤러 하나"만 띄워서 검사한다. DB 도 연결하지 않아 빠르다.
 * [@AutoConfigureMockMvc(addFilters = false) 의 뜻]
 * 로그인 검사(시큐리티 필터)를 끈다. 여기서는 컨트롤러의 동작만 보려는 것이기 때문이다.
 * [@Import(JacksonConfig.class) 의 뜻]
 * 날짜를 "2026-11-20T10:00:00" 형식으로 내보내는 팀 공통 설정을 이 테스트에도 적용한다.
 */
@WebMvcTest(AccommodationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JacksonConfig.class)
class AccommodationDetailControllerTest {

    // 주소의 {id} 자리에는 테스트마다 다른 값을 넣는다.
    private static final String URL = "/api/accommodations/{id}";

    // 가짜 HTTP 요청을 보내는 도구. 서버를 실제로 켜지 않고 컨트롤러를 호출한다.
    @Autowired
    private MockMvc mockMvc;

    // 진짜 서비스 대신 넣는 가짜(Mock). given(...) 으로 "이렇게 부르면 이렇게 답해라"를 정한다.
    // 이유: 이 테스트는 컨트롤러만 검사한다. 서비스와 DB 까지 진짜로 돌리면 느리고, 실패 원인을 찾기 어렵다.
    @MockitoBean
    private AccommodationService accommodationService;

    // 컨트롤러가 ReviewService 도 필요로 하기 때문에(숙소 리뷰 목록 API), 가짜를 넣어 줘야 컨트롤러가 만들어진다.
    @MockitoBean
    private ReviewService reviewService;

    // 명세 6.2 의 예시와 같은 값을 주면, 모든 항목이 명세의 이름과 형식으로 나가는지 확인한다.
    @Test
    @DisplayName("숙소 상세 조회 성공 시 200과 숙소 정보, 이미지, 편의시설, 평점, 객실 목록을 돌려준다")
    void getDetail_success() throws Exception {
        // given: 서비스가 아래 응답을 돌려준다고 정해 둔다.
        given(accommodationService.getDetail(12L)).willReturn(sampleResponse());

        // when & then: 요청을 보내고 응답의 각 항목을 검사한다.
        // jsonPath("$.data.name") 은 "응답 JSON 의 data 안의 name" 을 가리키는 표기다.
        mockMvc.perform(get(URL, 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                // 숙소 기본 정보
                .andExpect(jsonPath("$.data.accommodationId").value(12))
                .andExpect(jsonPath("$.data.host.hostId").value(3))
                .andExpect(jsonPath("$.data.host.nickname").value("바다사장"))
                .andExpect(jsonPath("$.data.name").value("해운대 오션뷰 호텔"))
                .andExpect(jsonPath("$.data.description").value("해변까지 걸어서 3분 거리입니다."))
                .andExpect(jsonPath("$.data.type").value("HOTEL"))
                .andExpect(jsonPath("$.data.region").value("BUSAN"))
                .andExpect(jsonPath("$.data.address").value("부산 해운대구 해운대해변로 100"))
                .andExpect(jsonPath("$.data.addressDetail").value("본관"))
                // 이미지: 넣은 순서 그대로 나가야 한다. (첫 번째가 대표 이미지)
                .andExpect(jsonPath("$.data.imageUrls", hasSize(2)))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("/images/2026/11/a1b2c3.jpg"))
                // 숙소 편의시설
                .andExpect(jsonPath("$.data.features", hasSize(1)))
                .andExpect(jsonPath("$.data.features[0].featureId").value(1))
                .andExpect(jsonPath("$.data.features[0].code").value("PARKING"))
                .andExpect(jsonPath("$.data.features[0].name").value("주차장"))
                .andExpect(jsonPath("$.data.features[0].category").value("FACILITY"))
                // 평점
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.reviewCount").value(37))
                // 객실
                .andExpect(jsonPath("$.data.rooms", hasSize(1)))
                .andExpect(jsonPath("$.data.rooms[0].roomId").value(101))
                .andExpect(jsonPath("$.data.rooms[0].name").value("디럭스 더블"))
                .andExpect(jsonPath("$.data.rooms[0].price").value(120000))
                .andExpect(jsonPath("$.data.rooms[0].grade").value("DELUXE"))
                .andExpect(jsonPath("$.data.rooms[0].standardCapacity").value(2))
                .andExpect(jsonPath("$.data.rooms[0].maxCapacity").value(3))
                .andExpect(jsonPath("$.data.rooms[0].extraPersonFee").value(20000))
                // 판매 여부의 이름이 명세대로 "isAvailable" 인지 확인한다.
                // 앞의 is 가 떨어진 "available" 이라는 이름으로는 나가면 안 된다.
                .andExpect(jsonPath("$.data.rooms[0].isAvailable").value(true))
                .andExpect(jsonPath("$.data.rooms[0].available").doesNotExist())
                .andExpect(jsonPath("$.data.rooms[0].imageUrls[0]").value("/images/2026/11/r101a.jpg"))
                .andExpect(jsonPath("$.data.rooms[0].features[0].featureId").value(21))
                .andExpect(jsonPath("$.data.rooms[0].features[0].code").value("BATHTUB"))
                // 날짜: 초 단위까지의 팀 공통 형식
                .andExpect(jsonPath("$.data.createdAt").value("2026-11-20T10:00:00"));
    }

    // 값이 비어 있을 때 항목이 사라지지 않고 null, 빈 배열, 0 으로 나가는지 확인한다.
    // 이유: 항목이 통째로 빠지면 이 API 를 쓰는 쪽(화면)에서 에러가 날 수 있다. 명세는 "없으면 빈 배열"이다.
    @Test
    @DisplayName("설명이 없으면 null, 이미지·편의시설·객실이 없으면 빈 배열, 리뷰가 없으면 0.0과 0을 돌려준다")
    void getDetail_emptyFields() throws Exception {
        AccommodationDetailResponse empty = new AccommodationDetailResponse(
                13L, new HostInfo(3L, "바다사장"), "빈 숙소", null, Type.PENSION,
                "GANGWON", "강원특별자치도 강릉시", "",
                List.of(), List.of(), 0.0, 0L, List.of(),
                LocalDateTime.of(2026, 11, 21, 9, 0, 0));
        given(accommodationService.getDetail(13L)).willReturn(empty);

        mockMvc.perform(get(URL, 13))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value(nullValue()))
                .andExpect(jsonPath("$.data.imageUrls").isArray())
                .andExpect(jsonPath("$.data.imageUrls", hasSize(0)))
                .andExpect(jsonPath("$.data.features", hasSize(0)))
                .andExpect(jsonPath("$.data.rooms", hasSize(0)))
                .andExpect(jsonPath("$.data.averageRating").value(0.0))
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    // 서비스가 던진 "숙소 없음" 예외가 공통 에러 형식의 404 로 바뀌는지 확인한다.
    @Test
    @DisplayName("숙소가 없거나 삭제되었으면 404 ACCOMMODATION_NOT_FOUND를 돌려준다")
    void getDetail_notFound() throws Exception {
        // given: 서비스가 예외를 던진다고 정해 둔다. (실제 서비스도 숙소가 없으면 이 예외를 던진다)
        given(accommodationService.getDetail(99999L))
                .willThrow(new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND));

        mockMvc.perform(get(URL, 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ACCOMMODATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("숙소를 찾을 수 없습니다."));
    }

    // 숫자가 아닌 숙소 번호는 컨트롤러에 들어오기 전에 막히는지 확인한다.
    @Test
    @DisplayName("숙소 ID가 숫자가 아니면 400 INVALID_INPUT을 돌려주고 서비스를 부르지 않는다")
    void getDetail_invalidId() throws Exception {
        mockMvc.perform(get(URL, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));

        // 잘못된 요청이므로 서비스는 한 번도 불리면 안 된다.
        verify(accommodationService, never()).getDetail(any());
    }

    // 명세 6.2 의 예시와 같은 값으로 응답을 만든다. (지역 값만 DB 에 실제로 들어 있는 영문 코드를 썼다)
    private static AccommodationDetailResponse sampleResponse() {
        RoomInfo room = new RoomInfo(
                101L, "디럭스 더블", "오션뷰, 더블 침대 1개", 120000, Grade.DELUXE,
                2, 3, 20000, true,
                List.of("/images/2026/11/r101a.jpg"),
                List.of(new FeatureInfo(21L, "BATHTUB", "욕조", FeatureCategory.FACILITY)));

        return new AccommodationDetailResponse(
                12L, new HostInfo(3L, "바다사장"), "해운대 오션뷰 호텔", "해변까지 걸어서 3분 거리입니다.", Type.HOTEL,
                "BUSAN", "부산 해운대구 해운대해변로 100", "본관",
                List.of("/images/2026/11/a1b2c3.jpg", "/images/2026/11/d4e5f6.jpg"),
                List.of(new FeatureInfo(1L, "PARKING", "주차장", FeatureCategory.FACILITY)),
                4.5, 37L,
                List.of(room),
                LocalDateTime.of(2026, 11, 20, 10, 0, 0));
    }
}
