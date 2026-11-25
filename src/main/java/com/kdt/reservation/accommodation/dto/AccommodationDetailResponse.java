package com.kdt.reservation.accommodation.dto;

import com.kdt.reservation.accommodation.Accommodation;
import com.kdt.reservation.accommodation.Type;
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
 * [괄호 안 필드 이름이 중요한 이유]
 * 여기 적은 이름이 그대로 JSON 의 키 이름이 된다. 그래서 명세 6.2의 이름과 똑같이 적었다.
 *
 * [현재 범위] 1단계: 숙소 기본 정보 + 이미지.
 * 편의시설(features), 평점(averageRating, reviewCount), 객실(rooms)은 다음 단계에서 추가한다.
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
     * 엔티티(DB 에서 꺼낸 값)를 응답 상자(DTO)로 바꾸는 메서드.
     *
     * [static 메서드로 만든 이유]
     * "바꾸는 방법"을 이 DTO 안 한 곳에 모아 두면, 서비스 코드에서는
     * AccommodationDetailResponse.of(...) 한 줄만 쓰면 되어 읽기 쉽다.
     *
     * [getAddress_detail(), getCreated_at() 처럼 이름에 밑줄이 있는 이유]
     * 엔티티 필드 이름이 address_detail, created_at 이고,
     * 롬복(@Getter)이 필드 이름 앞에 get 을 붙여 꺼내는 메서드를 만들어 주기 때문이다.
     */
    public static AccommodationDetailResponse of(Accommodation accommodation, List<String> imageUrls) {
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
                accommodation.getCreated_at()
        );
    }
}
