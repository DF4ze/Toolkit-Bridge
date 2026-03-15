package fr.ses10doigts.toolkitbridge.model.dto.llm.message;


public enum MessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL("tool");

    private final String sRole;

    MessageRole( String sRole ){
        this.sRole = sRole;
    }

    public static String toRole(MessageRole role) {
        return role.sRole;
    }

    public static MessageRole fromRole(String role) {
        if( role == null || role.trim().isEmpty() )
            throw new IllegalArgumentException("Unsupported empty role ");

        for(MessageRole mRole : MessageRole.values()){
            if( mRole.sRole.equals(role) )
                return mRole;
        }

        throw new IllegalArgumentException("Unsupported role: " + role);
    }
}