package com.kdt.reservation.accommodation;

import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 숙소 관련 "실제 처리 규칙"을 담는 곳.
 *
 * [역할 나누기] 요청은 Controller -> Service -> Repository -> DB 순서로 흘러간다.
 * - Controller : 주소(URL)와 연결되는 입구. 요청을 받고 응답 모양으로 감싸서 돌려준다.
 * - Service    : 규칙을 처리한다. (없으면 에러, 삭제된 숙소는 제외 등)
 * - Repository : DB 에서 꺼내고 저장한다.
 * 이렇게 나누면 "규칙을 고칠 때는 Service 만" 보면 되어 찾기 쉽다.
 *
 * [@Service 를 붙인 이유]
 * 스프링에게 "이 클래스를 직접 만들어서 관리해 달라"고 알리는 표시다.
 * 이 표시가 있어야 Controller 가 이 클래스를 넘겨받아 쓸 수 있다.
 *
 * [@RequiredArgsConstructor 를 붙인 이유]
 * 아래 final 필드들을 받는 생성자를 롬복이 자동으로 만들어 준다.
 * 스프링은 그 생성자를 보고 필요한 레포지토리를 알아서 넣어 준다. (이것을 "의존성 주입"이라고 한다)
 * 그래서 new AccommodationRepository() 같은 코드를 직접 쓰지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AccommodationService {

    // final: 한 번 넣어 주면 바뀌지 않는다는 뜻. 실수로 다른 것으로 바꿔치기하는 일을 막는다.
    private final AccommodationRepository accommodationRepository;
    private final AccommodationImageRepository accommodationImageRepository;

    /**
     * 숙소 상세 조회. (명세 6.2, 이슈 #27)
     *
     * [@Transactional(readOnly = true) 를 붙인 이유]
     * - @Transactional : 이 메서드가 끝날 때까지 DB 연결을 하나로 묶어 유지한다.
     *     우리 프로젝트는 open-in-view 가 꺼져 있어서(application.yaml), 이 표시가 없으면
     *     메서드 중간에 연관된 데이터(예: 숙소 주인)를 꺼내려 할 때 에러가 날 수 있다.
     * - readOnly = true : "읽기만 한다"는 표시. 실수로 데이터가 바뀌는 것을 막고 조회 성능에도 유리하다.
     *
     * @param id 조회할 숙소 번호 (주소의 {id} 자리에 들어온 값)
     * @return 응답의 data 에 들어갈 숙소 상세 정보
     */
    @Transactional(readOnly = true)
    public AccommodationDetailResponse getDetail(Long id) {

        // 1) 삭제되지 않은 숙소를 번호로 찾는다. 없으면 "숙소 없음" 예외를 던진다.
        //    - orElseThrow : Optional 상자에 값이 있으면 꺼내고, 비어 있으면 괄호 안의 예외를 던진다.
        //    - 예외를 "던지기만" 하는 이유: 에러 응답(JSON)으로 바꾸는 일은 공통 예외 처리기
        //      (GlobalExceptionHandler)가 한 곳에서 맡는다. 덕분에 팀 전체의 에러 응답 모양이 같아진다.
        //    - ACCOMMODATION_NOT_FOUND 는 ErrorCode 에 404 로 정의되어 있다. (명세: 없거나 삭제된 숙소는 404)
        Accommodation accommodation = accommodationRepository.findActiveById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOMMODATION_NOT_FOUND));

        // 2) 이 숙소의 이미지 주소 목록을 id 오름차순으로 가져온다. 이미지가 없으면 빈 목록이 온다.
        List<String> imageUrls = accommodationImageRepository.findUrlsByAccommodationId(id);

        // 3) 엔티티를 응답 모양(DTO)으로 바꿔서 돌려준다.
        //    바꾸는 방법은 DTO 의 of 메서드에 모아 두었다.
        return AccommodationDetailResponse.of(accommodation, imageUrls);
    }
}
