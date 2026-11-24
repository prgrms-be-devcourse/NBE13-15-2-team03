package com.kdt.reservation.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("success(data)는 HTTP 200과 본문 status 200, data를 담는다")
    void successWithData() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ApiResponse.Body<>(200, "hello"));
    }

    @Test
    @DisplayName("success()는 HTTP 200과 data null을 담는다")
    void successWithoutData() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ApiResponse.Body<>(200, null));
    }

    @Test
    @DisplayName("created(data)는 HTTP 201과 본문 status 201을 담는다")
    void created() {
        ApiResponse<Long> response = ApiResponse.created(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new ApiResponse.Body<>(201, 12L));
    }

    @Test
    @DisplayName("page(page)는 HTTP 200과 PageResponse를 담는다")
    void page() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 135);

        ApiResponse<PageResponse<String>> response = ApiResponse.page(page);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ApiResponse.Body<>(200, PageResponse.from(page)));
    }

    @Test
    @DisplayName("page(page, mapper)는 content를 변환해 담는다")
    void pageWithMapper() {
        Page<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 5);

        ApiResponse<PageResponse<String>> response = ApiResponse.page(page, n -> "no-" + n);

        assertThat(response.getBody().status()).isEqualTo(200);
        assertThat(response.getBody().data().content()).containsExactly("no-1", "no-2");
        assertThat(response.getBody().data().totalPages()).isEqualTo(3);
        assertThat(response.getBody().data().hasNext()).isTrue();
    }
}
