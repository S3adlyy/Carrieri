package services.guser;

import entities.guser.Track;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TrackService {
    public Connection connection;

    public TrackService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
    }

    public Track create(int workspaceId,
                        String title,
                        String description,
                        String category,
                        java.time.LocalDate startDate,
                        java.time.LocalDate endDate,
                        String visibility) throws SQLException {

        String sql = "INSERT INTO track (title, description, category, start_date, end_date, status, created_at, visibility, workspace_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, NOW(), ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description == null ? "" : description);
            ps.setString(3, category);

            if (startDate != null) ps.setDate(4, java.sql.Date.valueOf(startDate));
            else ps.setNull(4, Types.DATE);

            if (endDate != null) ps.setDate(5, java.sql.Date.valueOf(endDate));
            else ps.setNull(5, Types.DATE);

            ps.setString(6, "ACTIVE"); // keep consistent with your schema usage [file:529]
            ps.setString(7, visibility);
            ps.setInt(8, workspaceId);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for track insert");
                int id = keys.getInt(1);

                Track t = new Track();
                t.setId(id);
                t.setWorkspaceId(workspaceId);
                t.setTitle(title);
                t.setDescription(description == null ? "" : description);
                t.setCategory(category);
                t.setStatus("ACTIVE");
                t.setVisibility(visibility);
                t.setStartDate(startDate);
                t.setEndDate(endDate);
                t.setCreatedAt(java.time.LocalDateTime.now());
                return t;
            }
        }
    }


    public List<Track> listByWorkspace(int workspaceId, boolean onlyPublic) throws SQLException {
        String sql = onlyPublic
                ? "SELECT * FROM track WHERE workspace_id=? AND visibility='PUBLIC' ORDER BY created_at DESC"
                : "SELECT * FROM track WHERE workspace_id=? ORDER BY created_at DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, workspaceId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Track> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public void updateVisibility(int trackId, String visibility) throws SQLException {
        String sql = "UPDATE track SET visibility=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, visibility);
            ps.setInt(2, trackId);
            ps.executeUpdate();
        }
    }

    private Track map(ResultSet rs) throws SQLException {
        Track t = new Track();
        t.setId(rs.getInt("id"));
        t.setWorkspaceId(rs.getInt("workspace_id"));

        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setCategory(rs.getString("category"));
        t.setStatus(rs.getString("status"));
        t.setVisibility(rs.getString("visibility"));

        Date sd = rs.getDate("start_date");
        if (sd != null) t.setStartDate(sd.toLocalDate());

        Date ed = rs.getDate("end_date");
        if (ed != null) t.setEndDate(ed.toLocalDate());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());

        return t;
    }
}
