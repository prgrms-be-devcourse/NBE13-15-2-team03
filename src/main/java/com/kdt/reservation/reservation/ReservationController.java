package com.kdt.reservation.reservation;

import com.kdt.reservation.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    //예약할때 예약자는 로그인정보에서 가져오기에 요청에 예약자 정보가 없습니다..
    @PostMapping
    public ApiResponse<CreateReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
                                                         @AuthenticationPrincipal Long userId){
        return this.reservationService.create(request,userId);
    }


}
