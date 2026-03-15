package fr.ses10doigts.toolkitbridge.service.tool.bash;


import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.exception.ToolValidationException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashRequest;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BashSecurityService {

    private static final Map<String, String> ALLOWED_TOOLS = new HashMap<>();

    private final WorkspaceService workspaceService;


    public BashSecurityService(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;

        ALLOWED_TOOLS.put("ls", "/bin/ls");
        ALLOWED_TOOLS.put("pwd", "/bin/pwd");
        ALLOWED_TOOLS.put("echo", "/bin/echo");
        ALLOWED_TOOLS.put("cat", "/bin/cat");
        ALLOWED_TOOLS.put("head", "/usr/bin/head");
        ALLOWED_TOOLS.put("tail", "/usr/bin/tail");
        ALLOWED_TOOLS.put("grep", "/usr/bin/grep");
        ALLOWED_TOOLS.put("find", "/usr/bin/find");
        ALLOWED_TOOLS.put("date", "/bin/date");
        ALLOWED_TOOLS.put("whoami", "/usr/bin/whoami");
        ALLOWED_TOOLS.put("mvn", "/usr/bin/mvn"); // TODO Verify path
        ALLOWED_TOOLS.put("git", "/usr/bin/git");
    }

    public Set<String> getAllowedCommannds(){
        return ALLOWED_TOOLS.keySet();
    }

    public void validateRequest(BashRequest request) {
        if (request == null) {
            throw new ToolValidationException("Request cannot be null");
        }

        if (request.getTool() == null || request.getTool().isBlank()) {
            throw new ToolValidationException("Tool cannot be empty");
        }

        if (request.getTimeout() != null && (request.getTimeout() < 1 || request.getTimeout() > 60)) {
            throw new ToolValidationException("Timeout must be between 1 and 60 seconds");
        }

        if( ALLOWED_TOOLS.get(request.getTool()) == null ){
            throw new ForbiddenCommandException("Tool is forbidden: " + request.getTool());
        }
    }

    public void validateArgsForTool(String tool, List<String> args) throws IOException {
        switch (tool) {
            case "pwd", "date", "whoami" -> validateNoArgs(tool, args);
            case "echo" -> validateEchoArgs(args);
            case "cat" -> validateFileOnlyArgs(tool, args);
            case "head", "tail" -> validateHeadTailArgs(tool, args);
            case "ls" -> validateLsArgs(args);
            case "find" -> validateFindArgs(args);
            case "grep" -> validateGrepArgs(args);
            case "mvn" -> validateMvnArgs(args);
            case "git" -> validateGitArgs(args);
            default -> throw new ForbiddenCommandException("Tool not parametrized for validation: " + tool);
        }
    }

    public String getExecutable( String tool ){
        if( ALLOWED_TOOLS.get(tool) == null )
            throw new IllegalArgumentException("Tool is forbidden: " + tool);

        return ALLOWED_TOOLS.get(tool);
    }

    private void validateNoArgs(String tool, List<String> args) {
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' does not accept arguments");
        }
    }

    private void validateEchoArgs(List<String> args) {
        for (String arg : args) {
            workspaceService.validateCommandArg(arg);
        }
    }

    private void validateFileOnlyArgs(String tool, List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' requires at least one file path");
        }

        for (String arg : args) {
            workspaceService.validateRelativeWorkspacePathArg(arg);
        }
    }

    private void validateHeadTailArgs(String tool, List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' requires at least one argument");
        }

        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
            } else {
                workspaceService.validateRelativeWorkspacePathArg(arg);
            }
        }
    }

    private void validateLsArgs(List<String> args) throws IOException {
        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
            } else {
                workspaceService.validateRelativeWorkspacePathArg(arg);
            }
        }
    }

    private void validateFindArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            workspaceService.validateRelativeWorkspacePathArg(".");
            return;
        }

        boolean pathConsumed = false;

        for (String arg : args) {
            if (!pathConsumed && !arg.startsWith("-")) {
                workspaceService.validateRelativeWorkspacePathArg(arg);
                pathConsumed = true;
            } else {
                workspaceService.validateCommandArg(arg);
            }
        }
    }

    private void validateGrepArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool 'grep' requires at least a pattern");
        }

        boolean patternConsumed = false;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
                continue;
            }

            if (!patternConsumed) {
                workspaceService.validateCommandArg(arg);
                patternConsumed = true;
                continue;
            }

            workspaceService.validateRelativeWorkspacePathArg(arg);
        }
    }

    private void validateGitArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool 'git' requires at least a command");
        }

        // First argument should be the git command (e.g., "add", "commit", "status")
        String gitCommand = args.getFirst();
        workspaceService.validateCommandArg(gitCommand);

        boolean isSubcommand = !"version".equals(gitCommand) && !"help".equals(gitCommand);
        boolean argsConsumed = !isSubcommand; // Some commands like 'git version' take no further args

        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);

            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
                continue;
            }

            if (!argsConsumed) {
                // For subcommands like 'git add', validate the path/file being added
                workspaceService.validateRelativeWorkspacePathArg(arg);
                argsConsumed = true;
                continue;
            }

            workspaceService.validateRelativeWorkspacePathArg(arg);

        }
    }

    private void validateMvnArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool 'mvn' requires at least a phase or command");
        }

        // Maven typically starts with a phase/command like 'clean', 'install', 'test'
        String mvnCommand = args.getFirst();
        workspaceService.validateCommandArg(mvnCommand);

        boolean hasCommand = !"version".equals(mvnCommand) && !"help".equals(mvnCommand);
        boolean goalsConsumed = !hasCommand; // Commands like 'mvn version' take no further args

        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);

            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
                continue;
            }else{
                workspaceService.validateRelativeWorkspacePathArg(arg);
            }

            if (!goalsConsumed) {
                // Maven goal (e.g., 'mvn clean install', here 'install' is the goal)
                workspaceService.validateCommandArg(arg);
                goalsConsumed = true;
            }
        }
    }
}
