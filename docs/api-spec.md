# 숙소 예약 서비스 API 명세

- 작성일: 2026-11-23 (최종 수정 2026-11-24, 확정 ERD와 팀 결정 반영)
- 기준 문서: `docs/erd.md`, `db/01_schema.sql` (테이블 15개: USERS, SOCIAL_USER, ACCOMMODATION, ACCOMMODATION_IMAGE, ROOM, ROOM_IMAGE, FEATURE, ACCOMMODATION_FEATURE, ROOM_FEATURE, RESERVATION, PAYMENT, POINT, POINT_HISTORY, REVIEW, REVIEW_IMAGE)
- 범위: 엔드포인트 36개 (JSON API 34개, Spring Security가 처리하는 소셜 로그인 경로 2개)
- 명세 작성: 김진형
- 담당 표기: A 임재상(인증·회원·소셜 로그인·보안 설정), B 김진형(숙소·객실), C 천승혁(예약·결제·예약 일정 조회), D 최건(포인트·리뷰·이미지 업로드·공통 모듈)

## 0. 가정한 것 (팀 확정 필요)

아래 항목은 아직 정해지지 않아 임의로 정했다. 확정되면 0-1로 옮기고 해당 절을 고친다. 번호는 처음 매긴 그대로 두며, 빠진 번호는 0-1에 있다.

| # | 항목 | 이 문서의 가정 | 관련 절 |
|---|---|---|---|
| 2 | 토큰 전달과 만료 | 응답 본문으로 전달. Access 30분, Refresh 14일 | 3.2, 3.3 |
| 5 | 소셜 로그인 콜백 응답 | 리다이렉트 없이 JSON 본문으로 토큰 전달 | 4.2 |
| 6 | 가입 입력 규칙 | 아이디 영문 소문자·숫자 4~20자, 비밀번호 8~64자(영문과 숫자 포함) | 3.1 |
| 7 | enum 값 | `accommodation.type`은 확정(11/23 팀 합의). `room.grade`, `payment.method`는 ERD에 임시로 표기돼 있다. 값은 1.6에 있다 | 1.6 |
| 8 | `region` 값 | 시도 단위 고정 값 17개 (`서울`, `부산`, `제주` 등) | 6, 7 |
| 9 | `address_detail` 필수 여부 | ERD대로 필수 | 7.2 |
| 10 | 숙소 목록 정렬 | `LATEST`(기본), `PRICE_ASC`, `PRICE_DESC` | 6.1 |
| 11 | 예약 금액 계산 | `price × 박 수 + extra_person_fee × 초과 인원 × 박 수` | 9.1 |
| 12 | PENDING 예약 만료 | 생성 후 10분 안에 결제하지 않으면 자동 취소 | 9.1, 10.1 |
| 13 | 예약 취소 가능 시점 | 체크인 전날까지. 체크인 당일부터는 불가 | 9.4 |
| 15 | 결제의 사용·적립 포인트 | `PAYMENT`에 포인트 컬럼이 없다. 응답의 `usedPoint`, `earnedPoint`는 `POINT_HISTORY`에서 `reservation_id`와 `type`으로 찾는다. `PAYMENT.used_point` 컬럼을 추가하면 조회가 단순해진다 | 9.3, 10 |
| 20 | 취소 시 적립 포인트 | 회수하지 않는다. ERD의 내역 유형이 `EARN`, `USE`, `REFUND`뿐이라 회수를 기록할 수 없다. **이대로면 결제와 취소를 반복해 포인트를 쌓을 수 있다.** 회수 유형을 추가하거나 적립 시점을 체크아웃 이후로 옮겨야 한다 | 9.4, 11.2 |

## 0-1. 확정한 결정

2026-11-24 ERD 확정에 맞춰 정한 것이다. 각 담당자는 구현 전에 자기 담당 행을 확인한다. `원 #`은 0절에서 쓰던 번호다.

| 원 # | 항목 | 결정 | 관련 절 | 담당 |
|---|---|---|---|---|
| 1 | 공통 응답 포맷 | 성공 응답은 `status`와 `data`만 담는다. `message`는 에러 응답에만 둔다. 페이지 포맷은 1.3 그대로 | 1.1~1.3, 전체 | D |
| 3 | Refresh 토큰 저장 | 서버 인메모리에 저장하며 테이블은 없다. 사용자당 1개, 재발급 때 교체. 서버 인스턴스를 늘릴 때 Redis로 옮긴다 | 3.3, 3.4 | A |
| 4 | 소셜 전용 유저의 컬럼 | `login_id`, `password`, `phone`, `birth_date`는 모두 NOT NULL을 유지하고, 소셜 가입 시 서버가 임시 값을 넣는다. `login_id` = `{provider}_{providerUserId}` (예: `kakao_123456`), `password` = 랜덤 문자열의 BCrypt 해시, `phone` = 유니크한 임시 값(형식은 A가 구현에서 정함), `birth_date` = `1900-01-01`. 역할은 GUEST 고정. **소셜 전용 계정 판단 기준**: `SOCIAL_USER` 연결이 있고 `login_id`가 서버 생성 형식인 계정. `phone`, `birth_date`가 임시 값이면 응답에서는 null로 돌려주고 5.2로 채우게 한다 | 4, 5 | A |
| 14 | 포인트 규칙 | 1P = 1원, 1P 단위 사용. 결제 시 결제 금액(`amount`)의 5%를 적립하고 소수점 이하는 버린다. 예약 취소 시 사용분 환불. `POINT_HISTORY.amount`는 항상 양수로 저장 | 10, 11 | D |
| 16 | 리뷰 재작성 | 삭제한 리뷰의 예약에는 다시 쓸 수 없다(ERD 설계 메모). 이미지는 리뷰당 최대 5장 | 12 | D |
| 17 | 이미지 저장 | 저장 위치는 Cloud Storage(S3). 업로드 기능을 구현하기 전에는 랜덤 이미지 URL을 `imageUrls`에 넣어 쓴다. jpg, png, webp. 파일당 5MB 이하 | 12.6 | D |
| 18 | 탈퇴한 아이디·전화번호 | 재사용하지 않는다. `login_id`, `phone`이 UK이고 탈퇴가 soft delete라 현 스키마로는 재사용할 수 없다. 체크아웃 전인 예약이 있으면 탈퇴 거절 | 5.4 | A |
| 19 | `birth_date` | 유지한다(ERD에 NOT NULL) | 3.1, 4.2, 5 | A |
| 21 | 편의시설 데이터 | `FEATURE` 행은 `db/01_schema.sql`로 관리하고 API로는 만들거나 고치지 않는다. 선택할 수 있는 목록을 주는 `GET /api/features`(6.4)는 1차 목록에 없던 추가 API다 | 6.4 | B |
| 22 | 숙소·객실 이미지 | 각각 최대 10장. ERD에 순서나 대표 컬럼이 없으므로 id 오름차순 첫 번째 이미지를 대표(`thumbnailUrl`)로 쓴다. 수정은 목록 전체 교체(12.4와 같은 방식) | 6, 7, 8 | B |
| 23 | 편의시설 필터와 식별자 | 요청과 필터는 `featureId`(숫자)로 받는다. 숙소 목록은 숙소 편의시설로만 거르고, 여러 개를 보내면 모두 가진 숙소만(AND) 포함한다. 객실용 id를 보내면 400 `INVALID_FEATURE`. 수정은 목록 전체 교체 | 6.1, 7, 8 | B |
| 24 | 예약당 포인트 내역 | 한 예약에 `USE`, `EARN`, `REFUND` 내역은 유형별로 최대 1건이다. 같은 유형을 다시 만들려 하면 500 `POINT_HISTORY_ALREADY_EXISTS` | 9.4, 10.1, 11 | D |
| 25 | POINT 행 생성 시점 | 회원가입 시 잔액 0인 `POINT` 행을 회원 저장과 같은 트랜잭션에서 만든다. 포인트 사용·적립·환불 때 행이 없으면 500 `POINT_NOT_FOUND` | 3.1, 9.4, 10.1, 11 | D |

## 1. 공통

- Base URL: `/api` (소셜 로그인 경로 2개는 예외)
- 요청·응답 본문: JSON, 필드명은 camelCase (DB 컬럼은 snake_case)
- 인증: `Authorization: Bearer <access token>`. 권한이 `공개`인 API는 헤더가 필요 없다.
- 날짜: `YYYY-MM-DD`, 일시: ISO 8601 (`2026-11-23T14:00:00`)
- 금액과 포인트: 정수 (원, P)
- 삭제: 모두 soft delete. 삭제된 데이터는 조회에서 제외하고 404로 응답한다.

**권한 표기**

| 표기 | 의미 |
|---|---|
| 공개 | 인증 불필요 |
| 로그인 | 역할과 무관하게 인증 필요 |
| GUEST, HOST | 해당 역할만 가능. 다른 역할이면 403 `FORBIDDEN` |
| 본인 | 해당 리소스의 소유자만 가능 |

### 1.1 성공 응답

```json
{
  "status": 200,
  "data": { }
}
```

돌려줄 내용이 없으면 `data`는 null이다.

### 1.2 에러 응답

