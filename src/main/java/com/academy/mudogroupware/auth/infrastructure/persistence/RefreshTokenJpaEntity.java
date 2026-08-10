package com.academy.mudogroupware.auth.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshTokenJpaEntity extends BaseTimeEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "refresh_token_id")
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "refresh_token", nullable = false, length = 512, unique = true)
  private String refreshToken;

  public RefreshTokenJpaEntity(Long userId, String token) {
    this.userId = userId;
    this.refreshToken = token;
  }

  public void replace(String token) {
    this.refreshToken = token;
  }
}
