-- 배포 단위(학원)당 시스템 루트 설정 한 건만 저장한다. 파일·폴더 목록은 담지 않으며
-- Google Drive를 단일 원본으로 사용한다. PK를 1로 고정해 행이 둘 이상 생기지 않게 막는다.
CREATE TABLE shared_file_root (
    shared_file_root_id TINYINT NOT NULL,
    google_root_folder_id VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_shared_file_root PRIMARY KEY (shared_file_root_id),
    CONSTRAINT chk_shared_file_root_singleton CHECK (shared_file_root_id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 일반 업무 권한: 조회·생성·업로드·이름변경·이동·휴지통삭제. 시스템 루트 자체의 생성·복구는 포함하지 않는다.
INSERT INTO permission (code, resource, action, description)
SELECT 'SHAREDFILE:MANAGE', 'SHAREDFILE', 'MANAGE',
       '공유파일 탭 진입, 폴더·파일 목록·검색·상세·미리보기·다운로드, 시스템 루트 하위 폴더·파일 생성, 로컬 파일 업로드, Google Docs·Sheets·Slides 생성, 이름 변경, 이동, 휴지통 삭제를 할 수 있습니다.'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'SHAREDFILE:MANAGE');

-- 시스템 루트 복구·재생성 전용 권한. 일반 구성원에게는 부여하지 않는다.
INSERT INTO permission (code, resource, action, description)
SELECT 'SHAREDFILE:ROOT_MANAGE', 'SHAREDFILE', 'ROOT_MANAGE',
       '시스템 루트 생성이 실패했거나 Google Drive에서 삭제가 확인된 경우 시스템 루트를 재생성할 수 있습니다.'
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'SHAREDFILE:ROOT_MANAGE');
