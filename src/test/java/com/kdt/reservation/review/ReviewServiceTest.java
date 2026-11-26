package com.kdt.reservation.review;

import com.kdt.reservation.accommodation.AccommodationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// ReviewService 테스트 (숙소별 리뷰 수·평균 평점 조회)
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AccommodationRepository accommodationRepository;

    @InjectMocks
    private ReviewService reviewService;

    // 리포지토리가 준 집계 결과가 숙소 id를 key로 하는 Map으로 옮겨지는지 확인
    @Test
    @DisplayName("숙소 id 목록으로 조회하면 숙소별 리뷰 수와 평균 평점을 Map으로 돌려준다")
    void getReviewSummaries_success() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(1L, 2L)))
                .willReturn(List.of(new ReviewSummary(1L, 5, 3.4), new ReviewSummary(2L, 2, 4.5)));

        Map<Long, ReviewSummary> summaries = reviewService.getReviewSummaries(List.of(1L, 2L));

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(1L)).isEqualTo(new ReviewSummary(1L, 5, 3.4));
        assertThat(summaries.get(2L)).isEqualTo(new ReviewSummary(2L, 2, 4.5));
    }

    // 집계 결과에 없는 숙소(리뷰 0건)도 빠지지 않고 0 / 0.0으로 채워지는지 확인
    @Test
    @DisplayName("리뷰가 없는 숙소는 리뷰 수 0, 평균 평점 0.0으로 포함한다")
    void getReviewSummaries_fillsMissingAccommodations() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(1L, 2L, 3L)))
                .willReturn(List.of(new ReviewSummary(2L, 4, 4.0)));

        Map<Long, ReviewSummary> summaries = reviewService.getReviewSummaries(List.of(1L, 2L, 3L));

        assertThat(summaries).containsOnlyKeys(1L, 2L, 3L);
        assertThat(summaries.get(1L)).isEqualTo(new ReviewSummary(1L, 0, 0.0));
        assertThat(summaries.get(2L)).isEqualTo(new ReviewSummary(2L, 4, 4.0));
        assertThat(summaries.get(3L)).isEqualTo(new ReviewSummary(3L, 0, 0.0));
    }

    // 숙소가 여러 개여도 집계 쿼리는 한 번만 나가는지 확인 (N+1 방지)
    @Test
    @DisplayName("숙소가 여러 개여도 리포지토리를 한 번만 호출한다")
    void getReviewSummaries_singleQuery() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(1L, 2L, 3L)))
                .willReturn(List.of());

        reviewService.getReviewSummaries(List.of(1L, 2L, 3L));

        verify(reviewRepository, times(1)).findSummariesByAccommodationIds(any());
    }

    // 빈 목록이면 쿼리를 보내지 않고 빈 Map을 돌려주는지 확인 (in () 오류 방지)
    @Test
    @DisplayName("숙소 id 목록이 비어 있으면 빈 Map을 돌려주고 리포지토리를 부르지 않는다")
    void getReviewSummaries_emptyIds() {
        Map<Long, ReviewSummary> summaries = reviewService.getReviewSummaries(List.of());

        assertThat(summaries).isEmpty();
        verify(reviewRepository, never()).findSummariesByAccommodationIds(any());
    }

    // 같은 숙소 id가 중복으로 들어와도 결과가 하나로 정리되는지 확인
    @Test
    @DisplayName("숙소 id가 중복되어도 숙소당 하나의 결과만 돌려준다")
    void getReviewSummaries_duplicateIds() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(1L, 1L, 2L)))
                .willReturn(List.of(new ReviewSummary(1L, 3, 5.0)));

        Map<Long, ReviewSummary> summaries = reviewService.getReviewSummaries(List.of(1L, 1L, 2L));

        assertThat(summaries).containsOnlyKeys(1L, 2L);
        assertThat(summaries.get(1L)).isEqualTo(new ReviewSummary(1L, 3, 5.0));
    }

    // 단건 조회가 해당 숙소의 집계 결과를 그대로 돌려주는지 확인
    @Test
    @DisplayName("숙소 하나를 조회하면 그 숙소의 리뷰 수와 평균 평점을 돌려준다")
    void getReviewSummary_success() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(35L)))
                .willReturn(List.of(new ReviewSummary(35L, 5, 3.4)));

        ReviewSummary summary = reviewService.getReviewSummary(35L);

        assertThat(summary).isEqualTo(new ReviewSummary(35L, 5, 3.4));
    }

    // 단건 조회에서 리뷰가 없을 때 null이 아니라 0 / 0.0을 돌려주는지 확인
    @Test
    @DisplayName("숙소 하나를 조회했는데 리뷰가 없으면 리뷰 수 0, 평균 평점 0.0을 돌려준다")
    void getReviewSummary_noReviews() {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(2L)))
                .willReturn(List.of());

        ReviewSummary summary = reviewService.getReviewSummary(2L);

        assertThat(summary).isEqualTo(new ReviewSummary(2L, 0, 0.0));
    }

    // DB가 준 평균이 소수 첫째 자리로 반올림되어 나가는지 확인
    @ParameterizedTest
    @CsvSource({
            "4.333333333333333, 4.3",
            "4.25, 4.3",
            "4.666666666666667, 4.7",
            "3.04, 3.0",
            "5.0, 5.0"
    })
    @DisplayName("평균 평점은 소수 첫째 자리로 반올림한다")
    void getReviewSummary_roundsAverage(double average, double expected) {
        given(reviewRepository.findSummariesByAccommodationIds(List.of(1L)))
                .willReturn(List.of(new ReviewSummary(1L, 3, average)));

        ReviewSummary summary = reviewService.getReviewSummary(1L);

        assertThat(summary.averageRating()).isEqualTo(expected);
    }
}