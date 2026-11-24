package com.kdt.reservation.common.exception;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.config.JacksonConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerWebTest.SampleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, GlobalExceptionHandler.class, GlobalExceptionHandlerWebTest.SampleController.class})
class GlobalExceptionHandlerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비즈니스 예외는 ErrorCode의 상태와 메시지로 응답하고 errors는 담지 않는다")
    void businessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("ACCOMMODATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("숙소를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.timestamp", matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")));
    }

    @Test
    @DisplayName("본문 검증 실패는 400 INVALID_INPUT과 field, reason 목록으로 응답한다")
    void bodyValidation() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").value("숙소명은 필수입니다."));
    }

    @Test
    @DisplayName("필수 쿼리 파라미터가 없으면 파라미터 이름을 field로 알려준다")
    void missingParameter() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("keyword"))
                .andExpect(jsonPath("$.errors[0].reason").value("필수 값입니다."));
    }

    @Test
    @DisplayName("정의되지 않은 enum 쿼리 값은 형식 오류로 알려준다")
    void enumTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/enum").param("sort", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"))
                .andExpect(jsonPath("$.errors[0].reason").value("형식이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("JSON 문법 오류는 errors 없이 400 INVALID_INPUT으로 응답한다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    enum Sort {
        LATEST, PRICE_ASC
    }

    record Body(@NotBlank(message = "숙소명은 필수입니다.") String name) {
    }

    @RestController
    static class SampleController {

        @GetMapping("/test/business")
        ApiResponse<Void> business() {
            throw new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND);
        }

        @PostMapping("/test/body")
        ApiResponse<Void> body(@Valid @RequestBody Body body) {
            return ApiResponse.success();
        }

        @GetMapping("/test/param")
        ApiResponse<String> param(@RequestParam String keyword) {
            return ApiResponse.success(keyword);
        }

        @GetMapping("/test/enum")
        ApiResponse<Sort> sort(@RequestParam Sort sort) {
            return ApiResponse.success(sort);
        }
    }
}
