package com.kdt.reservation.accommodation;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.PageResponse;
import com.kdt.reservation.config.JacksonConfig;
import com.kdt.reservation.config.PageableConfig;
import com.kdt.reservation.review.ReviewService;
import com.kdt.reservation.review.ReviewSort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// AccommodationController 테스트 (숙소 리뷰 목록, api-spec 12.2)
@WebMvcTest(AccommodationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, PageableConfig.class})
class AccommodationControllerTest {

    private static final String URL = "/api/accommodations/{id}/reviews";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private AccommodationService accommodationService;

    // 명세 예시와 같은 응답을 주면 모든 필드가 명세의 이름과 형식으로 나가는지 확인
    @Test
    @DisplayName("리뷰 목록 조회 성공 시 200과 평균 평점, 리뷰 수, 리뷰 페이지를 돌려준다")
    void getAccommodationReviews_success() throws Exception {
        AccommodationReviewResponse review = new AccommodationReviewResponse(
                77L, 5, "바다가 바로 보여요", "방이 깨끗하고 직원분들이 친절했습니다.",
                List.of("/images/2026/12/8f3a1c.jpg"),
                new AccommodationReviewResponse.Author(7L, "여행자"),
                "디럭스 더블", LocalDateTime.of(2026, 12, 27, 10, 0, 0));
        given(reviewService.getAccommodationReviews(eq(1L), any(), any()))
                .willReturn(response(4.5, 37, List.of(review), 2, true));

        mockMvc.perform(get(URL, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.reviewCount").value(37))
                .andExpect(jsonPath("$.data.reviews.content", hasSize(1)))
                .andExpect(jsonPath("$.data.reviews.content[0].reviewId").value(77))
                .andExpect(jsonPath("$.data.reviews.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.reviews.content[0].title").value("바다가 바로 보여요"))
                .andExpect(jsonPath("$.data.reviews.content[0].body").value("방이 깨끗하고 직원분들이 친절했습니다."))
                .andExpect(jsonPath("$.data.reviews.content[0].imageUrls[0]").value("/images/2026/12/8f3a1c.jpg"))
                .andExpect(jsonPath("$.data.reviews.content[0].author.userId").value(7))
                .andExpect(jsonPath("$.data.reviews.content[0].author.nickname").value("여행자"))
                .andExpect(jsonPath("$.data.reviews.content[0].roomName").value("디럭스 더블"))
                .andExpect(jsonPath("$.data.reviews.content[0].createdAt").value("2026-12-27T10:00:00"))
                .andExpect(jsonPath("$.data.reviews.page").value(0))
                .andExpect(jsonPath("$.data.reviews.size").value(20))
                .andExpect(jsonPath("$.data.reviews.totalElements").value(37))
                .andExpect(jsonPath("$.data.reviews.totalPages").value(2))
                .andExpect(jsonPath("$.data.reviews.hasNext").value(true));
    }

    // 쿼리 파라미터를 하나도 안 보냈을 때 명세의 기본값이 서비스로 전달되는지 확인
    @Test
    @DisplayName("파라미터가 없으면 sort LATEST, page 0, size 20으로 조회한다")
    void getAccommodationReviews_defaultParams() throws Exception {
        mockMvc.perform(get(URL, 1))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getAccommodationReviews(eq(1L), eq(ReviewSort.LATEST), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    // 명세가 허용하는 정렬 값 3개가 각각 enum으로 변환되어 서비스로 전달되는지 확인
    @ParameterizedTest
    @EnumSource(ReviewSort.class)
    @DisplayName("sort 값을 그대로 서비스에 전달한다")
    void getAccommodationReviews_sort(ReviewSort sort) throws Exception {
        mockMvc.perform(get(URL, 1).param("sort", sort.name()))
                .andExpect(status().isOk());

        verify(reviewService).getAccommodationReviews(eq(1L), eq(sort), any());
    }

    // 요청한 page, size가 서비스로 전달되는지 확인
    @Test
    @DisplayName("page와 size 값을 그대로 서비스에 전달한다")
    void getAccommodationReviews_paging() throws Exception {
        mockMvc.perform(get(URL, 1).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewService).getAccommodationReviews(eq(1L), any(), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    // 리뷰가 없는 숙소는 에러가 아니라 0.0, 0, 빈 배열로 응답하는지 확인
    @Test
    @DisplayName("리뷰가 없으면 평균 평점 0.0, 리뷰 수 0, 빈 목록을 돌려준다")
    void getAccommodationReviews_noReviews() throws Exception {
        given(reviewService.getAccommodationReviews(eq(1L), any(), any()))
                .willReturn(response(0.0, 0, List.of(), 0, false));

        mockMvc.perform(get(URL, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(0.0))
                .andExpect(jsonPath("$.data.reviewCount").value(0))
                .andExpect(jsonPath("$.data.reviews.content", hasSize(0)))
                .andExpect(jsonPath("$.data.reviews.totalElements").value(0))
                .andExpect(jsonPath("$.data.reviews.hasNext").value(false));
    }

    // 선택 값이 비어 있을 때 필드가 빠지지 않고 null, 빈 배열로 나가는지 확인
    @Test
    @DisplayName("제목과 본문이 없으면 null, 이미지가 없으면 빈 배열로 돌려준다")
    void getAccommodationReviews_nullableFields() throws Exception {
        AccommodationReviewResponse review = new AccommodationReviewResponse(
                78L, 3, null, null, List.of(),
                new AccommodationReviewResponse.Author(8L, "뚜벅이"),
                "스탠다드 트윈", LocalDateTime.of(2026, 12, 28, 9, 30, 0));
        given(reviewService.getAccommodationReviews(eq(1L), any(), any()))
                .willReturn(response(3.0, 1, List.of(review), 1, false));

        mockMvc.perform(get(URL, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviews.content[0].title").value(nullValue()))
                .andExpect(jsonPath("$.data.reviews.content[0].body").value(nullValue()))
                .andExpect(jsonPath("$.data.reviews.content[0].imageUrls").isArray())
                .andExpect(jsonPath("$.data.reviews.content[0].imageUrls", hasSize(0)));
    }

    // 허용되지 않은 정렬 값은 컨트롤러 진입 전에 막히는지 확인
    @Test
    @DisplayName("sort 값이 올바르지 않으면 400 INVALID_INPUT을 돌려주고 서비스를 부르지 않는다")
    void getAccommodationReviews_invalidSort() throws Exception {
        mockMvc.perform(get(URL, 1).param("sort", "OLDEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"));

        verify(reviewService, never()).getAccommodationReviews(any(), any(), any());
    }

    // 숫자가 아닌 숙소 ID는 타입 변환 단계에서 막히는지 확인
    @Test
    @DisplayName("숙소 ID가 숫자가 아니면 400 INVALID_INPUT을 돌려주고 서비스를 부르지 않는다")
    void getAccommodationReviews_invalidId() throws Exception {
        mockMvc.perform(get(URL, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));

        verify(reviewService, never()).getAccommodationReviews(any(), any(), any());
    }

    // 서비스가 던진 숙소 없음 예외가 공통 에러 포맷의 404로 변환되는지 확인
    @Test
    @DisplayName("숙소가 없으면 404 ACCOMMODATION_NOT_FOUND를 돌려준다")
    void getAccommodationReviews_accommodationNotFound() throws Exception {
        given(reviewService.getAccommodationReviews(eq(99999L), any(), any()))
                .willThrow(new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND));

        mockMvc.perform(get(URL, 99999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ACCOMMODATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("숙소를 찾을 수 없습니다."));
    }

    // 서비스가 돌려줄 응답을 만든다 (page 0, size 20 고정)
    private static AccommodationReviewListResponse response(
            double averageRating, long reviewCount, List<AccommodationReviewResponse> content,
            int totalPages, boolean hasNext
    ) {
        return new AccommodationReviewListResponse(
                averageRating, reviewCount,
                new PageResponse<>(content, 0, 20, reviewCount, totalPages, hasNext));
    }
}
