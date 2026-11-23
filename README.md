# NBE13-15-2-team03 숙소 예약 서비스

호스트가 숙소와 객실을 등록하고, 게스트가 조회·예약·결제(모의)·리뷰를 하는 숙소 예약 서비스입니다.

## 기술 스택

- Java 21, Gradle
- Spring Boot 4.1.2
- MySQL 8
- 인증: JWT (Access + Refresh), 카카오·구글 OAuth2

## 팀원과 담당

| 담당 | 이름 | 도메인 | 그 외 역할 |
|---|---|---|---|
| A | 임재상 | 인증, 회원, 소셜 로그인, 보안 설정 | GitHub 저장소 설정 |
| B | 김진형 | 숙소, 객실 (관리와 조회·필터링) | API 명세 작성 |
| C | 천승혁 | 예약, 결제, 예약 일정 조회 | 기본 엔티티, 스프링 부트 초기 설정 |
| D | 최건 | 포인트, 리뷰, 이미지 업로드, 공통 모듈 | DB 초기 설정 |

## 문서

- [기능 및 API 목록](docs/feature-api-list.md)
- [팀 규칙](CONTRIBUTING.md)

## 실행 방법

1. `src/main/resources/application-secret.yml.example`을 복사해 `application-secret.yml`을 만들고 값을 채웁니다. (이 파일은 git에 올라가지 않습니다)
2. 로컬 MySQL을 실행합니다.
3. `./gradlew bootRun`