package com.academy.mudogroupware.attendance.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;

class LeaveGrantPersistenceAdapterTest {

    @Test
    void returnsTrueWhenGrantIsInserted() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LeaveGrantPersistenceAdapter adapter = new LeaveGrantPersistenceAdapter(
                mock(LeaveGrantJpaRepository.class), jdbcTemplate);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        assertTrue(adapter.saveIfAbsent(grant()));
    }

    @Test
    void returnsFalseWhenSameGrantAlreadyExists() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        LeaveGrantPersistenceAdapter adapter = new LeaveGrantPersistenceAdapter(
                mock(LeaveGrantJpaRepository.class), jdbcTemplate);
        when(jdbcTemplate.update(anyString(), any(Object[].class)))
                .thenThrow(new DuplicateKeyException("duplicate grant"));

        assertFalse(adapter.saveIfAbsent(grant()));
    }

    private LeaveGrant grant() {
        return LeaveGrant.annual(
                10L, LocalDate.of(2026, 8, 6), LocalDateTime.of(2026, 8, 6, 0, 5));
    }
}
