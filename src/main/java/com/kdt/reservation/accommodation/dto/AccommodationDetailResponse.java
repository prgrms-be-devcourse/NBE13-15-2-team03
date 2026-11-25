package com.kdt.reservation.accommodation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kdt.reservation.accommodation.Accommodation;
import com.kdt.reservation.accommodation.Type;
import com.kdt.reservation.common.Feature;
import com.kdt.reservation.common.FeatureCategory;
import com.kdt.reservation.room.Grade;
import com.kdt.reservation.room.Room;
import com.kdt.reservation.user.Users;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 숙소 상세 조회(GET /api/accommodations/{id}) 응답의 data 부분 모양. (명세 6.2)
 *
 * [DTO 를 따로 만드는 이유]
 * 엔티티(Accommodation)를 그대로 응답으로 내보내지 않는다.
 * - 엔티티에는 응답에 필요 없는 값(deleted_at 등)이 있고, 주인(user) 안에는 비밀번호 같은 값도 있다.
 * - 엔티티 필드 이름(address_detail)과 명세의 응답 이름(addressDetail)이 다르다.
 * 그래서 "밖으로 보여 줄 값만, 명세의 이름으로" 담는 전용 상자(DTO)를 만든다.
 *
 * [record 를 쓴 이유]
 * record 는 "값을 담기만 하는 클래스"를 짧게 쓰는 자바 문법이다.
 * 괄호 안에 필드만 적으면 생성자와 값을 꺼내는 메서드가 자동으로 만들어진다.
 * 한 번 만들면 값을 바꿀 수 없어서, 응답처럼 "만들어서 내보내기만 하는" 용도에 잘 맞는다.
 *
 * [괄호 안 필드 이름과 순서가 중요한 이유]
 * 여기 적은 이름이 그대로 JSON 의 키 이름이 되고, 적은 순서대로 JSON 에 나간다.
 * 그래서 명세 6.2의 이름·순서와 똑같이 적었다.
 */
