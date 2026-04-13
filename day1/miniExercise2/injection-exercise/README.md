## Vraag: Hoeveel moet je aanpassen in je framework vs. in de applicatiecode om FirePokemon toe te voegen?

### Framework (1 aanpassing)

In `EntityMetadata.java` moest de veldscan worden uitgebreid. Oorspronkelijk gebruikte de scanner alleen
`getDeclaredFields()` op de concrete klasse zelf, waardoor geërfde velden (zoals `id`, `name`, `type` en
`version` van `Pokemon`) onzichtbaar waren voor subklassen.

De oplossing was om de volledige klassehiërarchie te doorlopen van de subklasse omhoog naar `Object`
en een `Set` bij te houden om dubbele kolomnamen te voorkomen. Dit was een **eenmalige aanpassing** aan
het framework die automatisch van toepassing is op alle huidige en toekomstige subklassen.

(Hier ben ik zelf niet achter gekomen alles wat opeens super stuk en ik kon niet vinden waarom dus hiervoor heb ik AI gebruikt)

### Applicatiecode (1 nieuwe klasse)

Buiten de frameworkaanpassing was alleen een nieuwe klasse `FirePokemon.java` nodig:

- `@Entity` en `@Table(name = "fire-pokemon")` om de eigen tabel te declareren.
- Een constructor die `type` automatisch op `"Fire"` zet.
- Een extra veld `weaknesses` met standaardwaarde `"Water"`.

Er hoefde **niets** aan bestaande applicatieklassen zoals `Pokemon`, `PokemonRepository` of
`GenericRepository` te worden gewijzigd.
