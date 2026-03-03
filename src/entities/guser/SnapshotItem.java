package entities.guser;

public class SnapshotItem {
    private int snapshotId;
    private int artifactId;
    private int fileObjectId; // can be null for TEXT/LINK if you don’t freeze them to file objects

    public int getSnapshotId() { return snapshotId; }
    public void setSnapshotId(int snapshotId) { this.snapshotId = snapshotId; }

    public int getArtifactId() { return artifactId; }
    public void setArtifactId(int artifactId) { this.artifactId = artifactId; }

    public int getFileObjectId() { return fileObjectId; }
    public void setFileObjectId(int fileObjectId) { this.fileObjectId = fileObjectId; }
}
