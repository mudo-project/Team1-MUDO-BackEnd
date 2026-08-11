-- 강의실 시간 충돌 검사(existsOverlap)가 lecture.classroom_code로 필터링하는데
-- 이 컬럼에 인덱스가 없어, 강의/일정 데이터가 늘어나면 등록마다 넓은 범위를 스캔할 수 있다.

ALTER TABLE `lecture`
  ADD INDEX `idx_lecture_classroom_code` (`classroom_code`);
