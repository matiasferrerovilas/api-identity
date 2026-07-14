package com.api.identity.services;

import com.api.identity.entities.User;
import com.api.identity.entities.Workspace;
import com.api.identity.entities.WorkspaceMember;
import com.api.identity.enums.WorkspaceRole;
import com.api.identity.exceptions.BusinessException;
import com.api.identity.exceptions.EntityNotFoundException;
import com.api.identity.mappers.WorkspaceMapper;
import com.api.identity.records.workspaces.AddWorkspaceRecord;
import com.api.identity.records.workspaces.WorkspaceAdded;
import com.api.identity.records.workspaces.WorkspaceDTO;
import com.api.identity.repositories.UserRepository;
import com.api.identity.repositories.WorkspaceRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceAddService {

    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMapper workspaceMapper;

    @Transactional
    public List<WorkspaceAdded> createWorkspaces(AddWorkspaceRecord workspacesToAdd) {
        var userId = workspacesToAdd.userId();
        var workspaces = workspacesToAdd.workspaces();
        if (workspaces.isEmpty()) {
            throw new BusinessException("Debe indicar al menos un workspace a crear");
        }

        var owner = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario inexistente"));

        var names = workspaces.stream()
                .map(this::sanitizeName)
                .toList();

        this.verifyNoDuplicates(owner, names);

        var workspacesTobeAdded = names.stream()
                .map(name -> this.buildWorkspace(name, owner))
                .toList();

        return workspaceRepository.saveAll(workspacesTobeAdded).stream()
                .map(workspace -> new WorkspaceAdded(workspace.getId(), workspace.getName()))
                .toList();
    }

    private String sanitizeName(String description) {
        if (description == null || description.isBlank()) {
            throw new BusinessException("La descripción del workspace no puede estar vacía");
        }
        return description.trim();
    }

    private void verifyNoDuplicates(User owner, List<String> names) {
        var distinctNames = names.stream().distinct().count();
        if (distinctNames != names.size()) {
            throw new BusinessException("Hay workspaces con nombre repetido en la solicitud");
        }

        var existing = workspaceRepository.findByOwnerIdAndNameIn(owner.getId(), names);
        if (!existing.isEmpty()) {
            var existingNames = existing.stream()
                    .map(Workspace::getName)
                    .collect(Collectors.joining(", "));
            throw new BusinessException("Ya existe un workspace con ese nombre: " + existingNames);
        }
    }

    private Workspace buildWorkspace(String name, User owner) {
        var workspace = Workspace.builder()
                .name(name)
                .owner(owner)
                .build();

        workspace.getMembers().add(WorkspaceMember.builder()
                .user(owner)
                .workspace(workspace)
                .role(WorkspaceRole.OWNER)
                .build());

        return workspace;
    }

    public List<WorkspaceDTO> getWorkspaces(Long userId) {
        return workspaceRepository.findByUserIn(userId).stream()
                .map(workspaceMapper::toDTO)
                .toList();
    }
}
