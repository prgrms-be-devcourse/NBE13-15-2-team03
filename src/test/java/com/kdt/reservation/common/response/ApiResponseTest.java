package com.kdt.reservation.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("ok(data)는 HTTP 200과 본문 status 200, data를 담는다")
    void okWithData() {
        ResponseEntity<ApiResponse<String>> response = ApiResponse.ok("hello");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ApiResponse<>(200, "hello"));
    }

    @Test
    @DisplayName("ok()는 HTTP 200과 data null을 담는다")
    void okWithoutData() {
        ResponseEntity<ApiResponse<Void>> response = ApiResponse.ok();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(new ApiResponse<>(200, null));
    }

    @Test
    @DisplayName("created(data)는 HTTP 201과 본문 status 201을 담는다")
    void created() {
        ResponseEntity<ApiResponse<Long>> response = ApiResponse.created(12L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new ApiResponse<>(201, 12L));
    }
}