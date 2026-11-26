package com.kdt.reservation.reservation;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.common.response.PageResponse;
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

    //9.1 예약 생성
    @Transactional
    public ApiResponse<CreateReservationResponse> create(CreateReservationRequest request,Long userId){
        //방 찾기
        //방이 없는경우 ROOM_NOT_FOUND 예외처리 (예외 아직 미구현상태, 이름 수정 필요)
        Room room = this.roomRepository.findById(request.roomId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));
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
        Reservation reservation = new Reservation();
        //User
        Users user = this.userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        reservation.setUser(user);
        reservation.setRoom(room);
        reservation.setGuest_count(request.guestCount());
        reservation.setTotal_price(totalprice);
        reservation.setStatus(Status.PENDING);
        reservation.setCheck_in_date(request.checkInDate());
        reservation.setCheck_out_date(request.checkOutDate());
        reservation.setCreated_at(createtime);

        Reservation saved = this.reservationRepository.save(reservation);

        CreateReservationResponse dto = CreateReservationResponse.from(saved,nights,deadline);
        return ApiResponse.created(dto);

    }

    //9.2 내 예약 목록
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<MyReservationListResponse>> findMyReservations(Long userId, Status status, Pageable pageable){
        //내 예약이 존재 하는지 먼저 확인, 없으면 일단 401 UNAUTHORIZED
        Users user = this.userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
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

}
