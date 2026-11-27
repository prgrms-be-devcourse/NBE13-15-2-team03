package com.kdt.reservation.payment;

import com.kdt.reservation.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    //10.1 결제하여 예약을 확정 POST /api/reservations/{id}/payments
    @PostMapping("/api/reservations/{id}/payments")
    public ApiResponse<PaymentResponse> paymentExecution(
            @PathVariable("id") Long id,@AuthenticationPrincipal Long userId){
        return this.paymentService.executePay()
    }

}
