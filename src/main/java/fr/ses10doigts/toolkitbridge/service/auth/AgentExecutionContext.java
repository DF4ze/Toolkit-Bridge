package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;

import java.util.function.Supplier;

public interface AgentExecutionContext {
    <T> T runAs(AuthenticatedAgent bot, Supplier<T> action);
    void runAs(AuthenticatedAgent bot, Runnable action);
}