-- approval 모듈의 결재 신청/승인 이벤트(LeaveRequestSubmittedEvent, ApprovalDocumentDecidedEvent)를
-- 구독해 채워지는 attendance 자체 소유 테이블. approval의 어떤 테이블도 참조/FK로 연결하지 않는다
-- (documentId는 단순 식별자 값으로만 보관).
CREATE TABLE leave_request (
    leave_request_id BIGINT NOT NULL AUTO_INCREMENT,
    academy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (leave_request_id),
    CONSTRAINT uk_leave_request_document UNIQUE (document_id),
    INDEX idx_leave_request_academy_status_date (academy_id, status, start_date, end_date)
);
