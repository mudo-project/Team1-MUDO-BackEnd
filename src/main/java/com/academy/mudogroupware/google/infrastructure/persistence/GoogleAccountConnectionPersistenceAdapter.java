package com.academy.mudogroupware.google.infrastructure.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.google.domain.model.GoogleAccountConnection;
import com.academy.mudogroupware.google.domain.repository.GoogleAccountConnectionRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GoogleAccountConnectionPersistenceAdapter implements GoogleAccountConnectionRepository {

    private final GoogleAccountConnectionJpaRepository googleAccountConnectionJpaRepository;
    private final GoogleTokenCipher googleTokenCipher;

    @Override
    public GoogleAccountConnection save(GoogleAccountConnection connection) {
        GoogleAccountConnectionEntity entity = connection.getId() != null
                ? updateExisting(connection)
                : toEntity(connection);
        return toDomain(googleAccountConnectionJpaRepository.save(entity));
    }

    @Override
    public Optional<GoogleAccountConnection> find() {
        return googleAccountConnectionJpaRepository.findAll().stream().findFirst().map(this::toDomain);
    }

    @Override
    public void deleteAll() {
        googleAccountConnectionJpaRepository.deleteAll();
    }

    private GoogleAccountConnectionEntity updateExisting(GoogleAccountConnection domain) {
        GoogleAccountConnectionEntity entity = googleAccountConnectionJpaRepository.getReferenceById(domain.getId());
        entity.updateCheckResult(domain.getLastCheckedAt(), domain.isFailed());
        return entity;
    }

    private GoogleAccountConnectionEntity toEntity(GoogleAccountConnection domain) {
        return GoogleAccountConnectionEntity.builder()
                .googleEmail(domain.getGoogleEmail())
                .connectedByUserId(domain.getConnectedByUserId())
                .scope(domain.getScope())
                .encryptedRefreshToken(googleTokenCipher.encrypt(domain.getRefreshToken()))
                .connectedAt(domain.getConnectedAt())
                .tokenExpiresAt(domain.getTokenExpiresAt())
                .lastCheckedAt(domain.getLastCheckedAt())
                .failed(domain.isFailed())
                .build();
    }

    private GoogleAccountConnection toDomain(GoogleAccountConnectionEntity entity) {
        return GoogleAccountConnection.restore(
                entity.getId(), entity.getGoogleEmail(), entity.getConnectedByUserId(),
                entity.getScope(), googleTokenCipher.decrypt(entity.getEncryptedRefreshToken()),
                entity.getConnectedAt(), entity.getTokenExpiresAt(), entity.getLastCheckedAt(), entity.isFailed());
    }
}
