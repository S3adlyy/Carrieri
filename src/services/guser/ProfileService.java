package services.guser;

import entities.guser.User;
import utils.MyDatabase;

import java.sql.*;

public class ProfileService {

    private final Connection connection;

    public ProfileService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public void updateCandidateProfile(User u) throws SQLException {
        String sql = """
            UPDATE user
            SET first_name = ?, last_name = ?, location = ?, phone = ?,
                headline = ?, bio = ?,
                school = ?, degree = ?, field_of_study = ?, graduation_year = ?,
                hard_skills = ?, soft_skills = ?, github_url = ?, portfolio_url = ?
            WHERE id = ? AND roles = 'CANDIDATE'
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, req(u.getFirstname(), "First name is required."));
            ps.setString(2, req(u.getLastname(), "Last name is required."));
            ps.setString(3, emptyToNull(u.getLocation()));
            ps.setString(4, emptyToNull(u.getPhone()));

            ps.setString(5, emptyToNull(u.getHeadline()));
            ps.setString(6, emptyToNull(u.getBio()));

            ps.setString(7, emptyToNull(u.getSchool()));
            ps.setString(8, emptyToNull(u.getDegree()));
            ps.setString(9, emptyToNull(u.getFieldofstudy()));
            if (u.getGraduationyear() == null) ps.setNull(10, Types.SMALLINT);
            else ps.setInt(10, u.getGraduationyear());

            ps.setString(11, emptyToNull(u.getHardskills()));
            ps.setString(12, emptyToNull(u.getSoftskills()));
            ps.setString(13, emptyToNull(u.getGithuburl()));
            ps.setString(14, emptyToNull(u.getPortfoliourl()));

            ps.setInt(15, u.getId());

            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("Update failed (candidate not found).");
        }
    }

    public void updateRecruiterProfile(User u) throws SQLException {
        String sql = """
            UPDATE user
            SET first_name = ?, last_name = ?, location = ?, phone = ?,
                org_name = ?, website_url = ?, description = ?
            WHERE id = ? AND roles = 'RECRUITER'
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, req(u.getFirstname(), "First name is required."));
            ps.setString(2, req(u.getLastname(), "Last name is required."));
            ps.setString(3, emptyToNull(u.getLocation()));
            ps.setString(4, emptyToNull(u.getPhone()));

            ps.setString(5, emptyToNull(u.getOrgname()));
            ps.setString(6, emptyToNull(u.getWebsiteurl()));
            ps.setString(7, emptyToNull(u.getDescription()));

            ps.setInt(8, u.getId());

            int n = ps.executeUpdate();
            if (n == 0) throw new IllegalArgumentException("Update failed (recruiter not found).");
        }
    }

    public void updateProfilePicKey(int userId, String s3Key) throws SQLException {
        String sql = "UPDATE user SET profile_pic = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s3Key);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateLogoKey(int userId, String s3Key) throws SQLException {
        String sql = "UPDATE user SET logo_url = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, s3Key);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private User map(ResultSet rs) throws SQLException {
        // IMPORTANT: ResultSet.getString(columnLabel) must match SQL column labels exactly. [web:223]
        User u = new User();
        u.setId(rs.getInt("id"));

        u.setFirstname(rs.getString("first_name"));
        u.setLastname(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));

        u.setPasswordhash(rs.getString("password_hash"));
        u.setRoles(rs.getString("roles"));
        u.setIsactive(rs.getInt("is_active"));

        u.setType(rs.getString("type"));
        u.setHeadline(rs.getString("headline"));
        u.setBio(rs.getString("bio"));
        u.setLocation(rs.getString("location"));
        u.setVisibility(rs.getString("visibility"));

        u.setNiveau(rs.getString("niveau"));
        try { u.setScoreglobal(rs.getDouble("score_global")); } catch (Exception ignored) {}

        u.setOrgname(rs.getString("org_name"));
        u.setDescription(rs.getString("description"));
        u.setWebsiteurl(rs.getString("website_url"));
        u.setLogourl(rs.getString("logo_url"));
        u.setProfilepic(rs.getString("profile_pic"));

        u.setSchool(rs.getString("school"));
        u.setDegree(rs.getString("degree"));
        u.setFieldofstudy(rs.getString("field_of_study"));

        int gy = rs.getInt("graduation_year");
        u.setGraduationyear(rs.wasNull() ? null : gy);

        u.setHardskills(rs.getString("hard_skills"));
        u.setSoftskills(rs.getString("soft_skills"));
        u.setGithuburl(rs.getString("github_url"));
        u.setPortfoliourl(rs.getString("portfolio_url"));
        u.setPhone(rs.getString("phone"));

        return u;
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String req(String s, String msg) {
        if (s == null || s.trim().isEmpty()) throw new IllegalArgumentException(msg);
        return s.trim();
    }

    public java.util.List<User> suggestPeopleYouMayKnow(int currentUserId, String currentRole) throws java.sql.SQLException {
        // Candidate sees recruiters, recruiter sees candidates (simple LinkedIn-like suggestion)
        String targetRole = "CANDIDATE".equals(currentRole) ? "RECRUITER" : "CANDIDATE";

        String sql = """
        SELECT id, first_name, last_name, roles, headline, org_name, profile_pic, logo_url
        FROM user
        WHERE id <> ?
          AND roles = ?
          AND is_active = 1
        ORDER BY RAND()
        LIMIT 6
        """;

        try (java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, targetRole);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                java.util.ArrayList<User> out = new java.util.ArrayList<>();
                while (rs.next()) {
                    User u = new User();
                    u.setId(rs.getInt("id"));
                    u.setFirstname(rs.getString("first_name"));
                    u.setLastname(rs.getString("last_name"));
                    u.setRoles(rs.getString("roles"));
                    u.setHeadline(rs.getString("headline"));
                    u.setOrgname(rs.getString("org_name"));
                    u.setProfilepic(rs.getString("profile_pic"));
                    u.setLogourl(rs.getString("logo_url"));
                    out.add(u);
                }
                return out;
            }
        }
    }

}
