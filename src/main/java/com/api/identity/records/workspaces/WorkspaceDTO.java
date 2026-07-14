package com.api.identity.records.workspaces;

import java.io.Serializable;

public record WorkspaceDTO(Long id, String name, String owner) implements Serializable {
}
