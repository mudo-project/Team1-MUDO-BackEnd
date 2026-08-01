package com.academy.mudogroupware.auth.infrastructure.persistence;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Table(name="refresh_tokens") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) public class RefreshTokenJpaEntity { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="refresh_token_id") private Long id; @Column(name="user_id",nullable=false,unique=true) private Long userId; @Column(name="refresh_token",nullable=false,length=512,unique=true) private String refreshToken; @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
 public RefreshTokenJpaEntity(Long userId,String token){this.userId=userId;this.refreshToken=token;this.createdAt=LocalDateTime.now();} public void replace(String token){this.refreshToken=token;this.createdAt=LocalDateTime.now();}
}