public record AccommodationDetailResponse(
        Long accommodationId,       // 숙소 번호
        HostInfo host,              // 숙소 주인 정보 (아래에 정의한 작은 상자)
        String name,                // 숙소 이름
        String description,         // 숙소 설명 (없으면 null)
        Type type,                  // 숙소 유형. enum 은 JSON 에서 "HOTEL" 같은 글자로 나간다
        String region,              // 지역
        String address,             // 주소
        String addressDetail,       // 상세 주소
        List<String> imageUrls,     // 이미지 주소 목록. 첫 번째가 대표 이미지, 없으면 빈 목록
        List<FeatureInfo> features, // 숙소 편의시설 목록. 없으면 빈 목록
        double averageRating,       // 평균 평점. 소수 첫째 자리까지, 리뷰가 없으면 0.0
        long reviewCount,           // 리뷰 수 (삭제된 리뷰 제외)
        List<RoomInfo> rooms,       // 객실 목록. 삭제된 객실은 빠지고 판매 중지 객실은 포함. 없으면 빈 목록
        LocalDateTime createdAt     // 등록 일시
) {

    /**
     * 숙소 주인 정보. 명세에서 "host": { "hostId": 3, "nickname": "바다사장" } 모양이라
     * host 안에 들어갈 작은 상자를 따로 만들었다.
     *
     * [주인의 번호와 닉네임만 담는 이유]
     * Users 엔티티에는 비밀번호, 전화번호 같은 개인 정보가 있다.
     * 공개 API 에서 그런 값이 나가면 안 되므로 필요한 두 값만 꺼내 담는다.
     */
    public record HostInfo(Long hostId, String nickname) {
    }

    /**
     * 편의시설 한 개의 정보. 명세 6.2 의 features[] 안에 들어가는 모양이다.
     * 숙소 편의시설과 객실 편의시설이 같은 모양이라 이 상자 하나를 두 곳에서 같이 쓴다.
     *
     * [target(숙소용/객실용) 을 담지 않는 이유]
     * 명세 6.2 의 features[] 에는 target 항목이 없다. 숙소 밑에 있으면 숙소용, 객실 밑에 있으면 객실용이라
     * 위치만 봐도 알 수 있기 때문이다. (target 은 편의시설 목록 API 6.4 에서만 내보낸다)
     */
    public record FeatureInfo(
            Long featureId,             // 편의시설 번호. 등록·수정·필터에서 이 번호를 쓴다
            String code,                // 고유 코드 (예: PARKING)
            String name,                // 화면에 보여 줄 이름 (예: 주차장)
            FeatureCategory category    // 분류 (AMENITY, FACILITY, SERVICE)
    ) {

        /**
         * 편의시설 엔티티를 응답 상자로 바꾼다.
         * 엔티티의 필드 이름은 featureCategory 지만, 명세의 응답 이름은 category 라서 여기서 이름을 맞춘다.
         */
        public static FeatureInfo of(Feature feature) {
            return new FeatureInfo(
                    feature.getId(),
                    feature.getCode(),
                    feature.getName(),
                    feature.getFeatureCategory()
            );
        }
    }

    /**
     * 객실 한 개의 정보. 명세 6.2 의 rooms[] 안에 들어가는 모양이다.
     *
     * [바깥 record 안에 넣어 둔 이유]
     * 지금은 숙소 상세 응답에서만 쓰는 모양이라, 한 파일 안에 모아 두는 편이 찾기 쉽다.
     */
    public record RoomInfo(
            Long roomId,                // 객실 번호
            String name,                // 객실 이름
            String description,         // 객실 설명 (없으면 null)
            int price,                  // 1박 가격
            Grade grade,                // 객실 등급 (STANDARD, DELUXE, SUITE)
            int standardCapacity,       // 기준 인원
            int maxCapacity,            // 최대 인원
            int extraPersonFee,         // 기준 인원을 넘을 때 1인당 추가 요금

            // 판매 여부. false 면 예약할 수 없다.
            // [@JsonProperty("isAvailable") 를 붙인 이유]
            // 자바에서 boolean 값은 is 로 시작하는 이름을 특별하게 취급해서,
            // JSON 으로 바꿀 때 앞의 is 가 떨어져 "available" 로 나갈 수 있다.
            // 명세의 이름은 "isAvailable" 이므로, 이 이름으로 내보내라고 직접 정해 둔다.
            @JsonProperty("isAvailable") boolean isAvailable,

            List<String> imageUrls,     // 객실 이미지 주소 목록. id 오름차순, 없으면 빈 목록
            List<FeatureInfo> features  // 객실 편의시설 목록. 없으면 빈 목록
    ) {

        /**
         * 객실 엔티티를 응답 상자로 바꾼다.
         *
         * [getStandard_capacity() 처럼 이름에 밑줄이 있는 이유]
         * 엔티티 필드 이름이 standard_capacity 이고, 롬복(@Getter)이 그 이름 앞에 get 을 붙여 만들기 때문이다.
         * [room.isAvailable() 만 get 이 아니라 is 로 시작하는 이유]
         * 롬복은 boolean 필드의 꺼내는 메서드를 get 대신 is 로 만든다. (엔티티의 필드 이름은 available)
         */
        public static RoomInfo of(Room room, List<String> imageUrls, List<FeatureInfo> features) {
            // 아래 값의 순서는 위 record 괄호 안에 적은 필드 순서와 같아야 한다.
            return new RoomInfo(
                    room.getId(),
                    room.getName(),
                    room.getDescription(),
                    room.getPrice(),
                    room.getGrade(),
                    room.getStandard_capacity(),
                    room.getMax_capacity(),
                    room.getExtra_person_fee(),
                    room.isAvailable(),
                    imageUrls,
                    features
            );
        }
    }

    /**
     * 엔티티(DB 에서 꺼낸 값)를 응답 상자(DTO)로 바꾸는 메서드.
     *
     * [static 메서드로 만든 이유]
     * "바꾸는 방법"을 이 DTO 안 한 곳에 모아 두면, 서비스 코드에서는
     * AccommodationDetailResponse.of(...) 한 줄만 쓰면 되어 읽기 쉽다.
     *
     * [getAddress_detail(), getCreated_at() 처럼 이름에 밑줄이 있는 이유]
     * 엔티티 필드 이름이 address_detail, created_at 이고,
     * 롬복(@Getter)이 필드 이름 앞에 get 을 붙여 꺼내는 메서드를 만들어 주기 때문이다.
     *
     * @param accommodation 숙소 엔티티 (주인 정보가 함께 조회되어 있어야 한다)
     * @param imageUrls     숙소 이미지 주소 목록
     * @param features      숙소 편의시설 목록 (이미 응답 모양으로 바꾼 것)
     * @param averageRating 평균 평점 (이미 소수 첫째 자리로 맞춘 값)
     * @param reviewCount   리뷰 수
     * @param rooms         객실 목록 (이미 응답 모양으로 바꾼 것)
     */
    public static AccommodationDetailResponse of(Accommodation accommodation,
                                                 List<String> imageUrls,
                                                 List<FeatureInfo> features,
                                                 double averageRating,
                                                 long reviewCount,
                                                 List<RoomInfo> rooms) {
        // 숙소 주인. 레포지토리에서 join fetch 로 함께 가져왔기 때문에 여기서 DB 조회가 더 나가지 않는다.
        Users host = accommodation.getUser();

        // 아래 값의 순서는 위 record 괄호 안에 적은 필드 순서와 같아야 한다.
        return new AccommodationDetailResponse(
                accommodation.getId(),
                new HostInfo(host.getId(), host.getNickname()),
                accommodation.getName(),
                accommodation.getDescription(),
                accommodation.getType(),
                accommodation.getRegion(),
                accommodation.getAddress(),
                accommodation.getAddress_detail(),
                imageUrls,
                features,
                averageRating,
                reviewCount,
                rooms,
                accommodation.getCreated_at()
        );
    }
}
