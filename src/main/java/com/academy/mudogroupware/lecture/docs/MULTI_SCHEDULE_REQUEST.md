# Multi Schedule Request

`POST /api/lectures` and `PATCH /api/lectures/{lectureId}` support multiple weekly schedule rows.
Use the `schedules` array for lectures such as Monday/Wednesday/Friday classes.

## Request Body Example

```json
{
  "name": "High 1 Math Regular",
  "classType": "CLASS",
  "classroomCode": "A101",
  "grade": "HIGH_1",
  "teacherName": "Teacher Kim",
  "subjectName": "Math",
  "termName": "2026 Spring",
  "feeType": "PER_MONTH",
  "feeAmount": 300000,
  "schedules": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "19:00:00",
      "endTime": "21:00:00"
    },
    {
      "dayOfWeek": "WEDNESDAY",
      "startTime": "19:00:00",
      "endTime": "21:00:00"
    },
    {
      "dayOfWeek": "FRIDAY",
      "startTime": "19:00:00",
      "endTime": "21:00:00"
    }
  ]
}
```

## Request Field

| name | type | required | description |
|---|---|---:|---|
| `schedules` | `ScheduleRequest[]` | false | Multiple weekly schedule rows. Prefer this field for new clients. |
| `schedules[].dayOfWeek` | `DayOfWeek` | true | Class day. Example: `MONDAY`, `WEDNESDAY`, `FRIDAY`. |
| `schedules[].startTime` | `LocalTime` | true | Start time. Format: `HH:mm:ss`. |
| `schedules[].endTime` | `LocalTime` | true | End time. Must be later than `startTime`. |
| `dayOfWeek` | `DayOfWeek` | false | Legacy single-schedule field. Used only when `schedules` is empty. |
| `startTime` | `LocalTime` | false | Legacy single-schedule start time. |
| `endTime` | `LocalTime` | false | Legacy single-schedule end time. |

## Rules

- If `schedules` has values, every schedule row is saved.
- If `schedules` is empty, `dayOfWeek`, `startTime`, and `endTime` are saved as one schedule row.
- If both formats are missing, the request is rejected with `400 COMMON_400_1`.
- Existing overlap validation still applies to every schedule row.
