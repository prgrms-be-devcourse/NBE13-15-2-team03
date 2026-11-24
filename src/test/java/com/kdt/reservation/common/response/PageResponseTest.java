package com.kdt.reservation.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    @DisplayName("135건을 20개씩 나눈 첫 페이지는 totalPages 7, hasNext true다")
    void firstPage() {
        Page<Integer> page = new PageImpl<>(numbers(20), PageRequest.of(0, 20), 135);

        PageResponse<Integer> response = PageResponse.from(page);

        assertThat(response.content()).hasSize(20);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(135);
        assertThat(response.totalPages()).isEqualTo(7);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("마지막 페이지는 hasNext false다")
    void lastPage() {
        Page<Integer> page = new PageImpl<>(numbers(15), PageRequest.of(6, 20), 135);

        PageResponse<Integer> response = PageResponse.from(page);

        assertThat(response.content()).hasSize(15);
        assertThat(response.page()).isEqualTo(6);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("결과가 없으면 빈 content와 totalPages 0을 돌려준다")
    void emptyPage() {
        Page<Integer> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        PageResponse<Integer> response = PageResponse.from(page);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("mapper를 주면 content를 변환하고 페이지 정보는 유지한다")
    void mapContent() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2, 3), PageRequest.of(1, 3), 10);

        PageResponse<String> response = PageResponse.from(page, n -> "no-" + n);

        assertThat(response.content()).containsExactly("no-1", "no-2", "no-3");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.totalElements()).isEqualTo(10);
        assertThat(response.totalPages()).isEqualTo(4);
        assertThat(response.hasNext()).isTrue();
    }

    private List<Integer> numbers(int count) {
        return IntStream.range(0, count).boxed().toList();
    }
}
