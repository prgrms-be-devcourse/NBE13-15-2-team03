package com.kdt.reservation.point;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.reservation.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {
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

        PointHistory history = new PointHistory(reservation, PointType.USE, amount);
        point.setBalance(point.getBalance() - amount);

        this.pointHistoryRepository.save(history);

        return point;
    }

    // 5% 적립
    public Point earn(Reservation reservation) {
        return null;
    }
    
    public Point refund(Reservation reservation) {
        return null;
    }
}
