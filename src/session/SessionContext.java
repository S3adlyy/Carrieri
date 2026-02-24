package session;
import entities.User;

public final class SessionContext {
    private static User currentUser;
    private static Integer profileTargetUserId;
    private static boolean adminViewingProfile;

    private SessionContext() {}

    public static User getCurrentUser() { return currentUser; }
    public static void setCurrentUser(User u) { currentUser = u; }
    public static void clear() { currentUser = null; }

    public static void setProfileTargetUserId(Integer id) { profileTargetUserId = id; }
    public static Integer getProfileTargetUserId() { return profileTargetUserId; }
    public static void clearProfileTargetUserId() { profileTargetUserId = null; }

    public static void setAdminViewingProfile(boolean v) { adminViewingProfile = v; }
    public static boolean isAdminViewingProfile() { return adminViewingProfile; }


    public static boolean isLoggedIn() { return currentUser != null; }
}

