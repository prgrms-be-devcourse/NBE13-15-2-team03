package com.kdt.reservation.payment;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.point.Point;
import com.kdt.reservation.point.PointService;
import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.reservation.ReservationRepository;
import com.kdt.reservation.reservation.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PointService pointService;

    @Transactional
    public ApiResponse<PaymentResponse> executePay(Long reservationId,Long userId, PaymentRequest request){
        //해야할것 -> 결제가 되면 Reservatoin, Point, PointHistory 갱신
        //로직

        //1.예약 id로 예약을 가져온다
        //예약이 없는경우 404 RESERVATION_NOTFOUND
        Reservation reservation = this.reservationRepository.findWithLockById(reservationId)
                .orElseThrow(()-> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
//        Reservation reservation = this.reservationRepository.findById(reservationId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        //사용자가 불일치할경우 403 RESERVATION_ACCESS_DENIED
        if(!reservation.getUser().getId().equals(userId))
            throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
        //상태가 PENDING이 아닌경우 409 RESERVATION_NOT_PAYABLE
        if(!reservation.getStatus().equals(Status.PENDING))
            throw new BusinessException(ErrorCode.RESERVATION_NOT_PAYABLE);

        //2.request.usePoint를 보고 포인트를 얼마 사용할것인지 파악한다
        int usePoint = request.usePoint();
        int totalPrice = reservation.getTotal_price();
        //여기서 포인트가 0이 아닐때 락 걸림
        int amount = amountCalculate(totalPrice,usePoint,reservation);

        //청구서가 작성된 시각
        LocalDateTime createdAt = reservation.getCreated_at();
        //청구 이후 영수증 잉크가 찍힌 시각
        LocalDateTime paidAt = LocalDateTime.now();

        //10분이 지나 Reservation 무효화 및 Payment 무효화(기록을 위하여 저장)
        Long minutes = ChronoUnit.MINUTES.between(createdAt,paidAt);
        if(minutes>10){
            reservation.setStatus(Status.FAILED);
            Payment payment = new Payment(
                    reservation,
                    request.method(),
                    PayStatus.FAILED,
                    amount,
                    createdAt,
                    paidAt);
            this.paymentRepository.save(payment);
            throw new BusinessException(ErrorCode.RESERVATION_NOT_PAYABLE);
        }

        //3.payment 생성
        //결제를 PAID로 저장
        Payment payment = new Payment(
                reservation,
                request.method(),
                PayStatus.PAID,
                amount,
                createdAt,
                paidAt);
        this.paymentRepository.save(payment);
        //예약을 CONFIRMED로 저장
        reservation.setStatus(Status.CONFIRMED);

        //4.포인트 생성 5%
        //reservation의 id를 통해 PointHistory를 조회하여 (할인 전 가격 - 포인트)
        //이후 잔여포인트에 생성된 포인트 합산하여 저장후 Point 객체 반환
        //Point totalPoint = this.pointService.earn(reservation);
        //소숫점 이하는 버립니다
        int earnedPoint = amount*5/100;

        PaymentResponse dto = PaymentResponse.from(payment,usePoint,earnedPoint);
        return ApiResponse.created(dto);
    }

//    @Transactional(readOnly = true)
//    public ApiResponse<PaymentListResponse> getRecentPayment(Long reservationId){
//
//        //단건조회를 하는 API
//        //결제 기록을 최신순 배열로 돌려준다. 페이징 하지 않는다
//
//        //로직
//        //reservationId에 맞는 결제 내역을 가져온다
//
//    }

    private int amountCalculate(int totalPrice,int usePoint,Reservation reservation){
        //usePoint가 0인경우 포인트 없이 결제를 진행한다
        if(usePoint ==0)
            return totalPrice;
        //PointService.use를 사용하여 pointHistory를 갱신하고 Point 테이블에서 사용된 포인트만큼 차감시킨다
        Point use = pointService.use(reservation, usePoint);
        return totalPrice-usePoint;
    }

}
