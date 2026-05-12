package nl.topicus.orm;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import nl.topicus.orm.entities.Attack;

/**
 * DAO voor het beheren van {@link Attack} entiteiten via de generieke CRUD-operaties.
 */
public class AttackRepository extends AbstractDataSourceRepository<Attack>
{

    public AttackRepository(@Nonnull DataSource datasource) {
        super(Attack.class, datasource);
    }
}

