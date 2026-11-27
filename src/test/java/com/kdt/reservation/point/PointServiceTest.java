package com.kdt.reservation.point;

import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.reservation.Reservation;
import com.kdt.reservation.user.UserService;
import com.kdt.reservation.user.Users;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// PointService 테스트 (기본 포인트 생성, 포인트 행이 없는 경우, 잔액·내역 조회)
@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private PointRepository pointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserService userService;

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

    @Test
    @DisplayName("잔액 조회 시 회원의 포인트 잔액을 돌려준다")
    void getMyPoint_success() {
        Point point = new Point(mock(Users.class));
        point.setBalance(23750);
        given(pointRepository.findByUserId(USER_ID)).willReturn(Optional.of(point));

        PointResponse response = pointService.getMyPoint(USER_ID);

        assertThat(response.balance()).isEqualTo(23750);
        verify(userService).findActiveUser(USER_ID);
    }

    @Test
    @DisplayName("회원이 없거나 탈퇴했으면 포인트를 조회하지 않고 UNAUTHORIZED를 던진다")
    void getMyPoint_userNotFound() {
        given(userService.findActiveUser(USER_ID)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> pointService.getMyPoint(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(pointRepository, never()).findByUserId(any());
    }

    @Test
    @DisplayName("포인트 행이 없으면 잔액 조회 시 POINT_NOT_FOUND를 던진다")
    void getMyPoint_pointNotFound() {
        given(pointRepository.findByUserId(USER_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pointService.getMyPoint(USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);
    }

    @Test
    @DisplayName("내역 조회 시 내역을 응답 DTO로 바꿔 페이지로 돌려준다")
    void getMyPointHistory_success() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 12, 1, 15, 3, 7);
        Reservation reservation = mock(Reservation.class);
        given(reservation.getId()).willReturn(501L);
        PointHistory history = history(302L, PointType.EARN, 13750, reservation, createdAt);
        given(pointRepository.existsByUserId(USER_ID)).willReturn(true);
        given(pointHistoryRepository.findMyHistory(eq(USER_ID), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1));

        Page<PointHistoryResponse> page = pointService.getMyPointHistory(USER_ID, null, PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).containsExactly(
                new PointHistoryResponse(302L, PointType.EARN, 13750, 501L, createdAt));
        verify(userService).findActiveUser(USER_ID);
    }

    @Test
    @DisplayName("예약이 없는 내역은 reservationId를 null로 돌려준다")
    void getMyPointHistory_withoutReservation() {
        PointHistory history = history(303L, PointType.EARN, 1000, null, LocalDateTime.of(2026, 12, 2, 9, 0, 1));
        given(pointRepository.existsByUserId(USER_ID)).willReturn(true);
        given(pointHistoryRepository.findMyHistory(eq(USER_ID), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(history), PageRequest.of(0, 20), 1));

        Page<PointHistoryResponse> page = pointService.getMyPointHistory(USER_ID, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).reservationId()).isNull();
    }

    @Test
    @DisplayName("내역 조회 시 type을 그대로 넘기고 클라이언트의 정렬은 버린다")
    void getMyPointHistory_passesTypeAndIgnoresSort() {
        given(pointRepository.existsByUserId(USER_ID)).willReturn(true);
        given(pointHistoryRepository.findMyHistory(eq(USER_ID), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        pointService.getMyPointHistory(USER_ID, PointType.USE, PageRequest.of(2, 10, Sort.by("foo")));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(pointHistoryRepository).findMyHistory(eq(USER_ID), eq(PointType.USE), captor.capture());
        assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    @DisplayName("회원이 없거나 탈퇴했으면 내역을 조회하지 않고 UNAUTHORIZED를 던진다")
    void getMyPointHistory_userNotFound() {
        given(userService.findActiveUser(USER_ID)).willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        assertThatThrownBy(() -> pointService.getMyPointHistory(USER_ID, null, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(pointRepository, never()).existsByUserId(any());
        verify(pointHistoryRepository, never()).findMyHistory(any(), any(), any());
    }

    @Test
    @DisplayName("포인트 행이 없으면 내역을 조회하지 않고 POINT_NOT_FOUND를 던진다")
    void getMyPointHistory_pointNotFound() {
        given(pointRepository.existsByUserId(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> pointService.getMyPointHistory(USER_ID, null, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.POINT_NOT_FOUND);

        verify(pointHistoryRepository, never()).findMyHistory(any(), any(), any());
    }

    private static PointHistory history(Long id, PointType type, int amount, Reservation reservation,
                                        LocalDateTime createdAt) {
        PointHistory history = new PointHistory();
        history.setId(id);
        history.setPointType(type);
        history.setAmount(amount);
        history.setReservation(reservation);
        history.setCreated_at(createdAt);
        return history;
    }
}
