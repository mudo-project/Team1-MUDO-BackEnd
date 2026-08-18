-- 연동 해제 후 다른 Google 계정으로 재연동해도, 재연동 판단(계정이 바뀌었는지)이 이미 삭제된
-- google_account_connection 행에 의존하던 버그를 없애기 위해 이 값을 shared_file_root 자신에게
-- 저장한다. NULL 허용: 이 컬럼이 없던 기존 행(과거 FAILED 상태 등)과의 호환을 위함.
ALTER TABLE shared_file_root
    ADD COLUMN connected_google_email VARCHAR(255) NULL AFTER google_root_folder_id;