```json
{
  "status": 404,
  "error": "ACCOMMODATION_NOT_FOUND",
  "message": "숙소를 찾을 수 없습니다.",
  "timestamp": "2026-11-23T14:00:00"
}
```

입력값 검증 실패(`INVALID_INPUT`)는 어떤 필드가 틀렸는지 `errors` 배열로 함께 알려준다.

```json
{
  "status": 400,
  "error": "INVALID_INPUT",
  "message": "입력값이 올바르지 않습니다.",
  "errors": [
    { "field": "name", "reason": "숙소명은 1~50자여야 합니다." }
  ],
  "timestamp": "2026-11-23T14:00:00"
}
```

### 1.3 페이지 응답

목록 API는 쿼리 파라미터 `page`(기본 0), `size`(기본 20, 1~100)를 받고, `data`를 아래 구조로 돌려준다.

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "totalPages": 7,
  "hasNext": true
}
```

### 1.4 상태 코드

| 코드 | 쓰는 경우 |
|---|---|
| 200 | 조회, 수정, 삭제, 취소 성공 |
| 201 | 생성 성공 |
| 400 | 입력값 검증 실패, 조건 불충족 |
| 401 | 인증 실패 (토큰 없음·만료·위조, 로그인 실패) |
| 403 | 권한 없음 (역할 불일치, 남의 리소스) |
| 404 | 대상 없음 |
| 409 | 현재 상태와 충돌 (중복, 날짜 겹침, 이미 처리됨) |
| 413 | 업로드 파일이 너무 큼 |
| 500 | 서버 내부 오류 (데이터 정합성 위반) |

### 1.5 에러 코드

| 영역 | HTTP | 코드 | 발생 조건 |
|---|---|---|---|
| 공통 | 400 | `INVALID_INPUT` | 필수 값 누락, 길이·범위 위반, 허용되지 않은 enum 값 |
| 공통 | 401 | `UNAUTHORIZED` | Access 토큰 없음, 만료, 위조 |
| 공통 | 403 | `FORBIDDEN` | 필요한 역할이 아님 |
| 인증 | 401 | `LOGIN_FAILED` | 아이디 또는 비밀번호 불일치, 탈퇴한 계정 |
| 인증 | 401 | `INVALID_REFRESH_TOKEN` | Refresh 토큰 만료, 위조, 폐기됨 |
| 인증 | 409 | `DUPLICATE_LOGIN_ID` | 이미 사용 중인 아이디 |
| 회원 | 409 | `DUPLICATE_PHONE` | 이미 사용 중인 전화번호 |
| 회원 | 400 | `PASSWORD_MISMATCH` | 현재 비밀번호 불일치 |
| 회원 | 400 | `PASSWORD_NOT_SET` | 비밀번호를 직접 설정한 적 없는 소셜 전용 계정. 판단 기준은 0-1의 4번 |
| 회원 | 409 | `USER_HAS_ACTIVE_RESERVATION` | 체크아웃 전인 예약이 남아 있어 탈퇴 불가 |
| 숙소 | 400 | `INVALID_DATE_RANGE` | 날짜가 하나만 옴, 시작이 끝과 같거나 늦음, 과거 날짜 |
| 숙소 | 400 | `INVALID_PRICE_RANGE` | `minPrice`가 `maxPrice`보다 큼 |
| 숙소 | 403 | `NOT_ACCOMMODATION_OWNER` | 다른 HOST의 숙소·객실에 접근 |
| 숙소 | 404 | `ACCOMMODATION_NOT_FOUND` | 숙소가 없거나 삭제됨 |
| 숙소 | 409 | `ACCOMMODATION_HAS_ACTIVE_RESERVATION` | 체크아웃 전인 예약이 있는 숙소를 삭제하려 함 |
| 숙소·객실 | 400 | `INVALID_FEATURE` | 없는 편의시설 ID이거나, 대상이 맞지 않음(숙소에 객실용 ID, 객실에 숙소용 ID) |
| 객실 | 400 | `INVALID_CAPACITY` | `maxCapacity`가 `standardCapacity`보다 작음 |
| 객실 | 404 | `ROOM_NOT_FOUND` | 객실이 없거나 삭제됨 |
| 객실 | 409 | `ROOM_HAS_ACTIVE_RESERVATION` | 체크아웃 전인 예약이 있는 객실을 삭제하려 함 |
| 예약 | 400 | `GUEST_COUNT_EXCEEDED` | 인원이 객실 최대 인원을 넘음 |
| 예약 | 403 | `RESERVATION_ACCESS_DENIED` | 예약자도 숙소 주인도 아님 |
| 예약 | 404 | `RESERVATION_NOT_FOUND` | 예약이 없음 |
| 예약 | 409 | `ROOM_NOT_AVAILABLE` | 판매 중지된 객실 |
| 예약 | 409 | `RESERVATION_DATE_CONFLICT` | 같은 객실에 날짜가 겹치는 예약이 있음 |
| 예약 | 409 | `RESERVATION_ALREADY_CANCELLED` | 이미 취소된 예약 |
| 예약 | 409 | `CANCEL_PERIOD_EXPIRED` | 취소 가능 시점이 지남 |
| 결제 | 400 | `INVALID_POINT_AMOUNT` | 사용 포인트가 음수이거나 결제 금액보다 큼 |
| 결제 | 409 | `RESERVATION_NOT_PAYABLE` | 예약이 PENDING이 아님 (이미 결제, 취소, 만료) |
| 결제 | 409 | `INSUFFICIENT_POINT` | 보유 포인트 부족 |
| 결제 | 500 | `POINT_HISTORY_ALREADY_EXISTS` | 같은 예약에 같은 유형(`USE`, `EARN`, `REFUND`)의 포인트 내역이 이미 있음. 서버 오류이며 정상 흐름에서는 나오지 않는다 |
| 결제 | 500 | `POINT_NOT_FOUND` | 회원의 `POINT` 행이 없음. 가입 시 만들므로 서버 오류이며 정상 흐름에서는 나오지 않는다 |
| 리뷰 | 400 | `REVIEW_NOT_ALLOWED` | 예약이 CONFIRMED가 아니거나 체크아웃 전 |
| 리뷰 | 403 | `NOT_REVIEW_AUTHOR` | 다른 사람의 리뷰 |
| 리뷰 | 404 | `REVIEW_NOT_FOUND` | 리뷰가 없거나 삭제됨 |
| 리뷰 | 409 | `REVIEW_ALREADY_EXISTS` | 같은 예약에 리뷰가 이미 있음 |
| 이미지 | 400 | `INVALID_FILE_TYPE` | 허용되지 않은 파일 형식 |
| 이미지 | 413 | `FILE_TOO_LARGE` | 파일이 5MB를 넘음 |

### 1.6 Enum 값

2026-11-23 팀 합의로 확정한 값이다. DB에는 VARCHAR로 저장하고(`@Enumerated(EnumType.STRING)`), 요청과 응답에서도 같은 대문자 문자열을 쓴다. 정의되지 않은 값을 보내면 400 `INVALID_INPUT`이다.

| 컬럼 | API 필드 | 값 |
|---|---|---|
| `accommodation.type` | `type` | `HOTEL`, `MOTEL`, `PENSION`, `RESORT`, `POOL_VILLA`, `CAMPING` |
| `room.grade` | `grade` | `STANDARD`, `DELUXE`, `SUITE` |
| `payment.method` | `method` | `CARD`, `BANK_TRANSFER`, `KAKAO_PAY` |
| `users.role` | `role` | `GUEST`, `HOST`, `ADMIN` |
| `social_user.provider` | `socialProviders` | `KAKAO`, `GOOGLE` |
| `reservation.status` | `status` | `PENDING`, `CONFIRMED`, `CANCELLED` |
| `payment.status` | `status` | `READY`, `PAID`, `FAILED`, `CANCELLED` |
| `point_history.type` | `type` | `EARN`, `USE`, `REFUND` |
| `feature.category` | `category` | `AMENITY`, `FACILITY`, `SERVICE` |
| `feature.target` | `target` | `ACCOMMODATION`, `ROOM` |

`feature`의 두 값은 11/24 수정 ERD에서 가져왔다. 소셜 로그인 URL의 `{provider}`만 소문자(`kakao`, `google`)다. Spring Security의 등록 ID를 그대로 쓰기 때문이다.

## 2. 엔드포인트 요약

| # | Method | URI | 설명 | 권한 | 담당 |
|---|---|---|---|---|---|
| 3.1 | POST | `/api/auth/signup` | 회원가입 | 공개 | A |
| 3.2 | POST | `/api/auth/login` | 로그인, 토큰 발급 | 공개 | A |
| 3.3 | POST | `/api/auth/reissue` | Access 토큰 재발급 | 공개 | A |
| 3.4 | POST | `/api/auth/logout` | 로그아웃 | 로그인 | A |
| 4.1 | GET | `/oauth2/authorization/{provider}` | 소셜 로그인 시작 | 공개 | A |
| 4.2 | GET | `/login/oauth2/code/{provider}` | 소셜 로그인 콜백, 토큰 발급 | 공개 | A |
| 5.1 | GET | `/api/users/me` | 내 정보 조회 | 로그인 | A |
| 5.2 | PATCH | `/api/users/me` | 내 정보 수정 | 로그인 | A |
| 5.3 | PATCH | `/api/users/me/password` | 비밀번호 변경 | 로그인 | A |
| 5.4 | DELETE | `/api/users/me` | 회원 탈퇴 | 로그인 | A |
| 6.1 | GET | `/api/accommodations` | 숙소 목록 조회·필터링·페이징 | 공개 | B |
| 6.2 | GET | `/api/accommodations/{id}` | 숙소 상세 | 공개 | B |
| 6.3 | GET | `/api/accommodations/{id}/reserved-dates` | 객실별 예약 불가 날짜 | 공개 | C |
| 6.4 | GET | `/api/features` | 편의시설 목록 | 공개 | B |
| 7.1 | GET | `/api/accommodations/me` | 내 숙소 목록 | HOST | B |
| 7.2 | POST | `/api/accommodations` | 숙소 등록 | HOST | B |
| 7.3 | PATCH | `/api/accommodations/{id}` | 숙소 수정 | HOST (본인) | B |
| 7.4 | DELETE | `/api/accommodations/{id}` | 숙소 삭제 | HOST (본인) | B |
| 8.1 | POST | `/api/accommodations/{id}/rooms` | 객실 등록 | HOST (본인) | B |
| 8.2 | PATCH | `/api/rooms/{roomId}` | 객실 수정, 판매 중지 | HOST (본인) | B |
| 8.3 | DELETE | `/api/rooms/{roomId}` | 객실 삭제 | HOST (본인) | B |
| 9.1 | POST | `/api/reservations` | 예약 생성 | GUEST | C |
| 9.2 | GET | `/api/reservations/me` | 내 예약 목록 | GUEST | C |
| 9.3 | GET | `/api/reservations/{id}` | 예약 상세 | 본인 또는 숙소 HOST | C |
| 9.4 | POST | `/api/reservations/{id}/cancel` | 예약 취소 | 본인 | C |
| 9.5 | GET | `/api/accommodations/{id}/reservations` | 숙소의 예약 목록 | HOST (본인) | C |
| 10.1 | POST | `/api/reservations/{id}/payments` | 결제 후 예약 확정 | 본인 | C |
| 10.2 | GET | `/api/reservations/{id}/payments` | 결제 내역 조회 | 본인 | C |
| 11.1 | GET | `/api/points/me` | 포인트 잔액 조회 | 로그인 | D |
| 11.2 | GET | `/api/points/me/history` | 포인트 내역 조회 | 로그인 | D |
| 12.1 | POST | `/api/reviews` | 리뷰 작성 | 예약 본인 | D |
| 12.2 | GET | `/api/accommodations/{id}/reviews` | 숙소 리뷰 목록, 평균 평점 | 공개 | D |
| 12.3 | GET | `/api/reviews/me` | 내 리뷰 목록 | 로그인 | D |
| 12.4 | PATCH | `/api/reviews/{id}` | 리뷰 수정 | 본인 | D |
| 12.5 | DELETE | `/api/reviews/{id}` | 리뷰 삭제 | 본인 | D |
| 12.6 | POST | `/api/images` | 이미지 업로드 (숙소·객실·리뷰 공용) | 로그인 | D |

`/api/accommodations/me`, `/api/reservations/me`, `/api/reviews/me`는 `/{id}` 경로와 겹친다. `{id}`를 숫자 타입으로 받으면 Spring이 고정 경로 `me`를 먼저 매칭하므로 충돌하지 않는다.

## 3. 인증

### 3.1 회원가입

`POST /api/auth/signup` · 권한: 공개

**요청 본문**

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `loginId` | string | 필수 | 영문 소문자·숫자 4~20자. 중복 불가 |
| `password` | string | 필수 | 8~64자. 영문과 숫자를 모두 포함 |
| `name` | string | 필수 | 1~50자 |
| `nickname` | string | 필수 | 2~20자 |
| `phone` | string | 필수 | 숫자만 10~11자리. 중복 불가 |
| `birthDate` | date | 필수 | 오늘보다 과거 |
| `role` | enum | 필수 | `GUEST` 또는 `HOST`. `ADMIN`은 가입으로 만들 수 없다 |

```json
{
  "loginId": "traveler01",
  "password": "pass1234word",
  "name": "김여행",
  "nickname": "여행자",
  "phone": "01012345678",
  "birthDate": "1998-05-17",
  "role": "GUEST"
}
```

**응답 201**

```json
{
  "status": 201,
  "data": { "userId": 7 }
}
```

비밀번호는 BCrypt 해시로 저장하고 어떤 응답에도 담지 않는다.

가입과 함께 잔액 0인 `POINT` 행을 만든다. 회원 저장과 같은 트랜잭션이라 포인트 생성에 실패하면 가입도 취소된다.

**에러**: 400 `INVALID_INPUT`, 409 `DUPLICATE_LOGIN_ID`, 409 `DUPLICATE_PHONE`

### 3.2 로그인

`POST /api/auth/login` · 권한: 공개

**요청 본문**

| 필드 | 타입 | 필수 |
|---|---|---|
| `loginId` | string | 필수 |
| `password` | string | 필수 |

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `tokenType` | string | 항상 `Bearer` |
| `accessToken` | string | API 호출에 쓰는 JWT |
| `accessTokenExpiresIn` | int | Access 토큰 만료까지 남은 초 |
| `refreshToken` | string | 재발급에 쓰는 토큰 |
| `user.userId` | long | 사용자 ID |
| `user.nickname` | string | 닉네임 |
| `user.role` | enum | `GUEST`, `HOST`, `ADMIN` |

```json
{
  "status": 200,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { "userId": 7, "nickname": "여행자", "role": "GUEST" }
  }
}
```

아이디와 비밀번호 중 어느 쪽이 틀렸는지는 알려주지 않는다.

**에러**: 400 `INVALID_INPUT`, 401 `LOGIN_FAILED`

### 3.3 토큰 재발급

`POST /api/auth/reissue` · 권한: 공개

Refresh 토큰으로 새 Access 토큰과 새 Refresh 토큰을 받는다. 기존 Refresh 토큰은 그 즉시 폐기한다.

**요청 본문**

| 필드 | 타입 | 필수 |
|---|---|---|
| `refreshToken` | string | 필수 |

**응답 200** `data`: 3.2와 같은 구조에서 `user`를 뺀 것.

```json
{
  "status": 200,
  "data": {
    "tokenType": "Bearer",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "accessTokenExpiresIn": 1800,
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**에러**: 400 `INVALID_INPUT`, 401 `INVALID_REFRESH_TOKEN`

### 3.4 로그아웃

`POST /api/auth/logout` · 권한: 로그인

서버에 저장된 그 사용자의 Refresh 토큰을 삭제한다. 요청 본문은 없다. 이미 발급된 Access 토큰은 만료 시각까지 유효하다.

**응답 200**: `data`는 null.

**에러**: 401 `UNAUTHORIZED`

## 4. 소셜 로그인

두 경로는 Spring Security OAuth2 Client의 기본 경로다. 컨트롤러를 만들지 않고 설정과 로그인 성공 핸들러로 구현한다. `provider`는 `kakao` 또는 `google`이다.

### 4.1 소셜 로그인 시작

`GET /oauth2/authorization/{provider}` · 권한: 공개

브라우저를 카카오 또는 구글의 로그인 화면으로 보낸다(302 리다이렉트). JSON 응답이 아니므로 브라우저 주소창에서 직접 연다.

### 4.2 소셜 로그인 콜백

`GET /login/oauth2/code/{provider}` · 권한: 공개

제공자가 로그인을 마친 사용자를 이 경로로 돌려보낸다. 클라이언트가 직접 호출하지 않는다.

- 처음 로그인한 사용자면 `USERS`와 `SOCIAL_USER` 행을 만든다. 역할은 GUEST, 닉네임은 제공자가 준 값이다. `login_id`, `password`, `phone`, `birth_date`는 NOT NULL이므로 0-1의 4번에 정한 임시 값으로 채운다.
- 이미 연결된 사용자면 그 계정으로 로그인한다.

**응답 200** `data`: 3.2와 같은 구조에 아래 필드를 더한다.

| 필드 | 타입 | 설명 |
|---|---|---|
| `isNewUser` | boolean | 이번 로그인으로 계정이 만들어졌으면 true |
| `profileCompleted` | boolean | `phone`과 `birthDate`가 모두 임시 값이 아니면 true. false면 5.2로 채운다 |

**에러**: 제공자 인증 실패 또는 사용자의 동의 취소 시 401 `UNAUTHORIZED`

## 5. 회원

### 5.1 내 정보 조회

`GET /api/users/me` · 권한: 로그인

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `userId` | long | 사용자 ID |
| `loginId` | string | 아이디. 소셜 전용 계정은 서버가 만든 값(예: `kakao_123456`) |
| `name` | string | 이름 |
| `nickname` | string | 닉네임 |
| `phone` | string, null | 전화번호. 소셜 가입 후 아직 입력하지 않았으면(임시 값) null |
| `birthDate` | date, null | 생년월일. 소셜 가입 후 아직 입력하지 않았으면(임시 값) null |
| `role` | enum | `GUEST`, `HOST`, `ADMIN` |
| `socialProviders` | array | 연결된 소셜 계정. 예: `["KAKAO"]`. 없으면 빈 배열 |
| `createdAt` | datetime | 가입 일시 |

```json
{
  "status": 200,
  "data": {
    "userId": 7,
    "loginId": "traveler01",
    "name": "김여행",
    "nickname": "여행자",
    "phone": "01012345678",
    "birthDate": "1998-05-17",
    "role": "GUEST",
    "socialProviders": [],
    "createdAt": "2026-11-25T09:00:00"
  }
}
```

**에러**: 401 `UNAUTHORIZED`

### 5.2 내 정보 수정

`PATCH /api/users/me` · 권한: 로그인

보낸 필드만 바꾼다. 아이디와 역할은 바꿀 수 없다. 소셜 가입 계정은 여기서 `phone`과 `birthDate`를 보내 임시 값을 실제 값으로 덮어쓴다.

**요청 본문** (모두 선택, 1개 이상 필요)

| 필드 | 타입 | 검증 |
|---|---|---|
| `name` | string | 1~50자 |
| `nickname` | string | 2~20자 |
| `phone` | string | 숫자만 10~11자리. 중복 불가 |
| `birthDate` | date | 오늘보다 과거 |

**응답 200** `data`: 수정된 내 정보. 5.1과 같은 구조.

**에러**: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 409 `DUPLICATE_PHONE`

### 5.3 비밀번호 변경

`PATCH /api/users/me/password` · 권한: 로그인

**요청 본문**

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `currentPassword` | string | 필수 | 현재 비밀번호와 일치 |
| `newPassword` | string | 필수 | 8~64자. 영문과 숫자를 모두 포함 |

변경에 성공하면 저장된 Refresh 토큰을 삭제해 다시 로그인하게 한다. 소셜 전용 계정(0-1의 4번 기준)은 비밀번호가 임시 값이므로 400 `PASSWORD_NOT_SET`으로 거절한다.

**응답 200**: `data`는 null.

**에러**: 400 `INVALID_INPUT`, 400 `PASSWORD_MISMATCH`, 400 `PASSWORD_NOT_SET`, 401 `UNAUTHORIZED`

### 5.4 회원 탈퇴

`DELETE /api/users/me` · 권한: 로그인

- soft delete다. `deleted_at`을 기록하고 Refresh 토큰을 삭제한다.
- GUEST는 체크아웃 전인 PENDING·CONFIRMED 예약이 있으면 탈퇴할 수 없다.
- HOST는 자기 숙소에 체크아웃 전인 PENDING·CONFIRMED 예약이 있으면 탈퇴할 수 없다. 탈퇴하면 숙소와 객실도 함께 soft delete한다.

**응답 200**: `data`는 null.

**에러**: 401 `UNAUTHORIZED`, 409 `USER_HAS_ACTIVE_RESERVATION`

## 6. 숙소 조회

### 6.1 숙소 목록 조회

`GET /api/accommodations` · 권한: 공개

삭제되지 않은 숙소를 조건에 맞게 걸러 페이지 단위로 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `region` | string | 선택 | - | 지역. 정확히 일치하는 숙소만 |
| `type` | enum | 선택 | - | 숙소 유형 |
| `featureIds` | long 목록 | 선택 | - | 숙소 편의시설 ID(6.4). 여러 개는 쉼표로 잇는다(`featureIds=1,2`) |
| `minPrice` | int | 선택 | - | 1박 최저 가격. 0 이상 |
| `maxPrice` | int | 선택 | - | 1박 최고 가격. `minPrice` 이상 |
| `guestCount` | int | 선택 | - | 투숙 인원. 1 이상 |
| `checkIn` | date | 선택 | - | 체크인 날짜. `checkOut`과 함께 보내야 한다 |
| `checkOut` | date | 선택 | - | 체크아웃 날짜. `checkIn`보다 뒤여야 한다 |
| `sort` | enum | 선택 | `LATEST` | `LATEST`, `PRICE_ASC`, `PRICE_DESC` |
| `page` | int | 선택 | 0 | 페이지 번호. 0부터 시작 |
| `size` | int | 선택 | 20 | 페이지 크기. 1~100 |

**필터 규칙**

- 가격, 인원, 날짜 조건은 객실 기준이다. 조건을 하나라도 보내면, **모든 조건을 동시에 만족하는 객실이 1개 이상 있는 숙소**만 결과에 포함한다.
- 조건 판단에는 삭제되지 않았고 `isAvailable`이 true인 객실만 쓴다.
- 가격: 객실 `price`가 `minPrice` 이상 `maxPrice` 이하.
- 인원: 객실 `maxCapacity`가 `guestCount` 이상.
- 날짜: 그 객실에 PENDING 또는 CONFIRMED 예약 중 `check_in < checkOut AND check_out > checkIn`인 것이 없어야 한다.
- 객실 조건을 하나도 보내지 않으면 객실이 없는 숙소도 결과에 포함한다.
- 가격 정렬은 숙소의 `minPrice` 기준이다.
- 편의시설 조건은 숙소 기준이고 객실 조건과 별개로 적용한다. 보낸 ID를 **모두** 가진 숙소만 포함한다(AND). 없는 ID나 객실용 ID를 보내면 400 `INVALID_FEATURE`다. 객실 편의시설로는 거르지 않는다.

날짜 필터는 `RESERVATION` 테이블을 읽어야 하므로 예약 담당(C)의 엔티티가 나온 뒤 구현한다. 나머지 필터를 먼저 완성한다.

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `accommodationId` | long | 숙소 ID |
| `name` | string | 숙소명 |
| `type` | enum | 숙소 유형 |
| `region` | string | 지역 |
| `address` | string | 주소 (상세 주소 제외) |
| `thumbnailUrl` | string, null | 대표 이미지 URL(id 오름차순 첫 이미지). 이미지가 없으면 null |
| `minPrice` | int, null | 판매 중인 객실의 최저 1박 가격. 객실이 없으면 null |
| `averageRating` | double | 평균 평점. 소수 첫째 자리까지. 리뷰가 없으면 0.0 |
| `reviewCount` | int | 리뷰 수 |

```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "accommodationId": 12,
        "name": "해운대 오션뷰 호텔",
        "type": "HOTEL",
        "region": "부산",
        "address": "부산 해운대구 해운대해변로 100",
        "thumbnailUrl": "/images/2026/11/a1b2c3.jpg",
        "minPrice": 89000,
        "averageRating": 4.5,
        "reviewCount": 37
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_DATE_RANGE`, 400 `INVALID_PRICE_RANGE`, 400 `INVALID_FEATURE`

### 6.2 숙소 상세 조회

`GET /api/accommodations/{id}` · 권한: 공개

숙소 정보와 이미지, 편의시설, 객실 목록, 평균 평점을 돌려준다.

**경로 변수**

| 이름 | 타입 | 설명 |
|---|---|---|
| `id` | long | 숙소 ID |

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `accommodationId` | long | 숙소 ID |
| `host.hostId` | long | 호스트의 사용자 ID |
| `host.nickname` | string | 호스트 닉네임 |
| `name` | string | 숙소명 |
| `description` | string, null | 숙소 설명 |
| `type` | enum | 숙소 유형 |
| `region` | string | 지역 |
| `address` | string | 주소 |
| `addressDetail` | string | 상세 주소 |
| `imageUrls` | array | 숙소 이미지 URL 목록. id 오름차순이며 첫 번째가 대표 이미지. 없으면 빈 배열 |
| `features[]` | array | 숙소 편의시설 목록. 없으면 빈 배열 |
| `features[].featureId` | long | 편의시설 ID |
| `features[].code` | string | 편의시설 코드 |
| `features[].name` | string | 화면에 보여 줄 이름 |
| `features[].category` | enum | `AMENITY`, `FACILITY`, `SERVICE` |
| `averageRating` | double | 평균 평점. 리뷰가 없으면 0.0 |
| `reviewCount` | int | 리뷰 수 |
| `rooms[]` | array | 삭제되지 않은 객실 목록. 판매 중지 객실도 포함한다 |
| `rooms[].roomId` | long | 객실 ID |
| `rooms[].name` | string | 객실명 |
| `rooms[].description` | string, null | 객실 설명 |
| `rooms[].price` | int | 1박 가격 |
| `rooms[].grade` | enum | 객실 등급 |
| `rooms[].standardCapacity` | int | 기준 인원 |
| `rooms[].maxCapacity` | int | 최대 인원 |
| `rooms[].extraPersonFee` | int | 기준 인원 초과 시 1인당 추가 요금 |
| `rooms[].isAvailable` | boolean | 판매 여부. false면 예약할 수 없다 |
| `rooms[].imageUrls` | array | 객실 이미지 URL 목록. id 오름차순. 없으면 빈 배열 |
| `rooms[].features[]` | array | 객실 편의시설 목록. 필드는 `features[]`와 같다 |
| `createdAt` | datetime | 등록 일시 |

```json
{
  "status": 200,
  "data": {
    "accommodationId": 12,
    "host": { "hostId": 3, "nickname": "바다사장" },
    "name": "해운대 오션뷰 호텔",
    "description": "해변까지 걸어서 3분 거리입니다.",
    "type": "HOTEL",
    "region": "부산",
    "address": "부산 해운대구 해운대해변로 100",
    "addressDetail": "본관",
    "imageUrls": ["/images/2026/11/a1b2c3.jpg", "/images/2026/11/d4e5f6.jpg"],
    "features": [
      { "featureId": 1, "code": "PARKING", "name": "주차장", "category": "FACILITY" },
      { "featureId": 2, "code": "POOL", "name": "수영장", "category": "FACILITY" }
    ],
    "averageRating": 4.5,
    "reviewCount": 37,
    "rooms": [
      {
        "roomId": 101,
        "name": "디럭스 더블",
        "description": "오션뷰, 더블 침대 1개",
        "price": 120000,
        "grade": "DELUXE",
        "standardCapacity": 2,
        "maxCapacity": 3,
        "extraPersonFee": 20000,
        "isAvailable": true,
        "imageUrls": ["/images/2026/11/r101a.jpg"],
        "features": [
          { "featureId": 21, "code": "BATHTUB", "name": "욕조", "category": "FACILITY" }
        ]
      }
    ],
    "createdAt": "2026-11-25T10:30:00"
  }
}
```

**에러**: 404 `ACCOMMODATION_NOT_FOUND`

### 6.3 객실별 예약 불가 날짜 조회

`GET /api/accommodations/{id}/reserved-dates` · 권한: 공개 · 구현 담당: C

숙소의 객실별로 이미 예약된 기간을 돌려준다. 예약 화면에서 선택할 수 없는 날짜를 표시하는 데 쓴다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `from` | date | 선택 | 오늘 | 조회 시작 날짜 |
| `to` | date | 선택 | `from` + 60일 | 조회 끝 날짜. `from`보다 뒤, `from`에서 90일 이내 |
| `roomId` | long | 선택 | - | 이 객실만 조회 |

**규칙**

- PENDING 또는 CONFIRMED 예약만 포함한다.
- 기간은 체크인 날짜부터 체크아웃 전날까지 숙박한다는 뜻이다. `checkOutDate` 당일에는 다른 예약이 체크인할 수 있다.
- 삭제된 객실은 제외한다. 판매 중지 객실은 포함한다.

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `accommodationId` | long | 숙소 ID |
| `from`, `to` | date | 실제 조회한 기간 |
| `rooms[].roomId` | long | 객실 ID |
| `rooms[].name` | string | 객실명 |
| `rooms[].reservedRanges[]` | array | 예약된 기간 목록. 체크인 날짜순 |
| `rooms[].reservedRanges[].checkInDate` | date | 체크인 날짜 |
| `rooms[].reservedRanges[].checkOutDate` | date | 체크아웃 날짜 |

```json
{
  "status": 200,
  "data": {
    "accommodationId": 12,
    "from": "2026-12-01",
    "to": "2027-01-30",
    "rooms": [
      {
        "roomId": 101,
        "name": "디럭스 더블",
        "reservedRanges": [
          { "checkInDate": "2026-12-24", "checkOutDate": "2026-12-26" },
          { "checkInDate": "2026-12-31", "checkOutDate": "2027-01-02" }
        ]
      }
    ]
  }
}
```

**에러**: 400 `INVALID_DATE_RANGE`, 404 `ACCOMMODATION_NOT_FOUND`, 404 `ROOM_NOT_FOUND`

### 6.4 편의시설 목록 조회

`GET /api/features` · 권한: 공개

숙소나 객실을 등록하거나 검색할 때 고를 수 있는 편의시설 목록을 돌려준다. 항목 수가 적어 페이징하지 않는다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `target` | enum | 선택 | `ACCOMMODATION` 또는 `ROOM`. 보내면 그 대상용 항목만 돌려준다 |

**응답 200** `data[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `featureId` | long | 편의시설 ID. 등록·수정(7.2, 7.3, 8.1, 8.2)과 필터(6.1)에 이 값을 쓴다 |
| `code` | string | 고유 코드(`PARKING`, `BATHTUB` 등) |
| `name` | string | 화면에 보여 줄 이름 |
| `category` | enum | `AMENITY`, `FACILITY`, `SERVICE` |
| `target` | enum | `ACCOMMODATION`이면 숙소용, `ROOM`이면 객실용 |

```json
{
  "status": 200,
  "data": [
    { "featureId": 1, "code": "PARKING", "name": "주차장", "category": "FACILITY", "target": "ACCOMMODATION" },
    { "featureId": 21, "code": "BATHTUB", "name": "욕조", "category": "FACILITY", "target": "ROOM" }
  ]
}
```

항목은 `docs/erd.md`의 feature 기준 데이터(31개)를 따른다. ID는 `db/01_schema.sql`의 INSERT 순서다.

**에러**: 400 `INVALID_INPUT`

## 7. 숙소 관리

### 7.1 내 숙소 목록

`GET /api/accommodations/me` · 권한: HOST

토큰의 사용자가 등록한 숙소를 최신순으로 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | int | 선택 | 0 | 페이지 번호 |
| `size` | int | 선택 | 20 | 페이지 크기. 1~100 |

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `accommodationId` | long | 숙소 ID |
| `name` | string | 숙소명 |
| `type` | enum | 숙소 유형 |
| `region` | string | 지역 |
| `address` | string | 주소 |
| `thumbnailUrl` | string, null | 대표 이미지 URL(id 오름차순 첫 이미지). 이미지가 없으면 null |
| `roomCount` | int | 삭제되지 않은 객실 수 |
| `createdAt` | datetime | 등록 일시 |

```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "accommodationId": 12,
        "name": "해운대 오션뷰 호텔",
        "type": "HOTEL",
        "region": "부산",
        "address": "부산 해운대구 해운대해변로 100",
        "thumbnailUrl": "/images/2026/11/a1b2c3.jpg",
        "roomCount": 4,
        "createdAt": "2026-11-25T10:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

**에러**: 401 `UNAUTHORIZED`, 403 `FORBIDDEN`

### 7.2 숙소 등록

`POST /api/accommodations` · 권한: HOST

`host_id`는 요청으로 받지 않고 토큰의 사용자로 저장한다.

**요청 본문**

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `name` | string | 필수 | 공백 제외 1~50자 |
| `description` | string | 선택 | 255자 이하 |
| `type` | enum | 필수 | 정의된 숙소 유형 중 하나 |
| `region` | string | 필수 | 정의된 지역 값 중 하나 |
| `address` | string | 필수 | 1~255자 |
| `addressDetail` | string | 필수 | 1~255자 |
| `imageUrls` | array | 선택 | 12.6이 돌려준 URL. 최대 10개. 첫 번째가 대표 이미지 |
| `featureIds` | long 배열 | 선택 | 대상이 `ACCOMMODATION`인 편의시설 ID(6.4) |

```json
{
  "name": "해운대 오션뷰 호텔",
  "description": "해변까지 걸어서 3분 거리입니다.",
  "type": "HOTEL",
  "region": "부산",
  "address": "부산 해운대구 해운대해변로 100",
  "addressDetail": "본관",
  "imageUrls": ["/images/2026/11/a1b2c3.jpg", "/images/2026/11/d4e5f6.jpg"],
  "featureIds": [1, 2]
}
```

**응답 201**

```json
{
  "status": 201,
  "data": { "accommodationId": 12 }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_FEATURE`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`

### 7.3 숙소 수정

`PATCH /api/accommodations/{id}` · 권한: HOST (본인)

보낸 필드만 바꾼다. 보내지 않은 필드는 그대로 둔다. `imageUrls`나 `featureIds`를 보내면 기존 목록 전체를 보낸 목록으로 교체하고, 빈 배열을 보내면 모두 지운다.

**요청 본문** (모두 선택, 1개 이상 필요)

| 필드 | 타입 | 검증 |
|---|---|---|
| `name` | string | 공백 제외 1~50자 |
| `description` | string | 255자 이하 |
| `type` | enum | 정의된 숙소 유형 중 하나 |
| `region` | string | 정의된 지역 값 중 하나 |
| `address` | string | 1~255자 |
| `addressDetail` | string | 1~255자 |
| `imageUrls` | array | 최대 10개. 첫 번째가 대표 이미지 |
| `featureIds` | long 배열 | 대상이 `ACCOMMODATION`인 ID |

```json
{
  "name": "해운대 오션뷰 호텔 신관",
  "description": "2026년 12월 리모델링을 마쳤습니다."
}
```

**응답 200** `data`: 수정된 숙소 정보. 필드 뜻은 6.2와 같다.

```json
{
  "status": 200,
  "data": {
    "accommodationId": 12,
    "name": "해운대 오션뷰 호텔 신관",
    "description": "2026년 12월 리모델링을 마쳤습니다.",
    "type": "HOTEL",
    "region": "부산",
    "address": "부산 해운대구 해운대해변로 100",
    "addressDetail": "본관",
    "imageUrls": ["/images/2026/11/a1b2c3.jpg", "/images/2026/11/d4e5f6.jpg"],
    "features": [
      { "featureId": 1, "code": "PARKING", "name": "주차장", "category": "FACILITY" },
      { "featureId": 2, "code": "POOL", "name": "수영장", "category": "FACILITY" }
    ],
    "updatedAt": "2026-11-27T09:00:00"
  }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_FEATURE`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ACCOMMODATION_NOT_FOUND`

### 7.4 숙소 삭제

`DELETE /api/accommodations/{id}` · 권한: HOST (본인)

- soft delete다. `deleted_at`을 기록하고 이후 모든 조회에서 제외한다.
- 소속 객실도 함께 soft delete한다.
- 소속 객실에 체크아웃 전인 PENDING 또는 CONFIRMED 예약이 하나라도 있으면 삭제를 거절한다.

**응답 200**

```json
{
  "status": 200,
  "data": null
}
```

**에러**: 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ACCOMMODATION_NOT_FOUND`, 409 `ACCOMMODATION_HAS_ACTIVE_RESERVATION`

## 8. 객실 관리

### 8.1 객실 등록

`POST /api/accommodations/{id}/rooms` · 권한: HOST (본인)

경로의 `id`는 객실을 추가할 숙소의 ID다.

**요청 본문**

| 필드 | 타입 | 필수 | 기본값 | 검증 |
|---|---|---|---|---|
| `name` | string | 필수 | - | 공백 제외 1~50자 |
| `description` | string | 선택 | - | 255자 이하 |
| `price` | int | 필수 | - | 0 이상. 1박 기준 |
| `grade` | enum | 필수 | - | 정의된 객실 등급 중 하나 |
| `standardCapacity` | int | 필수 | - | 1 이상 |
| `maxCapacity` | int | 필수 | - | `standardCapacity` 이상 |
| `extraPersonFee` | int | 선택 | 0 | 0 이상 |
| `isAvailable` | boolean | 선택 | true | - |
| `imageUrls` | array | 선택 | - | 12.6이 돌려준 URL. 최대 10개. 첫 번째가 대표 이미지 |
| `featureIds` | long 배열 | 선택 | - | 대상이 `ROOM`인 편의시설 ID(6.4) |

```json
{
  "name": "디럭스 더블",
  "description": "오션뷰, 더블 침대 1개",
  "price": 120000,
  "grade": "DELUXE",
  "standardCapacity": 2,
  "maxCapacity": 3,
  "extraPersonFee": 20000,
  "imageUrls": ["/images/2026/11/r101a.jpg"],
  "featureIds": [21]
}
```

**응답 201**

```json
{
  "status": 201,
  "data": { "roomId": 101 }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_CAPACITY`, 400 `INVALID_FEATURE`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ACCOMMODATION_NOT_FOUND`

### 8.2 객실 수정

`PATCH /api/rooms/{roomId}` · 권한: HOST (본인)

- 보낸 필드만 바꾼다.
- `imageUrls`나 `featureIds`를 보내면 기존 목록 전체를 보낸 목록으로 교체하고, 빈 배열을 보내면 모두 지운다.
- 판매 중지는 `isAvailable`을 false로 보내서 처리한다. 판매 중지된 객실은 숙소 상세에는 보이지만 예약 생성은 거절된다.
- 인원은 수정 후의 값 기준으로 `maxCapacity`가 `standardCapacity` 이상이어야 한다.
- 가격을 바꿔도 이미 생성된 예약의 `total_price`는 바뀌지 않는다.

**요청 본문** (모두 선택, 1개 이상 필요)

| 필드 | 타입 | 검증 |
|---|---|---|
| `name` | string | 공백 제외 1~50자 |
| `description` | string | 255자 이하 |
| `price` | int | 0 이상 |
| `grade` | enum | 정의된 객실 등급 중 하나 |
| `standardCapacity` | int | 1 이상 |
| `maxCapacity` | int | `standardCapacity` 이상 |
| `extraPersonFee` | int | 0 이상 |
| `isAvailable` | boolean | - |
| `imageUrls` | array | 최대 10개. 첫 번째가 대표 이미지 |
| `featureIds` | long 배열 | 대상이 `ROOM`인 ID |

```json
{
  "price": 135000,
  "isAvailable": false
}
```

**응답 200** `data`: 수정된 객실 정보. 6.2의 `rooms[]` 필드와 `accommodationId`, `updatedAt`.

```json
{
  "status": 200,
  "data": {
    "roomId": 101,
    "accommodationId": 12,
    "name": "디럭스 더블",
    "description": "오션뷰, 더블 침대 1개",
    "price": 135000,
    "grade": "DELUXE",
    "standardCapacity": 2,
    "maxCapacity": 3,
    "extraPersonFee": 20000,
    "isAvailable": false,
    "imageUrls": ["/images/2026/11/r101a.jpg"],
    "features": [
      { "featureId": 21, "code": "BATHTUB", "name": "욕조", "category": "FACILITY" }
    ],
    "updatedAt": "2026-11-27T09:10:00"
  }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_CAPACITY`, 400 `INVALID_FEATURE`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ROOM_NOT_FOUND`

### 8.3 객실 삭제

`DELETE /api/rooms/{roomId}` · 권한: HOST (본인)

- soft delete다.
- 체크아웃 전인 PENDING 또는 CONFIRMED 예약이 있으면 삭제를 거절한다. 더 받지 않으려면 8.2로 판매 중지한다.

**응답 200**

```json
{
  "status": 200,
  "data": null
}
```

**에러**: 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ROOM_NOT_FOUND`, 409 `ROOM_HAS_ACTIVE_RESERVATION`

## 9. 예약

예약 상태는 세 가지다. 생성하면 `PENDING`, 결제하면 `CONFIRMED`, 취소하거나 결제 기한이 지나면 `CANCELLED`가 된다.

### 9.1 예약 생성

`POST /api/reservations` · 권한: GUEST

결제 대기(`PENDING`) 상태의 예약을 만든다. 생성 후 10분 안에 10.1로 결제해야 확정된다.

**요청 본문**

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `roomId` | long | 필수 | 존재하고 판매 중인 객실 |
| `checkInDate` | date | 필수 | 오늘 또는 그 이후 |
| `checkOutDate` | date | 필수 | `checkInDate`보다 뒤 |
| `guestCount` | int | 필수 | 1 이상, 객실 `maxCapacity` 이하 |

```json
{
  "roomId": 101,
  "checkInDate": "2026-12-24",
  "checkOutDate": "2026-12-26",
  "guestCount": 3
}
```

**처리 규칙**

- 금액: `totalPrice`는 요청으로 받지 않고 서버가 계산해 저장한다. 식은 `price × 박 수 + extraPersonFee × 초과 인원 × 박 수`이고, 초과 인원은 `guestCount − standardCapacity`(0 미만이면 0)이다. 위 예시는 `120000 × 2 + 20000 × 1 × 2 = 280000`이다.
- 중복 방지: 같은 객실의 PENDING 또는 CONFIRMED 예약 중 `기존.check_in < 요청.checkOut AND 기존.check_out > 요청.checkIn`인 것이 있으면 거절한다. 검사와 저장 사이에 다른 요청이 끼어들지 못하게 락을 건다.

**응답 201** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reservationId` | long | 예약 ID |
| `status` | enum | 항상 `PENDING` |
| `totalPrice` | int | 서버가 계산한 총 금액 |
| `nights` | int | 박 수 |
| `paymentDeadline` | datetime | 이 시각까지 결제하지 않으면 자동 취소 |

```json
{
  "status": 201,
  "data": {
    "reservationId": 501,
    "status": "PENDING",
    "totalPrice": 280000,
    "nights": 2,
    "paymentDeadline": "2026-12-01T15:10:00"
  }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_DATE_RANGE`, 400 `GUEST_COUNT_EXCEEDED`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 404 `ROOM_NOT_FOUND`, 409 `ROOM_NOT_AVAILABLE`, 409 `RESERVATION_DATE_CONFLICT`

### 9.2 내 예약 목록

`GET /api/reservations/me` · 권한: GUEST

내 예약을 최근에 만든 순서로 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | enum | 선택 | `PENDING`, `CONFIRMED`, `CANCELLED` 중 하나만 조회 |
| `page`, `size` | int | 선택 | 1.3 참고 |

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reservationId` | long | 예약 ID |
| `status` | enum | 예약 상태 |
| `accommodation.accommodationId` | long | 숙소 ID |
| `accommodation.name` | string | 숙소명 |
| `room.roomId` | long | 객실 ID |
| `room.name` | string | 객실명 |
| `checkInDate` | date | 체크인 날짜 |
| `checkOutDate` | date | 체크아웃 날짜 |
| `guestCount` | int | 인원 |
| `totalPrice` | int | 총 금액 |
| `hasReview` | boolean | 이 예약에 리뷰를 썼는지 |
| `createdAt` | datetime | 예약 생성 일시 |

```json
{
  "status": 200,
  "data": {
    "content": [
      {
        "reservationId": 501,
        "status": "CONFIRMED",
        "accommodation": { "accommodationId": 12, "name": "해운대 오션뷰 호텔" },
        "room": { "roomId": 101, "name": "디럭스 더블" },
        "checkInDate": "2026-12-24",
        "checkOutDate": "2026-12-26",
        "guestCount": 3,
        "totalPrice": 280000,
        "hasReview": false,
        "createdAt": "2026-12-01T15:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "hasNext": false
  }
}
```

**에러**: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `FORBIDDEN`

### 9.3 예약 상세

`GET /api/reservations/{id}` · 권한: 본인 또는 숙소 HOST

예약자 본인과 그 숙소를 등록한 HOST만 볼 수 있다.

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reservationId` | long | 예약 ID |
| `status` | enum | 예약 상태 |
| `guest.userId` | long | 예약자 ID |
| `guest.name` | string | 예약자 이름 |
| `accommodation.accommodationId` | long | 숙소 ID |
| `accommodation.name` | string | 숙소명 |
| `accommodation.address` | string | 숙소 주소 |
| `room.roomId` | long | 객실 ID |
| `room.name` | string | 객실명 |
| `checkInDate`, `checkOutDate` | date | 체크인·체크아웃 날짜 |
| `nights` | int | 박 수 |
| `guestCount` | int | 인원 |
| `totalPrice` | int | 총 금액 |
| `payment` | object, null | 가장 최근 결제. 결제 전이면 null. 필드는 10.2와 같다 |
| `createdAt` | datetime | 예약 생성 일시 |
| `cancelledAt` | datetime, null | 취소 일시 |

```json
{
  "status": 200,
  "data": {
    "reservationId": 501,
    "status": "CONFIRMED",
    "guest": { "userId": 7, "name": "김여행" },
    "accommodation": {
      "accommodationId": 12,
      "name": "해운대 오션뷰 호텔",
      "address": "부산 해운대구 해운대해변로 100"
    },
    "room": { "roomId": 101, "name": "디럭스 더블" },
    "checkInDate": "2026-12-24",
    "checkOutDate": "2026-12-26",
    "nights": 2,
    "guestCount": 3,
    "totalPrice": 280000,
    "payment": {
      "paymentId": 9001,
      "method": "CARD",
      "status": "PAID",
      "amount": 275000,
      "usedPoint": 5000,
      "createdAt": "2026-12-01T15:03:00",
      "paidAt": "2026-12-01T15:03:00",
      "cancelledAt": null
    },
    "createdAt": "2026-12-01T15:00:00",
    "cancelledAt": null
  }
}
```

**에러**: 401 `UNAUTHORIZED`, 403 `RESERVATION_ACCESS_DENIED`, 404 `RESERVATION_NOT_FOUND`

### 9.4 예약 취소

`POST /api/reservations/{id}/cancel` · 권한: 본인

요청 본문은 없다.

**처리 규칙**

- PENDING 또는 CONFIRMED 예약만 취소할 수 있다.
- 체크인 전날까지만 취소할 수 있다.
- 예약을 `CANCELLED`로 바꾸고 `cancelledAt`을 기록한다.
- CONFIRMED였다면 결제를 `CANCELLED`로 바꾸고, 사용한 포인트를 돌려주고 `REFUND` 내역을 남긴다. 이 과정은 한 트랜잭션으로 처리한다.
- 이 결제로 적립된 포인트는 회수하지 않는다(가정 20).

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reservationId` | long | 예약 ID |
| `status` | enum | 항상 `CANCELLED` |
| `cancelledAt` | datetime | 취소 일시 |
| `refund` | object, null | 환불 내용. 결제 전에 취소했으면 null |
| `refund.amount` | int | 취소된 결제 금액 |
| `refund.refundedPoint` | int | 돌려받은 사용 포인트 |

```json
{
  "status": 200,
  "data": {
    "reservationId": 501,
    "status": "CANCELLED",
    "cancelledAt": "2026-12-10T11:20:00",
    "refund": { "amount": 275000, "refundedPoint": 5000 }
  }
}
```

**에러**: 401 `UNAUTHORIZED`, 403 `RESERVATION_ACCESS_DENIED`, 404 `RESERVATION_NOT_FOUND`, 409 `RESERVATION_ALREADY_CANCELLED`, 409 `CANCEL_PERIOD_EXPIRED`, 500 `POINT_HISTORY_ALREADY_EXISTS`, 500 `POINT_NOT_FOUND`

### 9.5 숙소의 예약 목록

`GET /api/accommodations/{id}/reservations` · 권한: HOST (본인)

숙소에 들어온 예약을 체크인 날짜가 빠른 순서로 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `status` | enum | 선택 | `PENDING`, `CONFIRMED`, `CANCELLED` 중 하나만 조회 |
| `roomId` | long | 선택 | 이 객실의 예약만 조회 |
| `page`, `size` | int | 선택 | 1.3 참고 |

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reservationId` | long | 예약 ID |
| `status` | enum | 예약 상태 |
| `room.roomId` | long | 객실 ID |
| `room.name` | string | 객실명 |
| `guest.userId` | long | 예약자 ID |
| `guest.name` | string | 예약자 이름 |
| `checkInDate`, `checkOutDate` | date | 체크인·체크아웃 날짜 |
| `guestCount` | int | 인원 |
| `totalPrice` | int | 총 금액 |
| `createdAt` | datetime | 예약 생성 일시 |

**에러**: 401 `UNAUTHORIZED`, 403 `FORBIDDEN`, 403 `NOT_ACCOMMODATION_OWNER`, 404 `ACCOMMODATION_NOT_FOUND`

## 10. 결제

실제 PG 연동 없이 모의 결제로 처리한다. 요청이 유효하면 항상 성공한다.

### 10.1 결제

`POST /api/reservations/{id}/payments` · 권한: 본인

PENDING 예약을 결제하고 예약을 확정한다.

**요청 본문**

| 필드 | 타입 | 필수 | 기본값 | 검증 |
|---|---|---|---|---|
| `method` | enum | 필수 | - | `CARD`, `BANK_TRANSFER`, `KAKAO_PAY` |
| `usePoint` | int | 선택 | 0 | 0 이상, 보유 포인트 이하, 예약 `totalPrice` 이하 |

```json
{
  "method": "CARD",
  "usePoint": 5000
}
```

**처리 규칙** (한 트랜잭션)

1. 결제 금액 `amount`는 `totalPrice − usePoint`로 서버가 계산한다. 요청으로 받지 않는다.
2. `usePoint`만큼 포인트를 차감하고 사용 내역을 남긴다. 동시에 들어온 요청이 같은 잔액을 두 번 쓰지 못하게 락을 건다.
3. 결제를 `PAID`로 저장하고 예약을 `CONFIRMED`로 바꾼다.
4. `amount`의 5%를 포인트로 적립하고 내역을 남긴다. 소수점 이하는 버린다.

**응답 201** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `paymentId` | long | 결제 ID |
| `reservationId` | long | 예약 ID |
| `reservationStatus` | enum | 항상 `CONFIRMED` |
| `method` | enum | 결제 수단 |
| `status` | enum | 항상 `PAID` |
| `amount` | int | 실제 결제 금액 |
| `usedPoint` | int | 사용한 포인트 |
| `earnedPoint` | int | 적립된 포인트 |
| `paidAt` | datetime | 결제 일시 |

```json
{
  "status": 201,
  "data": {
    "paymentId": 9001,
    "reservationId": 501,
    "reservationStatus": "CONFIRMED",
    "method": "CARD",
    "status": "PAID",
    "amount": 275000,
    "usedPoint": 5000,
    "earnedPoint": 13750,
    "paidAt": "2026-12-01T15:03:00"
  }
}
```

`usedPoint`와 `earnedPoint`는 `PAYMENT`에 컬럼이 없어 이 예약의 `POINT_HISTORY`에서 읽는다(가정 15).

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_POINT_AMOUNT`, 401 `UNAUTHORIZED`, 403 `RESERVATION_ACCESS_DENIED`, 404 `RESERVATION_NOT_FOUND`, 409 `RESERVATION_NOT_PAYABLE`, 409 `INSUFFICIENT_POINT`, 500 `POINT_HISTORY_ALREADY_EXISTS`, 500 `POINT_NOT_FOUND`

### 10.2 결제 내역 조회

`GET /api/reservations/{id}/payments` · 권한: 본인

예약의 결제 기록을 최신순 배열로 돌려준다. 페이징하지 않는다.

**응답 200** `data[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `paymentId` | long | 결제 ID |
| `method` | enum | 결제 수단 |
| `status` | enum | `READY`, `PAID`, `FAILED`, `CANCELLED` |
| `amount` | int | 결제 금액 |
| `usedPoint` | int | 사용한 포인트. 이 예약의 `USE` 내역에서 읽는다 |
| `createdAt` | datetime | 결제 요청 일시 |
| `paidAt` | datetime, null | 결제 완료 일시 |
| `cancelledAt` | datetime, null | 결제 취소 일시 |

```json
{
  "status": 200,
  "data": [
    {
      "paymentId": 9001,
      "method": "CARD",
      "status": "PAID",
      "amount": 275000,
      "usedPoint": 5000,
      "createdAt": "2026-12-01T15:03:00",
      "paidAt": "2026-12-01T15:03:00",
      "cancelledAt": null
    }
  ]
}
```

결제 취소는 별도 API 없이 9.4 예약 취소에서 함께 처리한다.

**에러**: 401 `UNAUTHORIZED`, 403 `RESERVATION_ACCESS_DENIED`, 404 `RESERVATION_NOT_FOUND`

## 11. 포인트

적립, 사용, 환불은 전용 API 없이 결제(10.1)와 예약 취소(9.4) 안에서 처리한다. 여기에는 조회 API만 있다.

### 11.1 포인트 잔액 조회

`GET /api/points/me` · 권한: 로그인

**응답 200**

```json
{
  "status": 200,
  "data": { "balance": 23750 }
}
```

포인트를 한 번도 받은 적 없는 사용자는 `balance`가 0이다.

**에러**: 401 `UNAUTHORIZED`

### 11.2 포인트 내역 조회

`GET /api/points/me/history` · 권한: 로그인

내 포인트 내역을 최신순으로 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | enum | 선택 | 아래 유형 중 하나만 조회 |
| `page`, `size` | int | 선택 | 1.3 참고 |

**내역 유형**

| 값 | 의미 | 잔액 변화 |
|---|---|---|
| `EARN` | 결제 적립 | 증가 |
| `USE` | 결제에 사용 | 감소 |
| `REFUND` | 예약 취소로 사용분 환불 | 증가 |

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `pointHistoryId` | long | 내역 ID |
| `type` | enum | 내역 유형 |
| `amount` | int | 변동 포인트. 항상 양수이고 증감은 `type`으로 판단한다 |
| `reservationId` | long, null | 관련 예약 ID |
| `createdAt` | datetime | 발생 일시 |

```json
{
  "status": 200,
  "data": {
    "content": [
      { "pointHistoryId": 302, "type": "EARN", "amount": 13750, "reservationId": 501, "createdAt": "2026-12-01T15:03:00" },
      { "pointHistoryId": 301, "type": "USE", "amount": 5000, "reservationId": 501, "createdAt": "2026-12-01T15:03:00" }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "hasNext": false
  }
}
```

**에러**: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`

## 12. 리뷰

### 12.1 리뷰 작성

`POST /api/reviews` · 권한: 예약 본인

**요청 본문**

| 필드 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `reservationId` | long | 필수 | 내 예약 |
| `rating` | int | 필수 | 1~5 |
| `title` | string | 선택 | 100자 이하 |
| `body` | string | 선택 | 2000자 이하 |
| `imageUrls` | array | 선택 | 12.6이 돌려준 URL. 최대 5개 |

```json
{
  "reservationId": 501,
  "rating": 5,
  "title": "바다가 바로 보여요",
  "body": "방이 깨끗하고 직원분들이 친절했습니다.",
  "imageUrls": ["/images/2026/12/8f3a1c.jpg"]
}
```

**처리 규칙**

- 예약이 `CONFIRMED`이고 체크아웃 날짜가 지났을 때만 쓸 수 있다.
- 예약 1건에 리뷰는 1개다.
- `accommodation_id`는 요청으로 받지 않고 예약의 객실에서 찾아 저장한다.

**응답 201**

```json
{
  "status": 201,
  "data": { "reviewId": 77 }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `REVIEW_NOT_ALLOWED`, 401 `UNAUTHORIZED`, 403 `RESERVATION_ACCESS_DENIED`, 404 `RESERVATION_NOT_FOUND`, 409 `REVIEW_ALREADY_EXISTS`

### 12.2 숙소 리뷰 목록

`GET /api/accommodations/{id}/reviews` · 권한: 공개

숙소의 평균 평점과 리뷰 목록을 함께 돌려준다.

**쿼리 파라미터**

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `sort` | enum | 선택 | `LATEST` | `LATEST`, `RATING_DESC`, `RATING_ASC` |
| `page`, `size` | int | 선택 | 0, 20 | 1.3 참고 |

**응답 200** `data`

| 필드 | 타입 | 설명 |
|---|---|---|
| `averageRating` | double | 평균 평점. 소수 첫째 자리까지. 리뷰가 없으면 0.0 |
| `reviewCount` | int | 리뷰 수 |
| `reviews` | object | 페이지 응답(1.3) |
| `reviews.content[].reviewId` | long | 리뷰 ID |
| `reviews.content[].rating` | int | 평점 |
| `reviews.content[].title` | string, null | 제목 |
| `reviews.content[].body` | string, null | 본문 |
| `reviews.content[].imageUrls` | array | 이미지 URL 목록. 없으면 빈 배열 |
| `reviews.content[].author.userId` | long | 작성자 ID |
| `reviews.content[].author.nickname` | string | 작성자 닉네임 |
| `reviews.content[].roomName` | string | 투숙한 객실명 |
| `reviews.content[].createdAt` | datetime | 작성 일시 |

```json
{
  "status": 200,
  "data": {
    "averageRating": 4.5,
    "reviewCount": 37,
    "reviews": {
      "content": [
        {
          "reviewId": 77,
          "rating": 5,
          "title": "바다가 바로 보여요",
          "body": "방이 깨끗하고 직원분들이 친절했습니다.",
          "imageUrls": ["/images/2026/12/8f3a1c.jpg"],
          "author": { "userId": 7, "nickname": "여행자" },
          "roomName": "디럭스 더블",
          "createdAt": "2026-12-27T10:00:00"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 37,
      "totalPages": 2,
      "hasNext": true
    }
  }
}
```

**에러**: 400 `INVALID_INPUT`, 404 `ACCOMMODATION_NOT_FOUND`

### 12.3 내 리뷰 목록

`GET /api/reviews/me` · 권한: 로그인

내가 쓴 리뷰를 최신순으로 돌려준다. 쿼리 파라미터는 `page`, `size`다.

**응답 200** `data.content[]`

| 필드 | 타입 | 설명 |
|---|---|---|
| `reviewId` | long | 리뷰 ID |
| `reservationId` | long | 예약 ID |
| `accommodation.accommodationId` | long | 숙소 ID |
| `accommodation.name` | string | 숙소명 |
| `rating` | int | 평점 |
| `title` | string, null | 제목 |
| `body` | string, null | 본문 |
| `imageUrls` | array | 이미지 URL 목록 |
| `createdAt` | datetime | 작성 일시 |

**에러**: 401 `UNAUTHORIZED`

### 12.4 리뷰 수정

`PATCH /api/reviews/{id}` · 권한: 본인

보낸 필드만 바꾼다. `imageUrls`를 보내면 기존 이미지 목록 전체를 보낸 목록으로 교체한다.

**요청 본문** (모두 선택, 1개 이상 필요)

| 필드 | 타입 | 검증 |
|---|---|---|
| `rating` | int | 1~5 |
| `title` | string | 100자 이하 |
| `body` | string | 2000자 이하 |
| `imageUrls` | array | 최대 5개 |

**응답 200** `data`: 수정된 리뷰. 12.3의 필드와 `updatedAt`.

**에러**: 400 `INVALID_INPUT`, 401 `UNAUTHORIZED`, 403 `NOT_REVIEW_AUTHOR`, 404 `REVIEW_NOT_FOUND`

### 12.5 리뷰 삭제

`DELETE /api/reviews/{id}` · 권한: 본인

soft delete다. 삭제한 리뷰는 목록과 평균 평점 계산에서 빠진다.

**응답 200**: `data`는 null.

**에러**: 401 `UNAUTHORIZED`, 403 `NOT_REVIEW_AUTHOR`, 404 `REVIEW_NOT_FOUND`

### 12.6 이미지 업로드

`POST /api/images` · 권한: 로그인

이미지 파일 1개를 올리고 URL을 받는다. 숙소, 객실, 리뷰가 함께 쓴다. 받은 URL을 숙소(7.2, 7.3), 객실(8.1, 8.2), 리뷰(12.1, 12.4)의 `imageUrls`에 넣는다.

**요청**: `Content-Type: multipart/form-data`

| 파트 | 타입 | 필수 | 검증 |
|---|---|---|---|
| `file` | file | 필수 | jpg, png, webp. 5MB 이하 |

**응답 201**

```json
{
  "status": 201,
  "data": { "imageUrl": "/images/2026/12/8f3a1c.jpg" }
}
```

**에러**: 400 `INVALID_INPUT`, 400 `INVALID_FILE_TYPE`, 401 `UNAUTHORIZED`, 413 `FILE_TOO_LARGE`

## 13. 담당자별 확인 요청

각 담당자가 자기 절을 읽고 아래 항목을 확정하면, 0절의 가정을 0-1로 옮기고 본문을 고친다. 이미 확정된 항목은 0-1에 있으니 구현 전에 자기 담당 행을 확인한다.

| 담당 | 확인할 내용 |
|---|---|
| A 임재상 | 콜백에서 토큰을 주는 방식(가정 5). 가입 입력 규칙(가정 6) |
| B 김진형 | 지역 값(가정 8). 목록 정렬 옵션(가정 10) |
| C 천승혁 | 금액 계산식(가정 11). PENDING 만료 시간(가정 12). 취소 가능 시점(가정 13). `PAYMENT.used_point` 컬럼 추가 여부(가정 15) |
| D 최건 | 취소 시 적립 포인트 처리(가정 20) |
| 전원 | `address_detail` 필수 여부(가정 9). `room.grade`, `payment.method` 값 확정(가정 7) |
