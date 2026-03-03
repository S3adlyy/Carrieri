package security;

public enum Role {
    CANDIDATE, RECRUITER, ADMIN;

    public static Role fromDb(String s) {
        return Role.valueOf(s.trim().toUpperCase());
    }
}
