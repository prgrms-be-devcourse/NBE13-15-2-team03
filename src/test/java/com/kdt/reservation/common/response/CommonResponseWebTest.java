package com.kdt.reservation.common.response;

import com.kdt.reservation.config.JacksonConfig;
import com.kdt.reservation.config.PageableConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommonResponseWebTest.SampleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({JacksonConfig.class, PageableConfig.class, CommonResponseWebTest.SampleController.class})
class CommonResponseWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("내용이 없는 성공 응답은 status와 data null만 담는다")
    void emptySuccess() throws Exception {
        mockMvc.perform(get("/test/empty"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":200,\"data\":null}", JsonCompareMode.STRICT));
    }

    @Test
    @DisplayName("생성 응답은 HTTP 201과 본문 status 201이다")
    void createdSuccess() throws Exception {
        mockMvc.perform(post("/test/created"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    @DisplayName("날짜는 YYYY-MM-DD, 일시는 초 단위까지 나간다")
    void dateFormat() throws Exception {
        mockMvc.perform(get("/test/dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-11-23"))
                .andExpect(jsonPath("$.data.dateTime").value("2026-11-23T14:00:00"))
                .andExpect(jsonPath("$.data.missing").value(nullValue()));
    }

    @Test
    @DisplayName("쿼리 파라미터의 날짜는 ISO 형식으로 받는다")
    void dateQueryParam() throws Exception {
        mockMvc.perform(get("/test/echo-date").param("checkIn", "2026-12-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("2026-12-01"));
    }

    @Test
    @DisplayName("페이지 파라미터가 없으면 page 0, size 20이다")
    void defaultPage() throws Exception {
        mockMvc.perform(get("/test/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(135))
                .andExpect(jsonPath("$.data.totalPages").value(7))
                .andExpect(jsonPath("$.data.hasNext").value(true));
    }

    @Test
    @DisplayName("page와 size를 보내면 그대로 적용한다")
    void customPage() throws Exception {
        mockMvc.perform(get("/test/page").param("page", "1").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @ParameterizedTest
    @CsvSource({"size,500,0,100", "size,101,0,100", "size,0,0,20", "size,-5,0,20", "size,abc,0,20", "page,-1,0,20", "page,abc,0,20"})
    @DisplayName("page나 size가 범위를 벗어나면 400 없이 보정한다")
    void correctedPage(String name, String value, int expectedPage, int expectedSize) throws Exception {
        mockMvc.perform(get("/test/page").param(name, value))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(expectedPage))
                .andExpect(jsonPath("$.data.size").value(expectedSize));
    }

    record Created(long id) {
    }

    record Dates(LocalDate date, LocalDateTime dateTime, String missing) {
    }

    @RestController
    static class SampleController {

        @GetMapping("/test/empty")
        ResponseEntity<ApiResponse<Void>> empty() {
            return ApiResponse.ok();
        }

        @PostMapping("/test/created")
        ResponseEntity<ApiResponse<Created>> created() {
            return ApiResponse.created(new Created(12));
        }

        @GetMapping("/test/dates")
        ResponseEntity<ApiResponse<Dates>> dates() {
            LocalDateTime dateTime = LocalDateTime.of(2026, 11, 23, 14, 0, 0, 123_456_000);
            return ApiResponse.ok(new Dates(dateTime.toLocalDate(), dateTime, null));
        }

        @GetMapping("/test/echo-date")
        ResponseEntity<ApiResponse<LocalDate>> echoDate(@RequestParam LocalDate checkIn) {
            return ApiResponse.ok(checkIn);
        }

        @GetMapping("/test/page")
        ResponseEntity<ApiResponse<PageResponse<String>>> page(Pageable pageable) {
            Page<String> page = new PageImpl<>(List.of("a", "b"), pageable, 135);
            return ApiResponse.ok(PageResponse.from(page));
        }
    }
}
