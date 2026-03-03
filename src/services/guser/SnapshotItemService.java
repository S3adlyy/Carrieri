package services.guser;
import entities.guser.Artifact;
import utils.MyDatabase;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;


public class SnapshotItemService {
    public Connection connection;

    public SnapshotItemService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
    }

    public int findFileObjectId(int snapshotId, int artifactId) throws SQLException {
        String sql = "SELECT file_object_id FROM snapshot_item WHERE snapshot_id=? AND artifact_id=? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, snapshotId);
            ps.setInt(2, artifactId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;          // 0 means “missing”
                return rs.getInt("file_object_id"); // assumes it is NOT NULL for code snapshots
            }
        }
    }
    public static class SnapshotArtifactRow {
        public final Artifact artifact;
        public final int fileObjectId;

        public SnapshotArtifactRow(Artifact artifact, int fileObjectId) {
            this.artifact = artifact;
            this.fileObjectId = fileObjectId;
        }
    }

    public List<SnapshotArtifactRow> listArtifactsInSnapshot(int snapshotId) throws SQLException {
        String sql =
                "SELECT a.*, si.file_object_id " +
                        "FROM snapshot_item si " +
                        "JOIN artifact a ON a.id = si.artifact_id " +
                        "WHERE si.snapshot_id = ? " +
                        "ORDER BY a.created_at ASC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, snapshotId);
            try (ResultSet rs = ps.executeQuery()) {
                List<SnapshotArtifactRow> out = new ArrayList<>();
                while (rs.next()) {
                    Artifact a = new Artifact();
                    a.setId(rs.getInt("id"));
                    a.setTrackId(rs.getInt("track_id"));
                    a.setArtifactName(rs.getString("artifact_name"));
                    a.setArtifactDescription(rs.getString("artifact_description"));
                    a.setArtifactType(rs.getString("artifact_type"));
                    a.setLanguage(rs.getString("language"));
                    a.setTextContent(rs.getString("test_content"));

                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) a.setCreatedAt(created.toLocalDateTime());
                    Timestamp del = rs.getTimestamp("deleted_at");
                    if (del != null) a.setDeletedAt(del.toLocalDateTime());

                    int fileObjectId = rs.getInt("file_object_id");
                    out.add(new SnapshotArtifactRow(a, fileObjectId));
                }
                return out;
            }
        }
    }




}

