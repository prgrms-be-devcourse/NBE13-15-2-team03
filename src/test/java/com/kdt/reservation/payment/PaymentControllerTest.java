package com.kdt.reservation.payment;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.reservation.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PaymentControllerTest {

    @Autowired
    private MockMvc mvc;
    // 가짜 객체
    @MockitoBean
    private PaymentService paymentService;

    //10.1 정상동작
    @Test
    void paymentExecution_success()throws Exception{
        String body = """
                {
                "method": "CARD",
                "usePoint": 5000
                }""";
        PaymentResponse response = new PaymentResponse(
                9001L,501L, Status.CONFIRMED,PayMethod.CARD,PayStatus.PAID,275000,5000,13750,
                LocalDateTime.of(2026, 12, 1, 15, 3));

        given(paymentService.executePay(eq(501L), any(), any(PaymentRequest.class)))
                .willReturn(ApiResponse.created(response));
        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.paymentId").value(9001))
                .andExpect(jsonPath("$.data.reservationId").value(501))
                .andExpect(jsonPath("$.data.reservationStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.method").value("CARD"))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amount").value(275000))
                .andExpect(jsonPath("$.data.usedPoint").value(5000))
                .andExpect(jsonPath("$.data.earnedPoint").value(13750));
    }

    //400 INVALID_INPUT
    //method를 빼고 보냅니다. @Valid에서 걸려 서비스까지 가지 않으므로 given이 필요 없습니다
    @Test
    void paymentExecution_invalidInput() throws Exception {
        String body = """
                {
                "usePoint": 5000
                }""";

        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    //400 INVALID_POINT_AMOUNT
    //예약 금액보다 큰 포인트를 보냅니다. 서비스(use)가 던지는 예외가 400으로 바뀌는지 봅니다
    @Test
    void paymentExecution_invalidPointAmount() throws Exception {
        String body = """
                {
                "method": "CARD",
                "usePoint": 999999
                }""";
        given(paymentService.executePay(eq(501L), any(), any(PaymentRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INVALID_POINT_AMOUNT));

        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_POINT_AMOUNT"));
    }

    //404 RESERVATION_NOT_FOUND
    @Test
    void paymentExecution_reservationNotFound() throws Exception {
        String body = """
                {
                "method": "CARD",
                "usePoint": 5000
                }""";
        given(paymentService.executePay(eq(501L), any(), any(PaymentRequest.class)))
                .willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESERVATION_NOT_FOUND"));
    }

    //409 RESERVATION_NOT_PAYABLE
    @Test
    void paymentExecution_reservationNotPayable() throws Exception {
        String body = """
                {
                "method": "CARD",
                "usePoint": 5000
                }""";
        given(paymentService.executePay(eq(501L), any(), any(PaymentRequest.class)))
                .willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_PAYABLE));

        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("RESERVATION_NOT_PAYABLE"));
    }

    //409 INSUFFICIENT_POINT
    @Test
    void paymentExecution_insufficientPoint() throws Exception {
        String body = """
                {
                "method": "CARD",
                "usePoint": 5000
                }""";
        given(paymentService.executePay(eq(501L), any(), any(PaymentRequest.class)))
                .willThrow(new BusinessException(ErrorCode.INSUFFICIENT_POINT));

        mvc.perform(
                post("/api/reservations/501/payments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_POINT"));
    }

}
