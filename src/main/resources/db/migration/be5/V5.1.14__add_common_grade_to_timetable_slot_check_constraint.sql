-- MySQL은 CHECK 제약을 직접 ALTER할 수 없어 기존 제약을 지우고 COMMON을 포함해 다시 만든다.
ALTER TABLE timetable_slot
  DROP CONSTRAINT chk_timetable_slot_grade;

ALTER TABLE timetable_slot
  ADD CONSTRAINT chk_timetable_slot_grade CHECK (
    grade IN (
      'ELEMENTARY_1', 'ELEMENTARY_2', 'ELEMENTARY_3', 'ELEMENTARY_4', 'ELEMENTARY_5', 'ELEMENTARY_6',
      'MIDDLE_1', 'MIDDLE_2', 'MIDDLE_3',
      'HIGH_1', 'HIGH_2', 'HIGH_3',
      'COMMON'
    )
  );
