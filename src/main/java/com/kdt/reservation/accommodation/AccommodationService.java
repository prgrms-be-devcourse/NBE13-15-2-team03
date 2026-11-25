package com.kdt.reservation.accommodation;

import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse;
import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse.FeatureInfo;
import com.kdt.reservation.accommodation.dto.AccommodationDetailResponse.RoomInfo;
import com.kdt.reservation.common.Feature;
import com.kdt.reservation.common.exception.BusinessException;
import com.kdt.reservation.common.exception.ErrorCode;
import com.kdt.reservation.review.ReviewRepository;
import com.kdt.reservation.room.Room;
import com.kdt.reservation.room.RoomFeature;
import com.kdt.reservation.room.RoomFeatureRepository;
import com.kdt.reservation.room.RoomImage;
import com.kdt.reservation.room.RoomImageRepository;
import com.kdt.reservation.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final AccommodationFeatureRepository accommodationFeatureRepository;
    // 숙소 상세에는 객실 정보도 들어가므로 객실 쪽 레포지토리도 넘겨받아 쓴다.
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final RoomFeatureRepository roomFeatureRepository;
    // 평균 평점은 리뷰 담당(D)이 만든 리뷰 레포지토리의 메서드를 가져다 쓴다.
    // 이유: 같은 조회문을 두 군데에 따로 두면, 한쪽만 고쳤을 때 숙소 상세와 리뷰 목록의 평점이 서로 달라질 수 있다.
    private final ReviewRepository reviewRepository;

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

        // 3) 이 숙소의 편의시설을 가져와 응답 모양(FeatureInfo)으로 바꾼다.
        List<Feature> featureEntities = accommodationFeatureRepository.findFeaturesByAccommodationId(id);
        List<FeatureInfo> features = new ArrayList<>();
        for (Feature feature : featureEntities) {
            features.add(FeatureInfo.of(feature));
        }

        // 4) 리뷰 수와 평균 평점을 구한다. (삭제된 리뷰는 조회문에서 이미 빠져 있다)
        //    - 리뷰 수: 리뷰 레포지토리에 아직 메서드가 없어 숙소 레포지토리의 임시 메서드를 쓴다. (TODO 표시해 둠)
        //    - 평균 평점: 리뷰 레포지토리의 메서드를 쓴다. 리뷰가 없으면 null 이 온다.
        long reviewCount = accommodationRepository.countReviewsByAccommodationId(id);
        Double rawAverage = reviewRepository.findAverageRatingByAccommodationId(id);
        double averageRating = roundToOneDecimal(rawAverage);

        // 5) 이 숙소의 객실 목록을 응답 모양으로 만든다. (아래 메서드로 따로 빼 두었다)
        List<RoomInfo> rooms = getRoomInfos(id);

        // 6) 지금까지 모은 값들을 응답 모양(DTO)으로 묶어서 돌려준다.
        //    묶는 방법은 DTO 의 of 메서드에 모아 두었다.
        return AccommodationDetailResponse.of(accommodation, imageUrls, features, averageRating, reviewCount, rooms);
    }

    /**
     * 평균 평점을 소수 첫째 자리까지 반올림한다. 리뷰가 없어서 평균이 null 이면 0.0 을 돌려준다.
     * (명세 6.2: 평균 평점은 소수 첫째 자리까지, 리뷰가 없으면 0.0)
     *
     * [계산 방법] 예: 4.4666... -> 10 을 곱해 44.666... -> 반올림해 45 -> 10.0 으로 나눠 4.5
     * - Math.round : 소수점 아래를 반올림해 정수로 만든다.
     * - 10 이 아니라 10.0 으로 나누는 이유: 정수끼리 나누면 소수점 아래가 버려져 4 가 되어 버린다.
     */
    private double roundToOneDecimal(Double average) {
        if (average == null) {
            return 0.0;
        }
        return Math.round(average * 10) / 10.0;
    }

    /**
     * 한 숙소의 객실들을 응답 모양(RoomInfo)의 목록으로 만든다.
     *
     * [private 메서드로 따로 뺀 이유]
     * getDetail 안에 전부 적으면 길어져서 "큰 흐름"이 한눈에 안 보인다.
     * 객실 부분만 떼어 이름을 붙여 두면 getDetail 은 1)~6) 순서만 읽어도 이해된다.
     * private 은 "이 클래스 안에서만 쓴다"는 뜻이다.
     *
     * [DB 조회 횟수] 객실 1번 + 객실 이미지 1번 + 객실 편의시설 1번 = 총 3번. 객실 수가 늘어도 3번 그대로다.
     */
    private List<RoomInfo> getRoomInfos(Long accommodationId) {

        // (1) 삭제되지 않은 객실들을 번호 순서대로 가져온다. 판매 중지 객실도 포함된다. (명세 6.2)
        List<Room> rooms = roomRepository.findActiveByAccommodationId(accommodationId);

        // (2) 이 숙소의 모든 객실 이미지를 한 번에 가져온다. (객실마다 따로 조회하지 않기 위해서)
        List<RoomImage> roomImages = roomImageRepository.findByAccommodationId(accommodationId);

        // (3) 가져온 이미지들을 "객실 번호 -> 그 객실의 이미지 주소 목록" 모양으로 나눠 담는다.
        //     - Map 은 "열쇠(key)로 값(value)을 찾는 사전" 같은 자료 구조다. 여기서는 열쇠가 객실 번호다.
        //     - 이렇게 정리해 두면 (6)에서 객실마다 자기 이미지를 바로 꺼낼 수 있다.
        Map<Long, List<String>> imageUrlsByRoomId = new HashMap<>();
        for (RoomImage roomImage : roomImages) {
            // 이 이미지가 어느 객실의 것인지 알아낸다.
            // (1)에서 객실들을 이미 가져왔기 때문에 여기서 DB 조회가 더 나가지 않는다.
            Long roomId = roomImage.getRoom().getId();

            // computeIfAbsent: 이 객실 번호의 목록이 아직 없으면 빈 목록을 새로 만들어 넣고, 있으면 그 목록을 꺼낸다.
            // 그렇게 얻은 목록에 이미지 주소를 하나 추가한다.
            // (roomImages 가 id 오름차순으로 왔으므로 객실별 목록도 자연히 id 오름차순이 된다)
            imageUrlsByRoomId.computeIfAbsent(roomId, key -> new ArrayList<>()).add(roomImage.getUrl());
        }

        // (4) 이 숙소의 모든 객실 편의시설 연결 정보를 한 번에 가져온다.
        List<RoomFeature> roomFeatures = roomFeatureRepository.findByAccommodationId(accommodationId);

        // (5) 이미지와 같은 방법으로 "객실 번호 -> 그 객실의 편의시설 목록" 모양으로 나눠 담는다.
        Map<Long, List<FeatureInfo>> featuresByRoomId = new HashMap<>();
        for (RoomFeature roomFeature : roomFeatures) {
            // 연결 행의 기본키는 (객실 번호, 편의시설 번호) 묶음이라, 그 묶음 안에서 객실 번호를 바로 꺼낼 수 있다.
            Long roomId = roomFeature.getId().getRoom_id();

            // 편의시설 엔티티를 응답 모양으로 바꿔서 그 객실의 목록에 추가한다.
            // (조회문에서 join fetch 로 편의시설을 함께 가져왔기 때문에 여기서 DB 조회가 더 나가지 않는다)
            featuresByRoomId.computeIfAbsent(roomId, key -> new ArrayList<>())
                    .add(FeatureInfo.of(roomFeature.getFeature()));
        }

        // (6) 객실 엔티티를 하나씩 응답 모양(RoomInfo)으로 바꾼다.
        List<RoomInfo> roomInfos = new ArrayList<>();
        for (Room room : rooms) {
            // getOrDefault: 이 객실의 목록이 사전에 있으면 그것을, 없으면 빈 목록(List.of())을 쓴다.
            // 이유: 이미지나 편의시설이 하나도 없는 객실은 사전에 들어 있지 않다.
            //       그대로 꺼내면 null 이 나오는데, 명세는 "없으면 빈 배열"이라서 빈 목록으로 바꿔 준다.
            List<String> roomImageUrls = imageUrlsByRoomId.getOrDefault(room.getId(), List.of());
            List<FeatureInfo> roomFeatureInfos = featuresByRoomId.getOrDefault(room.getId(), List.of());
            roomInfos.add(RoomInfo.of(room, roomImageUrls, roomFeatureInfos));
        }
        return roomInfos;
    }
}
