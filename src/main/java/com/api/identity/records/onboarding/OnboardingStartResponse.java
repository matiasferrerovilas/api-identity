package com.api.identity.records.onboarding;

import com.api.identity.records.user.UserMe;
import com.api.identity.records.workspaces.WorkspaceAdded;

import java.util.List;

public record OnboardingStartResponse(UserMe user, List<WorkspaceAdded> workspaces) {
}
