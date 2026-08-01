title: "[Announcement] "
labels: ["announcement"]
body:
  - type: markdown
    attributes:
      value: |
        중요한 공지, 업데이트, 릴리스 안내를 공유하는 공간입니다.
        필요한 내용을 작성해 팀원들이 빠르게 이해할 수 있게 해주세요.

  - type: input
    id: summary
    attributes:
      label: 공지 제목
      description: 공지 내용을 한 줄로 요약해주세요.
      placeholder: 예) v1.2.0 배포 완료 및 API 응답 형식 변경 안내
    validations:
      required: true

  - type: textarea
    id: details
    attributes:
      label: 공지 내용
      description: 공유해야 할 핵심 내용을 자세히 적어주세요.
      placeholder: |
        예)
        - 무엇이 변경되었는지
        - 언제부터 적용되는지
        - 어떤 영향이 있는지
    validations:
      required: true

  - type: textarea
    id: impact
    attributes:
      label: 영향 범위
      description: 관련된 기능, 사용자, 팀, 서비스 범위를 적어주세요.
      placeholder: 예) backend API, frontend 로그인 페이지, QA 테스트 시나리오
    validations:
      required: false

  - type: textarea
    id: action_items
    attributes:
      label: 필요한 후속 조치
      description: 팀원들이 해야 할 일이 있다면 적어주세요.
      placeholder: 예) 프론트엔드 응답 필드명 수정, QA 재테스트 진행
    validations:
      required: false

  - type: input
    id: effective_date
    attributes:
      label: 적용 시점
      description: 적용 날짜나 배포 시점을 적어주세요.
      placeholder: 예) 2026-03-11 배포 완료 / 2026-03-15 적용 예정
    validations:
      required: false
