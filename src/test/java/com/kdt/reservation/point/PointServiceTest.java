package com.kdt.reservation.point;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.user.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// PointService 테스트 (기본 포인트 생성, 포인트 행이 없는 경우)
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("기본 포인트는 해당 회원의 잔액 0으로 저장한다")
    void createDefault_savesZeroBalance() {
        Users user = mock(Users.class);

        pointService.createDefault(user);

        ArgumentCaptor<Point> captor = ArgumentCaptor.forClass(Point.class);
        verify(pointRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getBalance()).isZero();
    }

    @Test
    @DisplayName("포인트 행이 없으면 사용 시 POINT_NOT_FOUND를 던진다")
    void use_pointNotFound() {
        Reservation reservation = reservationWithoutPoint();

        assertThatThrownBy(() -> pointService.use(reservation, 1000))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);

        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 행이 없으면 적립 시 POINT_NOT_FOUND를 던진다")
    void earn_pointNotFound() {
        Reservation reservation = reservationWithoutPoint();

        assertThatThrownBy(() -> pointService.earn(reservation))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);

        verify(pointRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 행이 없으면 환불 시 POINT_NOT_FOUND를 던진다")
    void refund_pointNotFound() {
        Reservation reservation = reservationWithoutPoint();

        assertThatThrownBy(() -> pointService.refund(reservation))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);

        verify(pointRepository, never()).save(any());
    }

    // 포인트 행이 없는 회원의 예약을 만든다
    private Reservation reservationWithoutPoint() {
        Users user = mock(Users.class);
        given(user.getId()).willReturn(USER_ID);
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setTotal_price(100000);
        given(pointRepository.findWithLockByUserId(USER_ID)).willReturn(Optional.empty());
        return reservation;
    }
}