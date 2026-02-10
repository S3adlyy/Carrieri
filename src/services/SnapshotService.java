package services;

import entities.Artifact;
import entities.FileObject;
import entities.Snapshot;
import utils.MyDatabase;
import utils.S3KeyUtil;
import utils.S3StorageService;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SnapshotService {
    public Connection connection;

    private final ArtifactService artifactService = new ArtifactService();
    private final FileObjectService fileObjectService;

    private final String bucket;
    private final String region;

    public SnapshotService(String bucket, String region) {
        this.connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
        this.bucket = bucket;
        this.region = region;
        this.fileObjectService = new FileObjectService(bucket, region);
    }

    public List<Snapshot> listByTrack(int trackId) throws SQLException {
        String sql = "SELECT * FROM snapshot WHERE track_id=? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, trackId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Snapshot> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public int createSnapshot(int candidateId, int trackId, int authorId,
                              String title, String message, boolean isFinal) throws Exception {

        boolean oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            int snapshotId = insertSnapshot(trackId, authorId, title, message, isFinal);

            List<Artifact> artifacts = artifactService.listActiveByTrack(trackId);
            String insSql = "INSERT INTO snapshot_item (snapshot_id, artifact_id, file_object_id) VALUES (?, ?, ?)";

            try (PreparedStatement ins = connection.prepareStatement(insSql)) {
                for (Artifact a : artifacts) {
                    Integer fileObjectId = resolveSnapshotFileObjectId(candidateId, trackId, snapshotId, a);

                    ins.setInt(1, snapshotId);
                    ins.setInt(2, a.getId());
                    if (fileObjectId == null) ins.setNull(3, Types.INTEGER);
                    else ins.setInt(3, fileObjectId);
                    ins.addBatch();
                }
                ins.executeBatch();
            }

            connection.commit();
            return snapshotId;

        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(oldAutoCommit);
        }
    }

    private int insertSnapshot(int trackId, int authorId, String title, String message, boolean isFinal) throws SQLException {
        String sql = "INSERT INTO snapshot (title, message, is_final, created_at, track_id, author_id) " +
                "VALUES (?, ?, ?, NOW(), ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, message);
            ps.setBoolean(3, isFinal);
            ps.setInt(4, trackId);
            ps.setInt(5, authorId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for snapshot insert");
                return keys.getInt(1);
            }
        }
    }

    /**
     * Ensures snapshot_item always points to a file_object, including TEXT/LINK.
     */
    private Integer resolveSnapshotFileObjectId(int candidateId, int trackId, int snapshotId, Artifact a) throws Exception {
        String type = a.getArtifactType() == null ? "" : a.getArtifactType().toUpperCase();

        if (type.equals("TEXT") || type.equals("LINK")) {
            String content = a.getTextContent() == null ? "" : a.getTextContent();

            File tmp = File.createTempFile("snapshot-" + snapshotId + "-artifact-" + a.getId(), ".txt");
            try {
                Files.writeString(tmp.toPath(), content, StandardCharsets.UTF_8);

                String key = S3KeyUtil.workspaceSnapshotTextKey(candidateId, trackId, a.getId(), snapshotId, "txt");
                String stableUrl = "s3://" + bucket + "/" + key;

                try (S3StorageService s3 = new S3StorageService(bucket, region)) {
                    s3.uploadFile(tmp, key, "text/plain");
                }

                String ins = "INSERT INTO file_object (storage_key, public_url, mime_type, file_size, uploaded_at, artifact_id) " +
                        "VALUES (?, ?, ?, ?, NOW(), ?)";
                try (PreparedStatement ps = connection.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, key);
                    ps.setString(2, stableUrl);
                    ps.setString(3, "text/plain");
                    ps.setLong(4, content.getBytes(StandardCharsets.UTF_8).length);
                    ps.setInt(5, a.getId());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("No generated key returned for text file_object insert");
                        return keys.getInt(1);
                    }
                }

            } finally {
                try { tmp.delete(); } catch (Exception ignored) {}
            }


        }

        FileObject latest = fileObjectService.findLatestByArtifact(a.getId());
        //naheha khater ken l artifact fihech files tnajamch taamel snapshot ken nkhaliwha
        /*if (latest == null) {
            throw new IllegalStateException("No uploaded file version for artifactId=" + a.getId());
        }
        return latest.getId(); */
        return (latest == null) ? null : latest.getId();
    }

    private Snapshot map(ResultSet rs) throws SQLException {
        Snapshot s = new Snapshot();
        s.setId(rs.getInt("id"));
        s.setTrackId(rs.getInt("track_id"));
        s.setAuthorId(rs.getInt("author_id"));
        s.setTitle(rs.getString("title"));
        s.setMessage(rs.getString("message"));
        s.setFinal(rs.getBoolean("is_final"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) s.setCreatedAt(created.toLocalDateTime());
        return s;
    }
}
