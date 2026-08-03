package com.academy.mudogroupware.notice.infrastructure.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * User 도메인 모듈이 생기기 전까지, 공지 작성자 정보 조회를 위한 users 테이블의 읽기 전용 최소 매핑.
 * User 모듈이 생기면 {@link NoticeAuthorDirectoryPortAdapter}와 함께 제거하고 그 모듈을 통해 조회한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfoEntity {

    @Id
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "resign_date")
    private LocalDate resignDate;
}
