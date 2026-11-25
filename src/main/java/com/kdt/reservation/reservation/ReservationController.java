package com.kdt.reservation.reservation;

import com.kdt.reservation.common.response.ApiResponse;
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
    public ApiResponse<MyReservationListResponse> getMyReservations(
            @RequestParam Status status,
            @RequestParam Pageable pageable,
            @AuthenticationPrincipal Long userId){
        return this.reservationService.findMyReservations(userId,status,pageable);
    }


}
