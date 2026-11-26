package com.kdt.reservation.point;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.user.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final int EARN_RATE_PERCENT = 5;

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;


    @Transactional
    public Point use(Reservation reservation, int amount) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        Point point = this.pointRepository.findWithLockByUserId(reservation.getUser().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_POINT));

        if (point.getBalance() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        this.saveHistory(reservation, PointType.USE, amount);
        point.setBalance(point.getBalance() - amount);

        return point;
    }

    // 결제 금액의 5%를 적립한다
    @Transactional
    public Point earn(Reservation reservation) {
        Point point = this.getOrCreateWithLock(reservation.getUser());

        int usedPoint = this.pointHistoryRepository
                .findByReservationIdAndPointType(reservation.getId(), PointType.USE)
                .map(PointHistory::getAmount)
                .orElse(0);
        int amount = (int) ((long) (reservation.getTotal_price() - usedPoint) * EARN_RATE_PERCENT / 100);
        if (amount <= 0) {
            return point;
        }

        this.saveHistory(reservation, PointType.EARN, amount);
        point.setBalance(point.getBalance() + amount);

        return point;
    }

    // 예약에 사용한 포인트를 돌려준다
    @Transactional
    public Point refund(Reservation reservation) {
        Point point = this.getOrCreateWithLock(reservation.getUser());

        int amount = this.pointHistoryRepository
                .findByReservationIdAndPointType(reservation.getId(), PointType.USE)
                .map(PointHistory::getAmount)
                .orElse(0);
        if (amount <= 0) {
            return point;
        }

        this.saveHistory(reservation, PointType.REFUND, amount);
        point.setBalance(point.getBalance() + amount);

        return point;
    }

    // 예약당 유형별 한 건만 허용하며 내역을 저장한다
    private void saveHistory(Reservation reservation, PointType pointType, int amount) {
        if (this.pointHistoryRepository.existsByReservationIdAndPointType(reservation.getId(), pointType)) {
            throw new BusinessException(ErrorCode.POINT_HISTORY_ALREADY_EXISTS);
        }
        this.pointHistoryRepository.save(new PointHistory(reservation, pointType, amount));
    }

    // 유저의 포인트를 락을 걸어 조회하고, 없으면 잔액 0으로 만든다
    // 가입 시 생성 부분 추가 후 삭제 예정
    private Point getOrCreateWithLock(Users user) {
        return this.pointRepository.findWithLockByUserId(user.getId())
                .orElseGet(() -> this.pointRepository.save(new Point(user)));
    }
}
