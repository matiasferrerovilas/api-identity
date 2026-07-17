package com.api.identity.records.workspaces;

import java.io.Serializable;
import java.util.List;

public record WorkspaceDTO(Long id,
                           String name,
                           String owner,
                           Metadata metadata) implements Serializable {

    public record Metadata(List<String> members) implements Serializable {
    }
}
