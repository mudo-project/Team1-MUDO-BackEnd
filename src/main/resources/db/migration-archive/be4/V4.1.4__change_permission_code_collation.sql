-- permission.code는 utf8mb4_unicode_ci(대소문자 구분 안 함)라, 대소문자가 다른 코드도
-- findAllByCodeIn 검증을 통과해버려 hasAuthority()(대소문자 구분)와 어긋나는 조용한 버그가
-- 생길 수 있다. utf8mb4_bin(구분함)으로 바꾼다. uk_permission_code는 재생성 불필요,
-- 기존 데이터가 전부 대문자라 마이그레이션 자체는 안전하다.
ALTER TABLE permission
    MODIFY code VARCHAR(100) COLLATE utf8mb4_bin NOT NULL;
