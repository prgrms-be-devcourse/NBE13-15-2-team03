package com.kdt.reservation.reservation;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.common.response.PageResponse;
import com.kdt.reservation.payment.PaymentRepository;
import com.kdt.reservation.point.PointHistoryRepository;
import com.kdt.reservation.review.ReviewRepository;
import com.kdt.reservation.room.Room;
import com.kdt.reservation.room.RoomRepository;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PaymentRepository paymentRepository;

    //9.1 예약 생성
    @Transactional
    public ApiResponse<CreateReservationResponse> create(CreateReservationRequest request,Long userId){
        //방 찾기
        //방이 없는경우 ROOM_NOT_FOUND 예외처리 (예외 아직 미구현상태, 이름 수정 필요)
        Room room = this.roomRepository.findById(request.roomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
        //삭제된 방인경우 deleted_at 값이 존재하므로 null이 아니면 ROOM_NOT_AVAILABLE
        if (room.getDeleted_at() !=null)
            throw new BusinessException(ErrorCode.ROOM_NOT_AVAILABLE);

        //예약인원이 최대 수용인원보다 많을경우
        if(request.guestCount()>room.getMax_capacity())
            throw new BusinessException(ErrorCode.GUEST_COUNT_EXCEEDED);

        //LocalDateTime은 년/월/일/시간/분/초 가 각각 객체라서 int로 뽑으면 안된다고 합니다
        //그렇기에 ChoroUnit.DAYS.between()을 사용하였습니다
        int nights = (int)ChronoUnit.DAYS.between(request.checkInDate(),request.checkOutDate());

        //초과인원은 입력받은인원-statndard수용인원으로 계산하였습니다
        int extraPerson= request.guestCount() - room.getStandard_capacity();
        //추가인원이 없는경우 0으로 처리하여 추가요금계산을 처리했습니다.
        if(extraPerson<0)
            extraPerson=0;
        LocalDateTime createtime = LocalDateTime.now();
        LocalDateTime deadline = createtime.plusMinutes(10);

        int totalprice = room.getPrice() * nights
                + room.getExtra_person_fee()*extraPerson*nights;
        //반환시켜줘야할 DTO 내용
        //reservationId, status, totalPrice,nights,paymentDeadline

        //User
        Users user = this.userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Reservation reservation = new Reservation(user,room,request.guestCount(),totalprice,Status.PENDING,
                request.checkInDate(),request.checkOutDate(),createtime);

        Reservation saved = this.reservationRepository.save(reservation);

        CreateReservationResponse dto = CreateReservationResponse.from(saved,nights,deadline);
        return ApiResponse.created(dto);

    }

    //9.2 내 예약 목록
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<MyReservationListResponse>> findMyReservations(Long userId, Status status, Pageable pageable){
        //내 예약이 존재 하는지 먼저 확인, 없으면 일단 401 UNAUTHORIZED
        Users user = this.userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        //status가 null인경우
        //if(status == null)
            //throw new BusinessException(ErrorCode.INVALID_INPUT);

        //로직

        //1.페이지size만큼 reservation 가져오기
        Page<Reservation> allByUserId = this.reservationRepository.findAllByUserId(userId, status, pageable);
        //2.SET을 만들어서 사용되는 Reservaion_id 저장하기)
        List<Reservation> reservationList = allByUserId.getContent();
        Set<Long> idSet = new HashSet<>();
        //리뷰 조회를 위한 set
        for(Reservation reservation: reservationList){
            idSet.add(reservation.getId());
        }
        //리뷰가 있는 id들을 모아놓은 List
        Set<Long> hasReviewList = this.reviewRepository.findByReservationId(idSet);

        Page<MyReservationListResponse> dto = allByUserId.map(reservation ->
                MyReservationListResponse.from(reservation,hasReviewList.contains(reservation.getId())));
       return ApiResponse.page(dto);


    }

//    //9.3 예약 상세 단건조회
//    @Transactional(readOnly = true)
//    public ApiResponse<ReservationResponse> findReservationDetail(Long reservationId,Long userId){
//        //예약자 본인과 그 숙소를 등록한 host만 볼 수 있다.
//
//        //로직
//        //ID(예약아이디)로 찾는 로직
//        //1.주어지는 ID로 예약을 찾는다
//        //1.1없는경우 404 RESERVATION_NOT_FOUND
//        Reservation reservation = this.reservationRepository.findById(reservationId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
//        //2.예약이 존재하는경우 사용자 검증을 진행한다
//
//        //예약내역을 가져오면 사용자를 검증한다
//        //1.사용자가 HOST인지 GUEST인지 확인한다
//        Users user = this.userRepository.findById(userId)
//                .orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));
//        //2.사용자가 Guest인 경우 userID와 사용자의 id를 비교한다
//        if(user.getRole().equals("GUEST")){
//            //2.1검증 성공인 경우
//            if(user.getId().equals(userId)){
//
//                this.paymentRepository.findByReservationId(reservationId)
//                        .orElse(null);
//
////                int nights = (int)ChronoUnit.DAYS.between(reservation.getCheck_in_date(),reservation.getCheck_out_date());
////                ReservationResponse.from(reservation,nights,)
//                //반환형태에 맞게 DTO로 포장하여 ApiResponse에 담아 return
//            }
//            //2.2없는경우 403 RESERVATION_ACCESS_DENIED
//            else
//                throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED);
//        }
//        //3.사용자가 Host인 경우 Host가 등록한 숙소목록을 만든다
//        //4.숙소 목록에서 accomodtation_id와 같은 id를 가진 숙소를 찾는다
//        //4.1검증 성공인경우 반환형태에 맞게 DTO로 포장하여 ApiResponse에 담아 return
//        //4.2검증 실패인경우 403 RESERVATION_ACCESS_DENIED
//    }
}
