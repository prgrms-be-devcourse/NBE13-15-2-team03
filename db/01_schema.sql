SET NAMES utf8mb4;

CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    login_id   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL COMMENT 'BCrypt 해시',
    name       VARCHAR(50)  NOT NULL,
    nickname   VARCHAR(20)  NOT NULL,
    phone      VARCHAR(20)  NOT NULL,
    role       VARCHAR(20)  NOT NULL COMMENT 'GUEST / HOST / ADMIN',
    birth_date DATE         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NULL,
    deleted_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_login_id UNIQUE (login_id),
    CONSTRAINT uk_users_phone UNIQUE (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE social_user (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    provider         VARCHAR(20)  NOT NULL COMMENT 'KAKAO / GOOGLE',
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_social_user_provider UNIQUE (provider, provider_user_id),
    CONSTRAINT fk_social_user_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE feature (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    code     VARCHAR(30) NOT NULL COMMENT '항목 코드 (PARKING, BATHTUB 등)',
    name     VARCHAR(50) NOT NULL COMMENT '표시 이름',
    category VARCHAR(20) NOT NULL COMMENT 'AMENITY / FACILITY / SERVICE',
    target   VARCHAR(20) NOT NULL COMMENT 'ACCOMMODATION / ROOM',
    PRIMARY KEY (id),
    CONSTRAINT uk_feature_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE accommodation (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    host_id        BIGINT       NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    description    VARCHAR(255) NULL,
    type           VARCHAR(20)  NOT NULL COMMENT 'HOTEL / MOTEL / PENSION / RESORT / POOL_VILLA / CAMPING',
    region         VARCHAR(20)  NOT NULL COMMENT '필터용 시/도',
    address        VARCHAR(255) NOT NULL,
    address_detail VARCHAR(255) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NULL,
    deleted_at     DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_accommodation_host FOREIGN KEY (host_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE accommodation_image (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    accommodation_id BIGINT NOT NULL,
    url              TEXT   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_accommodation_image_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE accommodation_feature (
    accommodation_id BIGINT NOT NULL,
    feature_id       BIGINT NOT NULL COMMENT 'target이 ACCOMMODATION인 feature만 허용 (애플리케이션에서 검증)',
    PRIMARY KEY (accommodation_id, feature_id),
    CONSTRAINT fk_accommodation_feature_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (id),
    CONSTRAINT fk_accommodation_feature_feature FOREIGN KEY (feature_id) REFERENCES feature (id),
    INDEX idx_accommodation_feature (feature_id, accommodation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE room (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    accommodation_id  BIGINT       NOT NULL,
    name              VARCHAR(50)  NOT NULL,
    description       VARCHAR(255) NULL,
    price             INT          NOT NULL,
    grade             VARCHAR(20)  NOT NULL COMMENT '값 추후 확정',
    standard_capacity INT          NOT NULL,
    max_capacity      INT          NOT NULL,
    extra_person_fee  INT          NOT NULL DEFAULT 0,
    is_available      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NULL,
    deleted_at        DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_room_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE room_image (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    room_id BIGINT NOT NULL,
    url     TEXT   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_room_image_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE room_feature (
    room_id    BIGINT NOT NULL,
    feature_id BIGINT NOT NULL COMMENT 'target이 ROOM인 feature만 허용 (애플리케이션에서 검증)',
    PRIMARY KEY (room_id, feature_id),
    CONSTRAINT fk_room_feature_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_room_feature_feature FOREIGN KEY (feature_id) REFERENCES feature (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE reservation (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    guest_id       BIGINT      NOT NULL,
    room_id        BIGINT      NOT NULL,
    guest_count    INT         NOT NULL,
    total_price    INT         NOT NULL COMMENT '예약 시점 금액 스냅샷',
    status         VARCHAR(20) NOT NULL COMMENT 'PENDING / CONFIRMED / CANCELLED',
    check_in_date  DATE        NOT NULL,
    check_out_date DATE        NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    cancelled_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reservation_guest FOREIGN KEY (guest_id) REFERENCES users (id),
    CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES room (id),
    INDEX idx_reservation_room_dates (room_id, check_in_date, check_out_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE payment (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    reservation_id BIGINT      NOT NULL,
    method         VARCHAR(20) NOT NULL COMMENT '값 추후 확정',
    status         VARCHAR(20) NOT NULL COMMENT 'READY / PAID / FAILED / CANCELLED',
    amount         INT         NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    paid_at        DATETIME(6) NULL,
    cancelled_at   DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_payment_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- reservation_id가 UNIQUE라서, soft delete된 리뷰가 있는 예약에는 리뷰를 다시 쓸 수 없다.
CREATE TABLE review (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    reservation_id   BIGINT       NOT NULL,
    user_id          BIGINT       NOT NULL COMMENT '작성자',
    accommodation_id BIGINT       NOT NULL,
    rating           INT          NOT NULL,
    title            VARCHAR(100) NULL,
    body             TEXT         NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NULL,
    deleted_at       DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_review_reservation UNIQUE (reservation_id),
    CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_review_accommodation FOREIGN KEY (accommodation_id) REFERENCES accommodation (id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE review_image (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    review_id BIGINT NOT NULL,
    url       TEXT   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES review (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE point (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    balance    INT         NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_point_user UNIQUE (user_id),
    CONSTRAINT fk_point_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE point_history (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    user_id        BIGINT      NOT NULL,
    reservation_id BIGINT      NULL COMMENT '관련 예약',
    type           VARCHAR(20) NOT NULL COMMENT 'EARN / USE / REFUND',
    amount         INT         NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_point_history_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_point_history_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 편의시설 기준 데이터. 모든 환경에 필요한 값이라 더미 데이터가 아니라 스키마와 함께 넣는다.
INSERT INTO feature (id, code, name, category, target) VALUES
    (1, 'PARKING', '주차장', 'FACILITY', 'ACCOMMODATION'),
    (2, 'POOL', '수영장', 'FACILITY', 'ACCOMMODATION'),
    (3, 'FITNESS', '피트니스', 'FACILITY', 'ACCOMMODATION'),
    (4, 'SAUNA', '사우나', 'FACILITY', 'ACCOMMODATION'),
    (5, 'RESTAURANT', '레스토랑', 'FACILITY', 'ACCOMMODATION'),
    (6, 'BBQ_AREA', '바비큐장', 'FACILITY', 'ACCOMMODATION'),
    (7, 'LAUNDRY_ROOM', '세탁실', 'FACILITY', 'ACCOMMODATION'),
    (8, 'SHOWER_ROOM', '공용 샤워실', 'FACILITY', 'ACCOMMODATION'),
    (9, 'SHARED_KITCHEN', '공용 취사장', 'FACILITY', 'ACCOMMODATION'),
    (10, 'WIFI', '와이파이', 'SERVICE', 'ACCOMMODATION'),
    (11, 'BREAKFAST', '조식', 'SERVICE', 'ACCOMMODATION'),
    (12, 'LUGGAGE_STORAGE', '짐보관', 'SERVICE', 'ACCOMMODATION'),
    (13, 'PET_FRIENDLY', '반려견 동반', 'SERVICE', 'ACCOMMODATION'),
    (14, 'SELF_CHECK_IN', '셀프 체크인', 'SERVICE', 'ACCOMMODATION'),
    (15, 'FRONT_DESK_24H', '24시간 프런트', 'SERVICE', 'ACCOMMODATION'),
    (16, 'TOILETRIES', '욕실용품', 'AMENITY', 'ROOM'),
    (17, 'TOWELS', '수건', 'AMENITY', 'ROOM'),
    (18, 'HAIR_DRYER', '드라이기', 'AMENITY', 'ROOM'),
    (19, 'BATHROBE', '가운·슬리퍼', 'AMENITY', 'ROOM'),
    (20, 'BOTTLED_WATER', '생수', 'AMENITY', 'ROOM'),
    (21, 'BATHTUB', '욕조', 'FACILITY', 'ROOM'),
    (22, 'SPA', '스파·월풀', 'FACILITY', 'ROOM'),
    (23, 'KITCHEN', '주방·취사', 'FACILITY', 'ROOM'),
    (24, 'AIR_CONDITIONER', '에어컨', 'FACILITY', 'ROOM'),
    (25, 'OTT', 'OTT', 'FACILITY', 'ROOM'),
    (26, 'PC', 'PC', 'FACILITY', 'ROOM'),
    (27, 'STYLER', '스타일러', 'FACILITY', 'ROOM'),
    (28, 'TERRACE', '테라스', 'FACILITY', 'ROOM'),
    (29, 'PRIVATE_POOL', '개별 수영장', 'FACILITY', 'ROOM'),
    (30, 'PRIVATE_BBQ', '개별 바비큐', 'FACILITY', 'ROOM'),
    (31, 'ELECTRICITY', '전기 사용', 'FACILITY', 'ROOM');
