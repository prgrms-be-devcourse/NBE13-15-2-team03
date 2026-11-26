package com.kdt.reservation.reservation;

import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    //9.1 예약생성 POST /api/reservation
    @PostMapping
    public ApiResponse<CreateReservationResponse> create(@Valid @RequestBody CreateReservationRequest request,
                                                         @AuthenticationPrincipal Long userId){
        return this.reservationService.create(request,userId);
    }
    //9.2 나의 예약목록 조회 GET /api/reservations/me
    @GetMapping("/me")
    public ApiResponse<PageResponse<MyReservationListResponse>> getMyReservations(
            //required true라고 하고 내부에서 null처리 로직을 주석 처리 하였습니다
            @RequestParam(required = true) Status status,
            Pageable pageable,
            @AuthenticationPrincipal Long userId){
        return this.reservationService.findMyReservations(userId,status,pageable);
    }


}
