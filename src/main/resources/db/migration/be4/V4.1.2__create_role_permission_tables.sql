CREATE TABLE role (
    role_id     BIGINT       NOT NULL AUTO_INCREMENT,
    academy_id  BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_role PRIMARY KEY (role_id),
    CONSTRAINT uk_role_academy_name UNIQUE (academy_id, name),
    CONSTRAINT fk_role_academy FOREIGN KEY (academy_id) REFERENCES academy (academy_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE permission (
    permission_id BIGINT       NOT NULL AUTO_INCREMENT,
    code          VARCHAR(100) NOT NULL,
    resource      VARCHAR(50)  NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    description   VARCHAR(255) NULL,
    CONSTRAINT pk_permission PRIMARY KEY (permission_id),
    CONSTRAINT uk_permission_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT pk_role_permission PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role (role_id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN role_id BIGINT NULL AFTER role,
    ADD CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES role (role_id);

-- 기존 role 문자열 컬럼 제거 (role_id 조립식 권한으로 대체). notice 도메인의 UserInfoEntity가
-- 이 컬럼을 참조 중이라 notice 담당자가 별도로 자기 shim을 고쳐야 함 (팀 공지 완료).
ALTER TABLE users DROP COLUMN role;

-- 계정·권한 도메인 자체 기능에 필요한 permission만 시드 (다른 도메인은 자기 기능 만들 때 각자 추가)
INSERT INTO permission (code, resource, action, description) VALUES
    ('ROLE:MANAGE', 'ROLE', 'MANAGE', '역할 생성/수정/삭제 및 권한 조립'),
    ('ACCOUNT:CREATE', 'ACCOUNT', 'CREATE', '학원 직원 계정 발급');
