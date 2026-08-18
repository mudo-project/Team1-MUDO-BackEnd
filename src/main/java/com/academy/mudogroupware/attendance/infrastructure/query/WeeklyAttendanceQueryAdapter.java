package com.academy.mudogroupware.attendance.infrastructure.query;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.application.port.WeeklyAttendanceEmployee;
import com.academy.mudogroupware.attendance.application.port.WeeklyAttendanceQueryPort;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WeeklyAttendanceQueryAdapter implements WeeklyAttendanceQueryPort {

    private static final String FIND_EMPLOYEES = """
            SELECT u.id, u.name, r.name AS role_name,
                   ar.work_date, ar.clock_in_at, ar.clock_out_at, ar.status
            FROM users u
            LEFT JOIN role r ON r.role_id = u.role_id
            LEFT JOIN attendance_record ar
              ON ar.user_id = u.id
             AND ar.work_date BETWEEN ? AND ?
            WHERE u.status = 'ACTIVE'
              AND u.id <> ?
              AND NOT (u.account_type = 'ADMIN' AND u.admin_scope = 'PLATFORM')
            ORDER BY u.name ASC, u.id ASC, ar.work_date ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<WeeklyAttendanceEmployee> findEmployees(
            Long ownerUserId, LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query(FIND_EMPLOYEES, (rs, rowNum) -> {
            Timestamp clockIn = rs.getTimestamp("clock_in_at");
            Timestamp clockOut = rs.getTimestamp("clock_out_at");
            String status = rs.getString("status");
            return new WeeklyAttendanceEmployee(
                    rs.getLong("id"), rs.getString("name"), rs.getString("role_name"),
                    rs.getDate("work_date") == null ? null : rs.getDate("work_date").toLocalDate(),
                    clockIn == null ? null : clockIn.toLocalDateTime(),
                    clockOut == null ? null : clockOut.toLocalDateTime(),
                    status == null ? null : AttendanceStatus.valueOf(status));
        }, startDate, endDate, ownerUserId);
    }
}
