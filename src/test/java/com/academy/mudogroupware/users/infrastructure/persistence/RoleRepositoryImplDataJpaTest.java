package com.academy.mudogroupware.users.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.academy.mudogroupware.users.domain.model.Role;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(RoleRepositoryImpl.class)
class RoleRepositoryImplDataJpaTest {

    @Autowired
    private RoleRepositoryImpl roleRepository;

    @Autowired
    private PermissionJpaRepository permissionJpaRepository;

    @Test
    void findByIdReturnsRoleWithCurrentPermissionCodes() {
        permissionJpaRepository.save(
                PermissionEntity.builder().code("NOTICE:READ").resource("NOTICE").action("READ")
                        .description("공지 조회").build());
        Role saved = roleRepository.save(Role.create(1L, "강사", "설명", LocalDateTime.now()));

        roleRepository.updatePermissions(saved.getId(), Set.of("NOTICE:READ"));
        Role found = roleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPermissionCodes()).containsExactly("NOTICE:READ");
    }

    @Test
    void updatePermissionsReplacesEntirePermissionSet() {
        permissionJpaRepository.save(
                PermissionEntity.builder().code("NOTICE:READ").resource("NOTICE").action("READ").build());
        permissionJpaRepository.save(
                PermissionEntity.builder().code("CHAT:SEND").resource("CHAT").action("SEND").build());
        Role saved = roleRepository.save(Role.create(1L, "강사", "설명", LocalDateTime.now()));

        roleRepository.updatePermissions(saved.getId(), Set.of("NOTICE:READ", "CHAT:SEND"));
        Role afterFirstAssign = roleRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterFirstAssign.getPermissionCodes()).containsExactlyInAnyOrder("NOTICE:READ", "CHAT:SEND");

        roleRepository.updatePermissions(saved.getId(), Set.of("CHAT:SEND"));
        Role afterSecondAssign = roleRepository.findById(saved.getId()).orElseThrow();
        assertThat(afterSecondAssign.getPermissionCodes()).containsExactly("CHAT:SEND");
    }

    @Test
    void updatePermissionsCanClearAllPermissions() {
        permissionJpaRepository.save(
                PermissionEntity.builder().code("NOTICE:READ").resource("NOTICE").action("READ").build());
        Role saved = roleRepository.save(Role.create(1L, "강사", "설명", LocalDateTime.now()));
        roleRepository.updatePermissions(saved.getId(), Set.of("NOTICE:READ"));

        roleRepository.updatePermissions(saved.getId(), Set.of());

        Role found = roleRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getPermissionCodes()).isEmpty();
    }

    @Test
    void updateNameAndDescriptionUpdatesManagedEntity() {
        Role saved = roleRepository.save(Role.create(1L, "강사", "설명", LocalDateTime.now()));

        roleRepository.updateNameAndDescription(saved.getId(), "조교", "새 설명", "#FFFFFF");
        Role found = roleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("조교");
        assertThat(found.getDescription()).isEqualTo("새 설명");
        assertThat(found.getColor()).isEqualTo("#FFFFFF");
    }
}
