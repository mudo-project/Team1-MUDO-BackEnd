INSERT INTO permission (code, resource, action, description) VALUES
    ('ATTENDANCE:WIFI_IP_MANAGE', 'ATTENDANCE', 'WIFI_IP_MANAGE', '학원 출퇴근 허용 IP 조회·등록·삭제'),
    ('ATTENDANCE:POLICY_MANAGE', 'ATTENDANCE', 'POLICY_MANAGE', '학원 근무시간 정책 관리'),
    ('ATTENDANCE:CHECK_IN', 'ATTENDANCE', 'CHECK_IN', '출근 체크인'),
    ('ATTENDANCE:CHECK_OUT', 'ATTENDANCE', 'CHECK_OUT', '퇴근 체크아웃'),
    ('ATTENDANCE:READ', 'ATTENDANCE', 'READ', '소속 학원의 오늘 팀 근태 현황 조회');
