# lecture API Flow

## 강의 등록

```text
POST /api/lectures
-> LectureController.createLecture
-> CreateLectureRequest.toCommand
-> CreateLectureService.createLecture
-> TermRepository.findByAcademyIdAndName or save
-> SubjectRepository.findByAcademyIdAndName or save
-> ClassroomRepository.findByAcademyIdAndName or save
-> LectureRepository.existsOverlappingSchedule
-> LectureRepository.save
```

교실은 별도 관리 탭 없이 강의 등록 과정에서 이름 기준으로 생성/재사용한다.

## 강의 목록 조회

```text
GET /api/lectures
-> LectureController.getLectures
-> LectureQueryService.getLectures
-> LectureRepository.findAll
-> Term/Subject/Classroom 이름 배치 조회
-> TeacherDirectoryPort.findTeachers
-> EnrolledStudentsPort.findByLectureId
-> LectureSummaryResponse
```

`TeacherDirectoryPort`는 lecture가 정의하고 users 모듈의 `LectureTeacherDirectoryAdapter`가 구현한다. 응답에는 `teacherId`, `teacherName`이 함께 포함된다.

## 강의 상세 조회

```text
GET /api/lectures/{lectureId}
-> LectureQueryService.getLectureDetail
-> LectureRepository.findById
-> academyId 소유권 검증
-> Term/Subject/Classroom 이름 조회
-> TeacherDirectoryPort.findTeachers
-> EnrolledStudentsPort.findByLectureId
-> LectureDetailResponse
```

student 데이터의 소유권은 student 모듈에 있다. lecture는 수강 학생 Entity/Repository를 직접 참조하지 않고 Port로 Projection만 받는다.

## 강의 수정

```text
PATCH /api/lectures/{lectureId}
-> LectureController.updateLecture
-> UpdateLectureRequest.toCommand
-> UpdateLectureService.updateLecture
-> LectureRepository.findById
-> TermRepository.findByName or save
-> SubjectRepository.findByName or save
-> ClassroomRepository.findByNameForUpdate or save
-> LectureRepository.existsOverlapExcludingLecture
-> LectureRepository.save
```

강의 수정은 등록과 같은 요청 형태를 사용한다. 단, 시간 충돌 검사는 수정 대상 강의 id를 제외하고 수행한다.

## 강의 삭제

```text
DELETE /api/lectures/{lectureId}
-> LectureController.deleteLecture
-> DeleteLectureService.deleteLecture
-> LectureRepository.findById
-> LectureRepository.deleteById
```

강의 삭제는 `deletedAt`을 채우는 소프트 삭제다. 삭제된 강의는 목록/상세 조회, id 목록 조회, 교실 시간 충돌 검사, 매출 리포트용 강의 조회에서 제외한다.
