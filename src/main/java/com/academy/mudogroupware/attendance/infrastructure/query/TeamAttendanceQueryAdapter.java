package com.academy.mudogroupware.attendance.infrastructure.query;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.attendance.application.port.TeamAttendanceEmployee;
import com.academy.mudogroupware.attendance.application.port.TeamAttendanceQueryPort;

import lombok.RequiredArgsConstructor;

/**
 * Consumer: attendance
 * Purpose: 오늘 팀 근태 화면에 필요한 활성 직원과 출근 시각 조회
 */
@Repository
@RequiredArgsConstructor
public class TeamAttendanceQueryAdapter implements TeamAttendanceQueryPort {

    private static final String FIND_EMPLOYEES_WITH_ATTENDANCE = """
            SELECT u.id, u.name, ar.clock_in_at
            FROM users u
            LEFT JOIN attendance_record ar
              ON ar.user_id = u.id
             AND ar.work_date = ?
            WHERE u.status = 'ACTIVE'
              AND u.id <> ?
            ORDER BY u.name ASC, u.id ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<TeamAttendanceEmployee> findEmployeesWithAttendance(
            Long ownerUserId, LocalDate workDate) {
        return jdbcTemplate.query(
                FIND_EMPLOYEES_WITH_ATTENDANCE,
                (resultSet, rowNumber) -> new TeamAttendanceEmployee(
                        resultSet.getLong("id"),
                        resultSet.getString("name"),
                        resultSet.getTimestamp("clock_in_at") == null
                                ? null
                                : resultSet.getTimestamp("clock_in_at").toLocalDateTime()),
                workDate, ownerUserId);
    }
}
