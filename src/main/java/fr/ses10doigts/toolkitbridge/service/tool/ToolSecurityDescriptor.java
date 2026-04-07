package fr.ses10doigts.toolkitbridge.service.tool;

public record ToolSecurityDescriptor(
        boolean webAccess,
        boolean scriptedExecution
) {

    public static ToolSecurityDescriptor standard() {
        return new ToolSecurityDescriptor(false, false);
    }

    public static ToolSecurityDescriptor scripted() {
        return new ToolSecurityDescriptor(false, true);
    }

    public static ToolSecurityDescriptor webEnabled() {
        return new ToolSecurityDescriptor(true, false);
    }
}
