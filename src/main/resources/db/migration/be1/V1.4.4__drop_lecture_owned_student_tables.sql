-- V1.4.1에서 lecture 모듈이 만들었던 student/enrollment 테이블을 제거한다.
-- 학생/수강 등록 도메인은 student 모듈(V5.1.1)이 소유하는 것으로 정리됐고,
-- student/enrollment 테이블명이 서로 충돌해 V5.1.1이 적용될 수 없었다.
DROP TABLE IF EXISTS `enrollment`;
DROP TABLE IF EXISTS `student`;
