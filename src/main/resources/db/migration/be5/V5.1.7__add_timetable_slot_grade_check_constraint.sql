-- V5.1.6에서 남았을 수 있는 NULL이나 매핑되지 않은 값(잘못 입력된 자유 문자열 등)이 있으면
-- 이 제약이 실패해 배포를 막는다 - Grade enum과 다른 값이 조용히 남아 있다가 나중에 조회
-- 시점에서야 JPA 역직렬화 오류로 터지는 것을 방지한다.
ALTER TABLE timetable_slot
  ADD CONSTRAINT chk_timetable_slot_grade CHECK (
    grade IN (
      'ELEMENTARY_1', 'ELEMENTARY_2', 'ELEMENTARY_3', 'ELEMENTARY_4', 'ELEMENTARY_5', 'ELEMENTARY_6',
      'MIDDLE_1', 'MIDDLE_2', 'MIDDLE_3',
      'HIGH_1', 'HIGH_2', 'HIGH_3'
    )
  );
