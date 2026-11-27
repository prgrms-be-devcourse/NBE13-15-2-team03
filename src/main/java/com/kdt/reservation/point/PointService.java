package com.kdt.reservation.point;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.user.UserService;
import com.kdt.reservation.user.Users;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {
    private static final int EARN_RATE_PERCENT = 5;

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final UserService userService;
    private final EntityManager entityManager;

    // 가입한 유저의 포인트를 잔액 0으로 만든다
    @Transactional
    public Point createDefault(Users user) {
        return this.pointRepository.save(new Point(user));
    }

    @Transactional
    public Point use(Reservation reservation, int amount) {
        if (amount < 0 || amount > reservation.getTotal_price()) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        Point point = this.getWithLock(reservation.getUser().getId());

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
        Point point = this.getWithLock(reservation.getUser().getId());

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
        Point point = this.getWithLock(reservation.getUser().getId());

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

    // 유저의 포인트를 락을 걸어 조회하고, 없으면 예외를 던진다
    private Point getWithLock(Long userId) {
        return this.findWithLock(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));
    }

    // 유저의 포인트를 락을 걸어 조회한다
    // 같은 트랜잭션에서 락 없이 먼저 조회한 엔티티가 있으면 락 조회도 그 엔티티(이전 잔액)를 돌려주므로,
    // 엔티티를 영속성 컨텍스트에서 떼어낸 뒤 다시 조회해 다른 트랜잭션이 커밋한 잔액을 읽는다
    private Optional<Point> findWithLock(Long userId) {
        this.pointRepository.findWithLockByUserId(userId).ifPresent(this.entityManager::detach);
        return this.pointRepository.findWithLockByUserId(userId);
    }

    @Transactional(readOnly = true)
    public PointResponse getMyPoint(Long userId) {
        this.userService.findActiveUser(userId);

        Point point = this.pointRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_NOT_FOUND));

        return new PointResponse(point.getBalance());
    }
}
