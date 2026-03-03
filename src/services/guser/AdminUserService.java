package services.guser;

import entities.guser.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminUserService {
    public Connection connection;

    public AdminUserService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("AdminUserService connected");
    }

    public List<User> search(String q, String role, String status) throws SQLException {
        // role: Any / Candidate / Recruiter / Admin
        // status: Any / Active / Disabled

        String sql =
                "SELECT * FROM user " +
                        "WHERE ( ? IS NULL OR LOWER(CONCAT(first_name,' ',last_name,' ',email)) LIKE ? ) " +
                        "AND ( ? = 'Any' OR roles = ? ) " +
                        "AND ( ? = 'Any' " +
                        "      OR ( ? = 'Active' AND is_active = 1 ) " +
                        "      OR ( ? = 'Disabled' AND is_active = 0 ) ) " +
                        "ORDER BY id DESC";

        String qq = (q == null || q.trim().isEmpty()) ? null : q.trim().toLowerCase();
        String like = (qq == null) ? null : "%" + qq + "%";

        String roleFilter = (role == null || role.isBlank()) ? "Any" : role;
        roleFilter = normalizeRole(roleFilter);

        String statusFilter = (status == null || status.isBlank()) ? "Any" : status;

        List<User> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, qq);
            ps.setString(2, like);

            ps.setString(3, roleFilter);
            ps.setString(4, roleFilter);

            ps.setString(5, statusFilter);
            ps.setString(6, statusFilter);
            ps.setString(7, statusFilter);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
        }
        return out;
    }

    public void setActive(int userId, boolean active) throws SQLException {
        String sql = "UPDATE user SET is_active = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, active ? 1 : 0);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void setRole(int userId, String roleUiValue) throws SQLException {
        String role = normalizeRole(roleUiValue);
        if ("Any".equals(role)) throw new IllegalArgumentException("Role is required.");

        String sql = "UPDATE user SET roles = ?, type = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setString(2, role); // you use type="CANDIDATE"/"RECRUITER" etc in signup; keep consistent
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    public boolean isAdminByEmail(String email) throws SQLException {
        if (email == null || email.trim().isEmpty()) return false;

        String sql = "SELECT roles FROM user WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String roles = rs.getString("roles");
                return roles != null && roles.trim().equalsIgnoreCase("ADMIN");
            }
        }
    }

    private static String normalizeRole(String roleUiValue) {
        if (roleUiValue == null) return "Any";
        String r = roleUiValue.trim().toUpperCase();

        // allow UI labels
        if (r.equals("CANDIDATE")) return "CANDIDATE";
        if (r.equals("RECRUITER")) return "RECRUITER";
        if (r.equals("ADMIN")) return "ADMIN";
        if (r.equals("ANY")) return "Any";

        // also accept "Candidate"/"Recruiter"/"Admin"
        if (r.equals("CANDIDATE")) return "CANDIDATE";
        if (r.equals("RECRUITER")) return "RECRUITER";
        if (r.equals("ADMIN")) return "ADMIN";

        return roleUiValue; // fallback if you store differently
    }

    // Same mapping approach as in UserService
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setFirstname(rs.getString("first_name"));
        u.setLastname(rs.getString("last_name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordhash(rs.getString("password_hash"));
        u.setRoles(rs.getString("roles"));
        u.setIsactive(rs.getInt("is_active"));
        u.setType(rs.getString("type"));

        Timestamp c = rs.getTimestamp("created_at");
        u.setCreatedat(c != null ? c.toLocalDateTime() : null);

        Timestamp l = rs.getTimestamp("last_login_at");
        u.setLastloginat(l != null ? l.toLocalDateTime() : null);

        u.setHeadline(rs.getString("headline"));
        u.setBio(rs.getString("bio"));
        u.setLocation(rs.getString("location"));

        u.setOrgname(rs.getString("org_name"));
        u.setDescription(rs.getString("description"));
        u.setWebsiteurl(rs.getString("website_url"));
        u.setLogourl(rs.getString("logo_url"));
        u.setProfilepic(rs.getString("profile_pic"));

        u.setSchool(rs.getString("school"));
        u.setDegree(rs.getString("degree"));
        u.setFieldofstudy(rs.getString("field_of_study"));
        u.setGraduationyear((Integer) rs.getObject("graduation_year"));
        u.setHardskills(rs.getString("hard_skills"));
        u.setSoftskills(rs.getString("soft_skills"));
        u.setGithuburl(rs.getString("github_url"));
        u.setPortfoliourl(rs.getString("portfolio_url"));
        u.setPhone(rs.getString("phone"));
        return u;
    }
}
