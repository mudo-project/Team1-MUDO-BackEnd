package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.lecture.domain.model.Term;

public interface TermRepository {

    Optional<Term> findByName(String name);

    List<Term> findAllById(List<Long> ids);

    Term save(Term term);
}
