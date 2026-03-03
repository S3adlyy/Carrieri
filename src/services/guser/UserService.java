package services.guser;

import entities.guser.User;
import security.PasswordHasher;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IUser<User> {
    public Connection connection;

    public UserService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
    }

    @Override
    public void signupCandidate(User u, String passwordPlain) throws SQLException {
        validateBase(u, passwordPlain);

        require(u.getHeadline(), "Headline");
        require(u.getBio(), "Bio");
        require(u.getLocation(), "Location");

        // Recommended “basic profile”
        require(u.getSchool(), "School");
        require(u.getDegree(), "Degree");
        require(u.getFieldofstudy(), "Field of study");
        if (u.getGraduationyear() == null) throw new IllegalArgumentException("Graduation year is required.");

        u.setRoles("CANDIDATE");
        u.setType("CANDIDATE");
        u.setIsactive(1);
        u.setCreatedat(LocalDateTime.now());
        u.setLastloginat(null);

        String sql = "INSERT INTO user (" +
                "first_name, last_name, email, password_hash, roles, is_active, last_login_at, created_at, type, " +
                "headline, bio, location, profile_pic, " +
                "school, degree, field_of_study, graduation_year, " +
                "hard_skills, soft_skills, github_url, portfolio_url, phone, " +
                "org_name, description, website_url, logo_url" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, ?, ?, " +
                "?, ?, ?, ?, " +
                "?, ?, ?, ?, ?, " +
                "NULL, NULL, NULL, NULL" +
                ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.getFirstname().trim());
            ps.setString(2, u.getLastname().trim());
            ps.setString(3, normalizeEmail(u.getEmail()));
            ps.setString(4, PasswordHasher.hash(passwordPlain));
            ps.setString(5, u.getRoles());
            ps.setInt(6, u.getIsactive());
            ps.setTimestamp(7, null);
            ps.setTimestamp(8, Timestamp.valueOf(u.getCreatedat()));
            ps.setString(9, u.getType());

            ps.setString(10, u.getHeadline().trim());
            ps.setString(11, u.getBio().trim());
            ps.setString(12, u.getLocation().trim());
            ps.setString(13, emptyToNull(u.getProfilepic())); // optional

            ps.setString(14, u.getSchool().trim());
            ps.setString(15, u.getDegree().trim());
            ps.setString(16, u.getFieldofstudy().trim());
            ps.setInt(17, u.getGraduationyear());

            ps.setString(18, emptyToNull(u.getHardskills()));  // optional
            ps.setString(19, emptyToNull(u.getSoftskills()));  // optional
            ps.setString(20, emptyToNull(u.getGithuburl()));   // optional
            ps.setString(21, emptyToNull(u.getPortfoliourl())); // optional
            ps.setString(22, emptyToNull(u.getPhone()));       // optional

            ps.executeUpdate();
            System.out.println("Candidate created successfully");
        }
    }

    @Override
    public void signupRecruiter(User u, String passwordPlain) throws SQLException {
        validateBase(u, passwordPlain);

        require(u.getOrgname(), "Organization name");
        require(u.getWebsiteurl(), "Website URL");
        require(u.getDescription(), "Description");
        require(u.getLocation(), "Location");
        // phone optional

        u.setRoles("RECRUITER");
        u.setType("RECRUITER");
        u.setIsactive(1);
        u.setCreatedat(LocalDateTime.now());
        u.setLastloginat(null);

        String sql = "INSERT INTO user (" +
                "first_name, last_name, email, password_hash, roles, is_active, last_login_at, created_at, type, " +
                "location, phone, " +
                "org_name, description, website_url, logo_url, " +
                "headline, bio, profile_pic, " +
                "school, degree, field_of_study, graduation_year, hard_skills, soft_skills, github_url, portfolio_url" +
                ") VALUES (" +
                "?, ?, ?, ?, ?, ?, ?, ?, ?, " +
                "?, ?, " +
                "?, ?, ?, ?, " +
                "NULL, NULL, NULL, " +
                "NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL" +
                ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.getFirstname().trim());
            ps.setString(2, u.getLastname().trim());
            ps.setString(3, normalizeEmail(u.getEmail()));
            ps.setString(4, PasswordHasher.hash(passwordPlain));
            ps.setString(5, u.getRoles());
            ps.setInt(6, u.getIsactive());
            ps.setTimestamp(7, null);
            ps.setTimestamp(8, Timestamp.valueOf(u.getCreatedat()));
            ps.setString(9, u.getType());

            ps.setString(10, u.getLocation().trim());
            ps.setString(11, emptyToNull(u.getPhone())); // optional

            ps.setString(12, u.getOrgname().trim());
            ps.setString(13, u.getDescription().trim());
            ps.setString(14, u.getWebsiteurl().trim());
            ps.setString(15, emptyToNull(u.getLogourl())); // optional

            ps.executeUpdate();
            System.out.println("Recruiter created successfully");
        }
    }

    @Override
    public User login(String email, String passwordPlain) throws SQLException {
        if (isBlank(email) || isBlank(passwordPlain)) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        String sql = "SELECT * FROM user WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Invalid credentials.");

                User u = map(rs);

                if (u.getIsactive() == 0) throw new IllegalArgumentException("Account is disabled.");
                if (!PasswordHasher.verify(passwordPlain, u.getPasswordhash())) {
                    throw new IllegalArgumentException("Invalid credentials.");
                }

                updateLastLogin(u.getId());
                u.setLastloginat(LocalDateTime.now());
                return u;
            }
        }
    }

    private void updateLastLogin(int id) throws SQLException {
        String sql = "UPDATE user SET last_login_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void ajouter(User u) throws SQLException {
        throw new UnsupportedOperationException("Use signupCandidate/signupRecruiter for now.");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "UPDATE user SET is_active = 0 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("User deactivated successfully");
        }
    }

    @Override
    public void update(User u) throws SQLException {
        String sql = "UPDATE user SET first_name = ?, last_name = ?, headline = ?, bio = ?, location = ?, " +
                "school = ?, degree = ?, field_of_study = ?, graduation_year = ?, hard_skills = ?, soft_skills = ?, " +
                "github_url = ?, portfolio_url = ?, phone = ?, " +
                "org_name = ?, description = ?, website_url = ? " +
                "WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.getFirstname());
            ps.setString(2, u.getLastname());
            ps.setString(3, u.getHeadline());
            ps.setString(4, u.getBio());
            ps.setString(5, u.getLocation());

            ps.setString(6, u.getSchool());
            ps.setString(7, u.getDegree());
            ps.setString(8, u.getFieldofstudy());
            if (u.getGraduationyear() == null) ps.setNull(9, Types.SMALLINT); else ps.setInt(9, u.getGraduationyear());
            ps.setString(10, u.getHardskills());
            ps.setString(11, u.getSoftskills());

            ps.setString(12, u.getGithuburl());
            ps.setString(13, u.getPortfoliourl());
            ps.setString(14, u.getPhone());

            ps.setString(15, u.getOrgname());
            ps.setString(16, u.getDescription());
            ps.setString(17, u.getWebsiteurl());

            ps.setInt(18, u.getId());

            ps.executeUpdate();
            System.out.println("User updated successfully");
        }
    }

    @Override
    public List<User> read() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    @Override
    public User getById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    @Override
    public User getByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

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

    private static void validateBase(User u, String passwordPlain) {
        if (u == null) throw new IllegalArgumentException("User is required.");
        require(u.getFirstname(), "First name");
        require(u.getLastname(), "Last name");
        require(u.getEmail(), "Email");
        if (isBlank(passwordPlain) || passwordPlain.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void require(String s, String field) {
        if (isBlank(s)) throw new IllegalArgumentException(field + " is required.");
    }
}
