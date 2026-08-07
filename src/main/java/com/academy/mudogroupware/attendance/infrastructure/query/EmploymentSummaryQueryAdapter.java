package com.academy.mudogroupware.attendance.infrastructure.query;

import java.util.Optional;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.attendance.application.port.EmploymentSummaryPort;
import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EmploymentSummaryQueryAdapter implements EmploymentSummaryPort {
    private final EntityManager entityManager;

    @Override
    public Optional<EmploymentSummary> findByUserIdAndAcademyId(Long userId, Long academyId) {
        List<?> results = entityManager.createNativeQuery(
                        "select date(joined_at) from users "
                                + "where id = :userId and academy_id = :academyId "
                                + "and joined_at is not null")
                .setParameter("userId", userId)
                .setParameter("academyId", academyId)
                .getResultList();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new EmploymentSummary(toLocalDate(results.get(0))));
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        return ((Date) value).toLocalDate();
    }
}
