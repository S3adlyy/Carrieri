package entities;

import java.util.Date;
import java.util.Objects;

public class TraitementReclamation {
    private int id;
    private Date dateTraitement;
    private String reponseAdmin;
    private String statutFinal;
    private Integer reclamationId;
    private Integer adminId;

    public TraitementReclamation() {
    }

    public TraitementReclamation(int id, Date dateTraitement, String reponseAdmin,
                                 String statutFinal, Integer reclamationId, Integer adminId) {
        this.id = id;
        this.dateTraitement = dateTraitement;
        this.reponseAdmin = reponseAdmin;
        this.statutFinal = statutFinal;
        this.reclamationId = reclamationId;
        this.adminId = adminId;
    }

    public TraitementReclamation(Date dateTraitement, String reponseAdmin,
                                 String statutFinal, Integer reclamationId, Integer adminId) {
        this.dateTraitement = dateTraitement;
        this.reponseAdmin = reponseAdmin;
        this.statutFinal = statutFinal;
        this.reclamationId = reclamationId;
        this.adminId = adminId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDateTraitement() {
        return dateTraitement;
    }

    public void setDateTraitement(Date dateTraitement) {
        this.dateTraitement = dateTraitement;
    }

    public String getReponseAdmin() {
        return reponseAdmin;
    }

    public void setReponseAdmin(String reponseAdmin) {
        this.reponseAdmin = reponseAdmin;
    }

    public String getStatutFinal() {
        return statutFinal;
    }

    public void setStatutFinal(String statutFinal) {
        this.statutFinal = statutFinal;
    }

    public Integer getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(Integer reclamationId) {
        this.reclamationId = reclamationId;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    @Override
    public String toString() {
        return "TraitementReclamation{" +
                "id=" + id +
                ", dateTraitement=" + dateTraitement +
                ", reponseAdmin='" + reponseAdmin + '\'' +
                ", statutFinal='" + statutFinal + '\'' +
                ", reclamationId=" + reclamationId +
                ", adminId=" + adminId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TraitementReclamation that = (TraitementReclamation) o;
        return id == that.id &&
                Objects.equals(dateTraitement, that.dateTraitement) &&
                Objects.equals(reponseAdmin, that.reponseAdmin) &&
                Objects.equals(statutFinal, that.statutFinal) &&
                Objects.equals(reclamationId, that.reclamationId) &&
                Objects.equals(adminId, that.adminId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateTraitement, reponseAdmin, statutFinal, reclamationId, adminId);
    }
}