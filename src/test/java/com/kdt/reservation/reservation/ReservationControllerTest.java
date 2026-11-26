package com.kdt.reservation.reservation;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
                501L, Status.PENDING, 280000, 2, LocalDateTime.of(2026, 12, 1, 15, 10)
        );
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willReturn(ApiResponse.created(response));
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isCreated());
    }

    //9.1비정상동작
    //400 INVALID_INPUT
    //roomId에 String을 넣어봅니다
    @Test
    void create_InvalidInput() throws Exception {
        String body = """
                    {
                    "roomId":"Wrong_Input_Id",
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-26",
                    "guestCount": 3
                    }
                """;
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                //400 badRequest와 함께
                .andExpect(status().isBadRequest())
                //INVALID_INPUT맞는지 확인합니다
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    //400 INVALID_DATE_RANGE
    @Test
    void create_InvalidDateRange_SameDate() throws Exception {
        //시작과 끝이 같음
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-24",
                    "guestCount": 3
                }
                """;
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.INVALID_DATE_RANGE));


        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_DATE_RANGE"));

    }

    //400 GUEST_COUNT_EXCEEDED
    //최대 인원을 넘음
    @Test
    void create_GuestCountExceeded() throws Exception {
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-26",
                    "guestCount": 99999
                }
                """;
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.GUEST_COUNT_EXCEEDED));

        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("GUEST_COUNT_EXCEEDED"));
    }

    //401 USER_NOT_FOUND - 현재 보류
    //403 FORBIDDEN - 현재 보류
    //404 ROOM_NOT_FOUND
    @Test
    void create_RoomNotFound() throws Exception {
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-24",
                    "guestCount": 3
                }
                """;
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_FOUND"));
    }

    //409 ROOM_NOT_AVAILABLE
    @Test
    void create_RoomNotAvailable() throws Exception {
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-24",
                    "guestCount": 3
                }
                """;
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE));
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ROOM_NOT_AVAILABLE"));
    }

    //409 RESERVATION_DATE_CONFLICT
    @Test
    void create_ReservationDateConflict() throws Exception {
        String body = """
                {
                    "roomId":101,
                    "checkInDate": "2026-12-24",
                    "checkOutDate": "2026-12-24",
                    "guestCount": 3
                }
                """;
        given(reservationService.create(any(CreateReservationRequest.class), any()))
                .willThrow(new BusinessException(ErrorCode.RESERVATION_DATE_CONFLICT));
        mvc.perform(
                        post("/api/reservations")
                                .contentType("application/json")
                                .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESERVATION_DATE_CONFLICT"));

    }

    //9.2 정상동작
    @Test
    void findMyReservations_success() throws Exception {
        MyReservationListResponse dto = new MyReservationListResponse(
                111L,
                Status.PENDING,
                new MyReservationListResponse.AccommodationSummary(1L,"AccdtinName"),
                new RoomSummary(101L,"RoomName"),
                LocalDate.of(2026,12,24),
                LocalDate.of(2026,12,26),
                3,
                280000,
                false,
                LocalDateTime.of(2026,12,1,15,10)
                );

        Page<MyReservationListResponse> page = new PageImpl<>(List.of(dto),PageRequest.of(0,20),1);
        given(reservationService.findMyReservations(
                any(), eq(Status.PENDING), any(Pageable.class)
        )).willReturn(ApiResponse.page(page));

        mvc.perform(
                        get("/api/reservations/me")
                                .contentType("application/json")
                                .param("status","PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reservationId").value(111))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].hasReview").value(false));
    }

    //9.2비정상동작
    //400 INVALID_INPUT
    //
    @Test
    void findMyReservations_InvalidInput()throws Exception{
        MyReservationListResponse dto = new MyReservationListResponse(
                111L,
                Status.PENDING,
                new MyReservationListResponse.AccommodationSummary(1L,"AccdtinName"),
                new RoomSummary(101L,"RoomName"),
                LocalDate.of(2026,12,24),
                LocalDate.of(2026,12,26),
                3,
                280000,
                false,
                LocalDateTime.of(2026,12,1,15,10)
        );

        Page<MyReservationListResponse> page = new PageImpl<>(List.of(dto),PageRequest.of(0,20),1);
        given(reservationService.findMyReservations(
                any(),eq(Status.PENDING),any(Pageable.class)
        )).willThrow(new BusinessException(ErrorCode.INVALID_INPUT));
        mvc.perform(
                get("/api/reservations/me")
                        .contentType("application/json")
                        .param("status","WrongStatus"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));

    }


}
