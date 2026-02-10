package services;

import entities.TraitementReclamation;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TraitementReclamationService implements ITraitementReclamationService {

    private Connection connection;

    public TraitementReclamationService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("TraitementReclamationService: Connection established");
    }

    @Override
    public void ajouter(TraitementReclamation traitement) throws SQLException {
        String req = "INSERT INTO traitement_reclamation (date_traitement, reponse_admin, statut_final, reclamation_id, admin_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setTimestamp(1, new Timestamp(traitement.getDateTraitement().getTime()));
        preparedStatement.setString(2, traitement.getReponseAdmin());
        preparedStatement.setString(3, traitement.getStatutFinal());

        if (traitement.getReclamationId() != null) {
            preparedStatement.setInt(4, traitement.getReclamationId());
        } else {
            preparedStatement.setNull(4, Types.INTEGER);
        }

        if (traitement.getAdminId() != null) {
            preparedStatement.setInt(5, traitement.getAdminId());
        } else {
            preparedStatement.setNull(5, Types.INTEGER);
        }

        preparedStatement.executeUpdate();
        System.out.println("TraitementReclamation added successfully.");
    }

    @Override
    public void update(TraitementReclamation traitement) throws SQLException {
        String sql = "UPDATE traitement_reclamation SET date_traitement = ?, reponse_admin = ?, " +
                "statut_final = ?, reclamation_id = ?, admin_id = ? WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setTimestamp(1, new Timestamp(traitement.getDateTraitement().getTime()));
        preparedStatement.setString(2, traitement.getReponseAdmin());
        preparedStatement.setString(3, traitement.getStatutFinal());

        if (traitement.getReclamationId() != null) {
            preparedStatement.setInt(4, traitement.getReclamationId());
        } else {
            preparedStatement.setNull(4, Types.INTEGER);
        }

        if (traitement.getAdminId() != null) {
            preparedStatement.setInt(5, traitement.getAdminId());
        } else {
            preparedStatement.setNull(5, Types.INTEGER);
        }

        preparedStatement.setInt(6, traitement.getId());
        preparedStatement.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM traitement_reclamation WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<TraitementReclamation> read() throws SQLException {
        String sql = "SELECT * FROM traitement_reclamation ORDER BY date_traitement DESC";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        return parseResultSet(rs);
    }

    @Override
    public List<TraitementReclamation> getByReclamationId(int reclamationId) throws SQLException {
        String sql = "SELECT * FROM traitement_reclamation WHERE reclamation_id = ? ORDER BY date_traitement DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, reclamationId);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public List<TraitementReclamation> getByAdminId(int adminId) throws SQLException {
        String sql = "SELECT * FROM traitement_reclamation WHERE admin_id = ? ORDER BY date_traitement DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, adminId);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public TraitementReclamation getLatestByReclamationId(int reclamationId) throws SQLException {
        String sql = "SELECT * FROM traitement_reclamation WHERE reclamation_id = ? " +
                "ORDER BY date_traitement DESC LIMIT 1";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, reclamationId);
        ResultSet rs = preparedStatement.executeQuery();

        List<TraitementReclamation> traitements = parseResultSet(rs);
        return traitements.isEmpty() ? null : traitements.get(0);
    }

    @Override
    public void traiterReclamation(int reclamationId, String reponseAdmin, String statutFinal, int adminId) throws SQLException {
        // Créer un nouveau traitement
        TraitementReclamation traitement = new TraitementReclamation(
                new Date(),
                reponseAdmin,
                statutFinal,
                reclamationId,
                adminId
        );

        ajouter(traitement);

        // Mettre à jour le statut de la réclamation
        IReclamationService reclamationService = new ReclamationService();
        reclamationService.updateStatut(reclamationId, statutFinal);
    }

    private List<TraitementReclamation> parseResultSet(ResultSet rs) throws SQLException {
        List<TraitementReclamation> traitements = new ArrayList<>();

        while (rs.next()) {
            TraitementReclamation traitement = new TraitementReclamation();
            traitement.setId(rs.getInt("id"));
            traitement.setDateTraitement(new Date(rs.getTimestamp("date_traitement").getTime()));
            traitement.setReponseAdmin(rs.getString("reponse_admin"));
            traitement.setStatutFinal(rs.getString("statut_final"));

            int reclamationId = rs.getInt("reclamation_id");
            if (!rs.wasNull()) {
                traitement.setReclamationId(reclamationId);
            }

            int adminId = rs.getInt("admin_id");
            if (!rs.wasNull()) {
                traitement.setAdminId(adminId);
            }

            traitements.add(traitement);
        }
        return traitements;
    }
}