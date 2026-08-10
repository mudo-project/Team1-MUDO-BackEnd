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
