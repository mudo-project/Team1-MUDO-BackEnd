package com.academy.mudogroupware.auth.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "refresh_tokens")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "refresh_token_id")
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "refresh_token", nullable = false, length = 512, unique = true)
  private String refreshToken;

  @LastModifiedDate
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public RefreshTokenJpaEntity(Long userId, String token) {
    this.userId = userId;
    this.refreshToken = token;
  }

  public void replace(String token) {
    this.refreshToken = token;
  }
}
