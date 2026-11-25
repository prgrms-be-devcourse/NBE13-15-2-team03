package com.kdt.reservation.accommodation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 숙소(accommodation) 테이블에서 데이터를 꺼내고 저장하는 창구.
 *
 * [interface 인데 구현 코드가 없는 이유]
 * JpaRepository 를 이어받으면(extends) 스프링이 앱을 켤 때 구현 코드를 대신 만들어 준다.
 * 그래서 save(저장), findById(번호로 찾기) 같은 기본 기능은 한 줄도 쓰지 않고 바로 쓸 수 있다.
 *
 * [JpaRepository<Accommodation, Long> 의 뜻]
 * "Accommodation 엔티티를 다루고, 그 기본키(id)의 타입은 Long 이다."
 */
public interface AccommodationRepository extends JpaRepository<Accommodation, Long> {

    /**
     * 삭제되지 않은 숙소 한 건을 번호(id)로 찾는다. 숙소 주인(host) 정보도 함께 가져온다.
     *
     * [@Query 를 쓴 이유]
     * 원래는 메서드 "이름"만 규칙대로 지어도 스프링이 조회문을 만들어 준다.
     * 그런데 우리 엔티티의 필드 이름이 deleted_at 처럼 밑줄(_)을 포함하고 있어서,
     * 스프링이 밑줄을 "하위 객체로 들어가라"는 뜻으로 잘못 해석해 앱이 켜지지 않는다. (PR #34 에서 공유된 문제)
     * 그래서 조회문(JPQL)을 직접 적었다.
     *
     * [조회문 읽는 법] JPQL 은 DB 테이블이 아니라 "엔티티 클래스 이름과 필드 이름"으로 쓴다.
     * - select a from Accommodation a : Accommodation 엔티티를 a 라는 별명으로 조회한다.
     * - join fetch a.user             : 숙소 주인(Users)도 같은 조회에서 한 번에 가져온다.
     *     이유: Accommodation.user 는 LAZY(필요해질 때 따로 조회) 설정이다. 이렇게 하지 않으면
     *           주인 닉네임을 꺼내는 순간 DB 조회가 한 번 더 나간다.
     *           응답에 주인 정보가 항상 들어가므로 처음부터 한 번에 가져오는 편이 낫다.
     * - where a.id = :id              : 번호가 일치하는 숙소만. (:id 자리에 아래 @Param("id") 값이 들어간다)
     * - and a.deleted_at is null      : 삭제 시각이 비어 있는(= 삭제되지 않은) 숙소만.
     *     이유: 우리 서비스는 삭제할 때 행을 지우지 않고 deleted_at 에 시각만 적는다(soft delete).
     *           그래서 조회할 때 "삭제되지 않은 것만"이라는 조건을 직접 붙여야 한다. (명세 6.2: 삭제된 숙소는 404)
     *
     * [반환 타입이 Optional 인 이유]
     * 찾는 숙소가 없을 수도 있다. Optional 은 "값이 있을 수도, 없을 수도 있는 상자"다.
     * 이 상자로 돌려주면 호출한 쪽(서비스)이 "없을 때 어떻게 할지"를 빠뜨리지 않고 처리하게 된다.
     */
    @Query("select a from Accommodation a join fetch a.user where a.id = :id and a.deleted_at is null")
    Optional<Accommodation> findActiveById(@Param("id") Long id);

    /**
     * 삭제되지 않은 숙소가 "있는지 없는지"만 확인한다. (리뷰 담당 D 가 추가, 숙소 리뷰 목록 API 에서 사용)
     *
     * [위의 findActiveById 와 따로 둔 이유]
     * 숙소 내용은 필요 없고 존재 여부만 알면 될 때는, 내용을 다 가져오는 것보다 개수만 세는 편이 가볍다.
     * count(a) > 0 : 조건에 맞는 숙소가 1개 이상이면 true, 없으면 false 가 된다.
     */
    @Query("""
            select count(a) > 0 from Accommodation a
            where a.id = :id
              and a.deleted_at is null
            """)
    boolean existsActiveById(@Param("id") Long id);
}
