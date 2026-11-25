package com.kdt.reservation.reservation;

import com.kdt.reservation.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReservationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ReservationControllerTest {

    @Autowired
    private MockMvc mvc;
    //가짜객체
    @MockitoBean
    private ReservationService reservationService;

    //9.1 정상동작
    @Test
    void create_success() throws Exception {
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-26",
                    "guestCount": 3
                }
                """;
        CreateReservationResponse response = new CreateReservationResponse(
                501L, Status.PENDING,280000,2, LocalDateTime.of(2026,12,1,15,10)
        );
        given(reservationService.create(any(CreateReservationRequest.class),any()))
                .willReturn(ApiResponse.created(response));
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isCreated());
    }


}
