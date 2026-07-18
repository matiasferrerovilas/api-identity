package com.api.identity.mappers;

import com.api.identity.entities.WorkspaceMember;
import com.api.identity.records.workspaces.WorkspaceMemberDTO;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkspaceMemberMapper {

    public List<WorkspaceMemberDTO> toDTO(List<WorkspaceMember> workspaceMembers, Long userId) {
        return workspaceMembers.stream()
                .collect(Collectors.groupingBy(m -> m.getWorkspace().getId(), LinkedHashMap::new, Collectors.toList()))
                .values().stream()
                .map(members -> toWorkspaceMemberDTO(members, userId))
                .toList();
    }

    private WorkspaceMemberDTO toWorkspaceMemberDTO(List<WorkspaceMember> members, Long userId) {
        var self = members.stream()
                .filter(m -> m.getUser().getId().equals(userId))
                .findFirst()
                .orElseGet(() -> members.get(0));
        var workspace = self.getWorkspace();

        return new WorkspaceMemberDTO(
                self.getId(),
                workspace.getId(),
                workspace.getName(),
                new WorkspaceMemberDTO.Metadata(
                        members.stream().map(m -> m.getUser().getEmail()).toList(),
                        self.getRole(),
                        self.getJoinedAt()));
    }
}
