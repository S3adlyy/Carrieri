package session;

public final class ProfileViewContext {
    private static Integer targetUserId = null;

    private ProfileViewContext() {}

    public static Integer getTargetUserId() { return targetUserId; }

    public static void viewUser(Integer userId) { targetUserId = userId; }

    public static void clear() { targetUserId = null; }
}
