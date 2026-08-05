package com.academy.mudogroupware.messenger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

class MessengerRepositoryFetchPlanTest {

    @Test
    void chatRoomMemberListQueryFetchesAllMembers() throws NoSuchMethodException {
        Query query = ChatRoomJpaRepository.class.getMethod("findAllByMember", Long.class, Long.class)
                .getAnnotation(Query.class);

        assertThat(query.value()).contains("join fetch r.members");
    }

    @Test
    void chatRoomFindByIdFetchesMembers() throws NoSuchMethodException {
        EntityGraph entityGraph = ChatRoomJpaRepository.class.getMethod("findById", Long.class)
                .getAnnotation(EntityGraph.class);

        assertThat(entityGraph.attributePaths()).containsExactly("members");
    }

    @Test
    void taskCardQueriesFetchAssignees() throws NoSuchMethodException {
        EntityGraph findAllGraph = ChatTaskCardJpaRepository.class
                .getMethod("findAllByChatRoomIdOrderByCreatedAtDescIdDesc", Long.class)
                .getAnnotation(EntityGraph.class);
        EntityGraph findByIdGraph = ChatTaskCardJpaRepository.class
                .getMethod("findById", Long.class)
                .getAnnotation(EntityGraph.class);

        assertThat(findAllGraph.attributePaths()).containsExactly("assignees");
        assertThat(findByIdGraph.attributePaths()).containsExactly("assignees");
    }
}
