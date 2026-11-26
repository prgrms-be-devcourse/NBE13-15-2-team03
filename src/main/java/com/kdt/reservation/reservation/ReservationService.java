package com.kdt.reservation.reservation;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.common.response.ApiResponse;
import com.kdt.reservation.room.Room;
import com.kdt.reservation.room.RoomRepository;
import com.kdt.reservation.user.UserRepository;
import com.kdt.reservation.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

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


}
