package nl.topicus.orm.mapping;

import java.lang.reflect.Field;
import javax.annotation.Nonnull;

/**
 * Houdt de mapping-informatie bij voor één relatie-veld: het veld zelf,
 * de foreign key kolomnaam en het type relatie (ManyToOne of OneToMany).
 */
public class RelatieMetadata {

    public enum RelatieType {
        MANY_TO_ONE,
        ONE_TO_MANY
    }

    private final Field field;
    private final String joinKolom;
    private final RelatieType relatieType;

    public RelatieMetadata(@Nonnull Field field, @Nonnull String joinKolom, @Nonnull RelatieType relatieType) {
        this.field = field;
        this.joinKolom = joinKolom;
        this.relatieType = relatieType;
        field.setAccessible(true);
    }

    public Field getField() {
        return field;
    }

    public String getJoinKolom() {
        return joinKolom;
    }

    public RelatieType getRelatieType() {
        return relatieType;
    }

    public Class<?> getGerelateerdeKlasse() {
        return field.getType();
    }
}
