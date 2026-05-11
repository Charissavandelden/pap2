package nl.topicus.orm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import nl.topicus.orm.entities.GymLeader;
import nl.topicus.orm.entities.Person;
import nl.topicus.orm.entities.Trainer;
import nl.topicus.orm.mapping.EntityMetadata;

/**
 * DAO voor het beheren van {@link Person} entiteiten.
 * Registreert subtypes zodat findAll() een mix van Trainer en GymLeader teruggeeft.
 */
public class PersonRepository extends AbstractDataSourceRepository<Person> {

    public PersonRepository(@Nonnull DataSource datasource) {
        super(Person.class, datasource);
        metadata.registerSubtype("TRAINER", new EntityMetadata<>(Trainer.class));
        metadata.registerSubtype("GYM_LEADER", new EntityMetadata<>(GymLeader.class));
    }

    public List<Person> findByName(@Nonnull String name) throws SQLException {
        return zoekMetLike("name", name);
    }

    private List<Person> zoekMetLike(@Nonnull String kolom, @Nonnull String waarde) throws SQLException {
        List<Person> results = new ArrayList<>();
        transactionManager.runInTransaction(() -> {
            try {
                String sql = "SELECT * FROM persons WHERE " + kolom + " LIKE ?";
                Connection conn = getConnection();
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, "%" + waarde + "%");
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            results.add(metadata.mapRow(rs));
                        }
                    }
                } finally {
                    sluitAlsNietTransactioneel(conn);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        return results;
    }
}
