package nl.topicus.injection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import nl.topicus.injection.entities.Person;

/**
 * DAO voor het beheren van {@link Person} entiteiten.
 * Erft de vijf generieke CRUD-operaties en voegt Person-specifieke zoekmethoden toe.
 */
public class PersonRepository extends AbstractDataSourceRepository<Person> {

    public PersonRepository(@Nonnull DataSource datasource) {
        super(Person.class, datasource);
    }

    public List<Person> findByName(@Nonnull String name) throws SQLException {
        return zoekMetLike("name", name);
    }

    private List<Person> zoekMetLike(@Nonnull String kolom, @Nonnull String waarde) throws SQLException {
        TransactionManager transactionManager = new TransactionManager();
                List<Person> results = new ArrayList<>();


        transactionManager.runInTransaction(() -> {
            try {
                String sql = "SELECT * FROM persons WHERE " + kolom + " LIKE ?";

                Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, "%" + waarde + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(metadata.mapRow(rs));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e); // must wrap checked exception
            }
        });

        return results;
    }
}
