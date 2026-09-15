package hgds.epicgrief;

public final class GuardResult {
    private static final GuardResult ALLOWED = new GuardResult(true, null);

    private final boolean allowed;
    private final String messageKey;

    private GuardResult(boolean allowed, String messageKey) {
        this.allowed = allowed;
        this.messageKey = messageKey;
    }

    public static GuardResult allowed() {
        return ALLOWED;
    }

    public static GuardResult denied(String messageKey) {
        return new GuardResult(false, messageKey);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
