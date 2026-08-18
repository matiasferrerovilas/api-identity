package com.api.identity.demo;

import com.api.identity.entities.User;
import com.api.identity.entities.Workspace;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.UserRole;
import com.api.identity.enums.UserType;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.repositories.UserRepository;
import com.api.identity.repositories.WorkspaceMemberRepository;
import com.api.identity.repositories.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Seeds a fixed set of demo data when the {@code demo} Spring profile is active.
 *
 * <p>This is a suite-wide convention: api-movements and api-keep each have their own
 * {@code demo} profile that seeds domain data (movements, goals, files) against workspace
 * id {@value #DEMO_WORKSPACE_ID} in this service, assuming that workspace already exists.
 * Do not change the fixed id without updating those repos too.
 *
 * <p>Guarded by {@link Profile @Profile("demo")}: this bean is never registered outside the
 * {@code demo} profile, so it cannot run in {@code dev}, {@code prod}, or the default profile.
 *
 * <p>Idempotent: re-running the app in {@code demo} profile (e.g. a restart) does not
 * duplicate the demo user, workspace, or membership.
 */
@Component
@Profile("demo")
@Slf4j
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    /**
     * Fixed id for the shared demo workspace, relied upon by the sibling apps' demo seeders.
     */
    static final long DEMO_WORKSPACE_ID = 1L;

    private static final String DEMO_EMAIL = "demo@example.com";
    private static final String DEMO_GIVEN_NAME = "Demo";
    private static final String DEMO_FAMILY_NAME = "User";
    private static final String DEMO_WORKSPACE_NAME = "Demo Workspace";

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var user = findOrCreateDemoUser();
        var workspace = findOrCreateDemoWorkspace();
        var membershipCreated = ensureDemoMembership(workspace, user);

        log.info(
                "Demo profile seed ready — user='{}' (id={}), workspace id={}, membership {}",
                user.getEmail(), user.getId(), workspace.getId(),
                membershipCreated ? "created" : "already existed");
    }

    private User findOrCreateDemoUser() {
        return userRepository.findByEmail(DEMO_EMAIL).orElseGet(() -> {
            var created = userRepository.save(User.builder()
                    .email(DEMO_EMAIL)
                    .givenName(DEMO_GIVEN_NAME)
                    .familyName(DEMO_FAMILY_NAME)
                    .userType(UserType.PERSONAL)
                    .userRoles(Set.of(UserRole.ROLE_FAMILY))
                    .build());
            log.info("Demo profile: created demo user '{}' (id={})", DEMO_EMAIL, created.getId());
            return created;
        });
    }

    private Workspace findOrCreateDemoWorkspace() {
        return workspaceRepository.findById(DEMO_WORKSPACE_ID)
                .orElseGet(this::insertDemoWorkspaceWithFixedId);
    }

    /**
     * {@link Workspace#getId()} uses {@code GenerationType.IDENTITY}, so Hibernate refuses to
     * persist a new entity with a pre-assigned id under that strategy (it always defers id
     * generation to the database). To honor the suite-wide fixed id=1 convention we insert the
     * row with a plain JDBC statement instead: MySQL allows an explicit value for an
     * AUTO_INCREMENT column and advances the table's auto-increment counter past it, so any
     * later, normally-generated workspace still gets an id greater than 1.
     */
    private Workspace insertDemoWorkspaceWithFixedId() {
        jdbcTemplate.update(
                "INSERT INTO workspaces (id, name, created_at, is_active) VALUES (?, ?, NOW(), TRUE)",
                DEMO_WORKSPACE_ID, DEMO_WORKSPACE_NAME);
        log.info("Demo profile: created demo workspace id={}", DEMO_WORKSPACE_ID);

        return workspaceRepository.findById(DEMO_WORKSPACE_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to seed demo workspace id=" + DEMO_WORKSPACE_ID));
    }

    private boolean ensureDemoMembership(Workspace workspace, User user) {
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId())) {
            return false;
        }

        workspaceMemberRepository.save(WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(WorkspaceRole.OWNER)
                .build());
        return true;
    }
}
