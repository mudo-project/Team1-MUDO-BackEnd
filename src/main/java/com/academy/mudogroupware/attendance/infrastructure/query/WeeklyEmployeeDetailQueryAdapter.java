package com.academy.mudogroupware.attendance.infrastructure.query;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.application.port.WeeklyEmployeeDetail;
import com.academy.mudogroupware.attendance.application.port.WeeklyEmployeeDetailQueryPort;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WeeklyEmployeeDetailQueryAdapter implements WeeklyEmployeeDetailQueryPort {

    private static final String FIND_EMPLOYEE = """
            SELECT u.id, u.name, r.name AS position, ar.work_date,
                   ar.clock_in_at, ar.clock_out_at, ar.status
            FROM users u
            LEFT JOIN role r ON r.role_id = u.role_id
            LEFT JOIN attendance_record ar
              ON ar.academy_id = u.academy_id
             AND ar.user_id = u.id
             AND ar.work_date BETWEEN ? AND ?
            WHERE u.academy_id = ?
              AND u.id = ?
              AND u.status = 'ACTIVE'
            ORDER BY ar.work_date ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<WeeklyEmployeeDetail> findByEmployee(
            Long academyId, Long userId, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(FIND_EMPLOYEE, (rs, rowNum) -> {
            Timestamp clockIn = rs.getTimestamp("clock_in_at");
            Timestamp clockOut = rs.getTimestamp("clock_out_at");
            String status = rs.getString("status");
            return new WeeklyEmployeeDetail(
                    rs.getLong("id"), rs.getString("name"), rs.getString("position"),
                    rs.getDate("work_date") == null ? null : rs.getDate("work_date").toLocalDate(),
                    clockIn == null ? null : clockIn.toLocalDateTime(),
                    clockOut == null ? null : clockOut.toLocalDateTime(),
                    status == null ? null : AttendanceStatus.valueOf(status));
        }, startDate, endDate, academyId, userId);
    }
}
