package services.guser;
import java.sql.SQLException;
import java.util.List;
import entities.guser.User;

public interface IUser <T> {

    //CRUD admin
    void ajouter(T t) throws SQLException;
    void supprimer(int id) throws SQLException;
    void update(T t) throws SQLException;
    List<T> read() throws SQLException;
    //LOGIN SIGNUP lel candidate w recruiter
    User getById(int id) throws SQLException;
    User getByEmail(String email) throws SQLException;

    void signupCandidate(User u, String passwordPlain) throws SQLException;
    void signupRecruiter(User u, String passwordPlain) throws SQLException;

    User login(String email, String passwordPlain) throws SQLException;

}
