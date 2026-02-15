package services;

import entities.Reclamation;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReclamationService implements IReclamationService {

    private Connection connection;

    public ReclamationService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("ReclamationService: Connection established");
    }

    @Override
    public void ajouter(Reclamation reclamation) throws SQLException {
        String req = "INSERT INTO reclamation (objet, description, categorie, date_creation, statut, priorite, utilisateur_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, reclamation.getObjet());
        preparedStatement.setString(2, reclamation.getDescription());
        preparedStatement.setString(3, reclamation.getCategorie());

        // CORRECTION: Vérifier que la date n'est pas null
        if (reclamation.getDateCreation() != null) {
            preparedStatement.setTimestamp(4, new Timestamp(reclamation.getDateCreation().getTime()));
        } else {
            preparedStatement.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        }

        preparedStatement.setString(5, reclamation.getStatut());
        preparedStatement.setString(6, reclamation.getPriorite());

        if (reclamation.getUtilisateurId() != null) {
            preparedStatement.setInt(7, reclamation.getUtilisateurId());
        } else {
            preparedStatement.setNull(7, Types.INTEGER);
        }

        preparedStatement.executeUpdate();
        System.out.println("Reclamation added successfully.");
    }

    @Override
    public void update(Reclamation reclamation) throws SQLException {
        String sql = "UPDATE reclamation SET objet = ?, description = ?, categorie = ?, date_creation = ?, " +
                "statut = ?, priorite = ?, utilisateur_id = ? WHERE id = ?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, reclamation.getObjet());
        preparedStatement.setString(2, reclamation.getDescription());
        preparedStatement.setString(3, reclamation.getCategorie());
        preparedStatement.setTimestamp(4, new Timestamp(reclamation.getDateCreation().getTime()));
        preparedStatement.setString(5, reclamation.getStatut());
        preparedStatement.setString(6, reclamation.getPriorite());

        if (reclamation.getUtilisateurId() != null) {
            preparedStatement.setInt(7, reclamation.getUtilisateurId());
        } else {
            preparedStatement.setNull(7, Types.INTEGER);
        }

        preparedStatement.setInt(8, reclamation.getId());
        preparedStatement.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reclamation WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<Reclamation> read() throws SQLException {
        String sql = "SELECT * FROM reclamation ORDER BY date_creation DESC";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Reclamation> reclamations = new ArrayList<>();

        while (rs.next()) {
            Reclamation reclamation = new Reclamation();
            reclamation.setId(rs.getInt("id"));
            reclamation.setObjet(rs.getString("objet"));
            reclamation.setDescription(rs.getString("description"));
            reclamation.setCategorie(rs.getString("categorie"));
            reclamation.setDateCreation(new Date(rs.getTimestamp("date_creation").getTime()));
            reclamation.setStatut(rs.getString("statut"));
            reclamation.setPriorite(rs.getString("priorite"));

            int userId = rs.getInt("utilisateur_id");
            if (!rs.wasNull()) {
                reclamation.setUtilisateurId(userId);
            }

            reclamations.add(reclamation);
        }
        return reclamations;
    }

    @Override
    public List<Reclamation> getByStatut(String statut) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE statut = ? ORDER BY date_creation DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, statut);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public List<Reclamation> getByPriorite(String priorite) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE priorite = ? ORDER BY date_creation DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, priorite);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public List<Reclamation> getByUtilisateur(int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE utilisateur_id = ? ORDER BY date_creation DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, utilisateurId);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public List<Reclamation> getByCategorie(String categorie) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE categorie = ? ORDER BY date_creation DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, categorie);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    @Override
    public void updateStatut(int id, String nouveauStatut) throws SQLException {
        String sql = "UPDATE reclamation SET statut = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, nouveauStatut);
        preparedStatement.setInt(2, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<Reclamation> searchByKeyword(String keyword) throws SQLException {
        String sql = "SELECT * FROM reclamation WHERE objet LIKE ? OR description LIKE ? ORDER BY date_creation DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        String searchPattern = "%" + keyword + "%";
        preparedStatement.setString(1, searchPattern);
        preparedStatement.setString(2, searchPattern);
        ResultSet rs = preparedStatement.executeQuery();

        return parseResultSet(rs);
    }

    private List<Reclamation> parseResultSet(ResultSet rs) throws SQLException {
        List<Reclamation> reclamations = new ArrayList<>();

        while (rs.next()) {
            Reclamation reclamation = new Reclamation();
            reclamation.setId(rs.getInt("id"));
            reclamation.setObjet(rs.getString("objet"));
            reclamation.setDescription(rs.getString("description"));
            reclamation.setCategorie(rs.getString("categorie"));
            reclamation.setDateCreation(new Date(rs.getTimestamp("date_creation").getTime()));
            reclamation.setStatut(rs.getString("statut"));
            reclamation.setPriorite(rs.getString("priorite"));

            int userId = rs.getInt("utilisateur_id");
            if (!rs.wasNull()) {
                reclamation.setUtilisateurId(userId);
            }

            reclamations.add(reclamation);
        }
        return reclamations;
    }
}