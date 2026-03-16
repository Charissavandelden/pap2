package nl.topicus.injection;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

/**
 * DAO voor het beheren van {@link Attack} entiteiten via de generieke CRUD-operaties.
 */
public class AttackDao extends AbstractDataSourceDao<Attack> {

    public AttackDao(@Nonnull DataSource datasource) {
        super(Attack.class, datasource);
    }
}

