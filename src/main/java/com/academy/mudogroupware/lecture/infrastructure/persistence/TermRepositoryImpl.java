package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.lecture.domain.model.Term;
import com.academy.mudogroupware.lecture.domain.repository.TermRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TermRepositoryImpl implements TermRepository {

    private final TermJpaRepository termJpaRepository;

    @Override
    public Optional<Term> findByName(String name) {
        return termJpaRepository.findByName(name).map(this::toDomain);
    }

    @Override
    public List<Term> findAllById(List<Long> ids) {
        return termJpaRepository.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Term save(Term term) {
        TermEntity entity = TermEntity.builder()
                .name(term.getName())
                .build();
        return toDomain(termJpaRepository.save(entity));
    }

    private Term toDomain(TermEntity entity) {
        return Term.restore(entity.getId(), entity.getName(), entity.getCreatedAt());
    }
}
