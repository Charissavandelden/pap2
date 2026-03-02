# Lab: PreparedStatements in de praktijk

## Scenario

Je bent gevraagd om een bestaande interne student-zoekpagina te reviewen. Een collega heeft de backend geschreven met JDBC, en de zoekfunctie "werkt prima". Maar toen de security-afdeling een snelle test deed, konden ze met een simpel trucje de volledige database uitlezen. Jouw taak: reproduceer de aanval, begrijp waarom het mis gaat, en los het op met PreparedStatements.

---

## Learning Goals

- SQL injection herkennen en reproduceren in een kwetsbare JDBC-applicatie
- Een `Statement` met string concatenation vervangen door een `PreparedStatement` met parameters
- Beoordelen waarom PreparedStatements bescherming bieden tegen SQL injection door het verschil in query-verwerking te analyseren
- Zelfstandig een nieuwe database-query ontwerpen en implementeren met PreparedStatements

---

## Prerequisites

- Java 21 geinstalleerd
- Maven geinstalleerd
- Een IDE (IntelliJ IDEA aanbevolen)
- Mini Exercise 1 afgerond (basiskennis JDBC, Statement, ResultSet)
- Basiskennis SQL (SELECT, WHERE)

---

# Lab Parts

Dit lab bevat **4 delen**.

---

## Part 1: De applicatie starten en verkennen

### What you will do

Start de meegeleverde applicatie en gebruik de zoekpagina om een student op te zoeken op e-mailadres. Bekijk in de console welke SQL-query er wordt uitgevoerd.

### Success criteria

- De applicatie start op `http://localhost:8080`
- Bij het zoeken op `alice@university.nl` verschijnt Alice's gegevens
- In de console is de uitgevoerde SQL-query zichtbaar

### Hints

<details>
<summary>Hint 1</summary>

Het project staat in de `injection-exercise` map. Gebruik de play button naast de main method of Maven om het te compileren en te runnen.

</details>

<details>
<summary>Hint 2</summary>

Open een terminal in de `injection-exercise` folder en run:

```bash
mvn compile exec:java -Dexec.mainClass="nl.topicus.injection.InjectionApp"
```

Je hebt hiervoor de `exec-maven-plugin` nodig, of je kunt de `InjectionApp` class direct runnen vanuit je IDE.

</details>

<details>
<summary>Hint 3</summary>

Open `http://localhost:8080` in je browser. Typ `alice@university.nl` in het zoekveld en klik op Zoeken. Bekijk de console-output in je terminal of IDE.

</details>

---

## Part 2: SQL injection uitvoeren

### What you will do

Voer een SQL injection-aanval uit via het zoekveld. Het doel: haal alle studenten op uit de database, niet alleen degene die je zoekt.

### Success criteria

- Alle 5 studenten verschijnen in de zoekresultaten, terwijl je maar een "e-mailadres" hebt ingevuld
- Je begrijpt waarom de aanval werkt door de query in de console te bekijken

### Hints

<details>
<summary>Hint 1</summary>

SQL injection werkt doordat gebruikersinput direct in een SQL-string wordt geplakt. Als de query er zo uitziet: `SELECT * FROM students WHERE email = '...'`, wat gebeurt er dan als je input het aanhalingsteken afsluit en extra SQL toevoegt?

</details>

<details>
<summary>Hint 2</summary>

Probeer deze waarde in het zoekveld: `' OR '1'='1`

</details>

<details>
<summary>Hint 3</summary>

De volledige query wordt dan:

```sql
SELECT * FROM students WHERE email = '' OR '1'='1'
```

De conditie `'1'='1'` is altijd waar, dus alle rijen worden geretourneerd. Bekijk de console om dit te bevestigen.

</details>

---

## Part 3: Statement vervangen door PreparedStatement

### What you will do

Open `StudentDao.java` en vervang de kwetsbare `findByEmail`-methode zodat deze een `PreparedStatement` gebruikt in plaats van string concatenation. Test daarna opnieuw of SQL injection nog werkt.

### Success criteria

- Zoeken op `alice@university.nl` werkt nog steeds en toont Alice
- De SQL injection (`' OR '1'='1`) geeft nu **geen resultaten** meer terug
- De code gebruikt `PreparedStatement` met een `?`-parameter in plaats van string concatenation

### Hints

<details>
<summary>Hint 1</summary>

Een `PreparedStatement` scheidt de query-structuur van de data. In plaats van de waarde in de string te plakken, gebruik je een `?` als placeholder.

</details>

<details>
<summary>Hint 2</summary>

De SQL wordt: `SELECT * FROM students WHERE email = ?`. Gebruik `connection.prepareStatement(sql)` om de statement aan te maken.

</details>

<details>
<summary>Hint 3</summary>

Na het aanmaken van de `PreparedStatement` zet je de waarde met `pstmt.setString(1, email)`. De `1` verwijst naar de eerste `?` in de query.

</details>

<details>
<summary>Hint 4</summary>

```java
String sql = "SELECT * FROM students WHERE email = ?";
try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    pstmt.setString(1, email);
    try (ResultSet rs = pstmt.executeQuery()) {
        // verwerk resultaten...
    }
}
```

</details>

---

## Part 4: findByName methode toevoegen

### What you will do

Voeg een nieuwe methode `findByName(String name)` toe aan `StudentDao` die een student zoekt op naam. Gebruik direct een `PreparedStatement` — nu je weet waarom dat belangrijk is. Maak ook een nieuw API-endpoint aan in `InjectionApp` om deze methode via de browser te kunnen aanroepen.

### Success criteria

- De methode `findByName` bestaat in `StudentDao` en gebruikt een `PreparedStatement`
- Via het endpoint `/api/search-by-name?name=Alice` kun je zoeken op naam
- De zoekopdracht is niet kwetsbaar voor SQL injection
- Optioneel: Voeg een tweede zoekveld toe aan de frontend (voor naam)

### Hints

<details>
<summary>Hint 1</summary>

De methode lijkt sterk op `findByEmail`, maar dan met `WHERE name = ?` in plaats van `WHERE email = ?`. Je kunt ook `LIKE` gebruiken als je wilt dat een gedeeltelijke naam ook werkt.

</details>

<details>
<summary>Hint 2</summary>

Als je `LIKE` wilt gebruiken voor gedeeltelijke matches, dan wordt de SQL: `SELECT * FROM students WHERE name LIKE ?` en stel je de parameter in als `pstmt.setString(1, "%" + name + "%")`.

</details>

<details>
<summary>Hint 3</summary>

Voor het API-endpoint in `InjectionApp`, maak een nieuw `server.createContext("/api/search-by-name", ...)` blok aan. Parse de `name` parameter uit de query string, net zoals bij het bestaande `/api/search` endpoint.

</details>

<details>
<summary>Hint 4</summary>

```java
public List<String> findByName(String name) throws SQLException {
    List<String> results = new ArrayList<>();
    String sql = "SELECT * FROM students WHERE name LIKE ?";
    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
        pstmt.setString(1, "%" + name + "%");
        try (ResultSet rs = pstmt.executeQuery()) {
            // verwerk resultaten zoals in findByEmail
        }
    }
    return results;
}
```

</details>

---

# Bonus Challenge (Optional)

Voeg een methode `findAll()` toe aan `StudentDao` en een bijbehorend `/api/students` endpoint. Gebruik dit endpoint om een overzichtspagina te bouwen die altijd alle studenten toont. Denk na over of je hier een `PreparedStatement` of een gewone `Statement` kunt gebruiken — en waarom. Wanneer is het onderscheid relevant vanuit security pov?

---

# Reflection Questions

### Implementation & Trade-offs

1. De kwetsbare versie gebruikte string concatenation om de email in de query te plaatsen. Waarom beschermt een `PreparedStatement` hier wel tegen, terwijl het uiteindelijk dezelfde SQL uitvoert?
2. Je kunt met `LIKE` zoeken op gedeeltelijke namen. Welk risico brengt dit met zich mee als gebruikers willekeurige patronen kunnen invoeren (denk aan performance)?

### Production Readiness

3. De foutmelding van de database wordt nu direct teruggestuurd naar de frontend als JSON. Waarom is dat in een productieomgeving een slecht idee vanuit security-perspectief?
4. Welke logging zou je aan de `StudentDao` toevoegen als dit een productiesysteem was dat door honderden gebruikers tegelijk wordt gebruikt?

### Debugging & Problem Solving

5. Stel dat je na het omschrijven naar `PreparedStatement` ineens geen resultaten meer terugkrijgt voor een gewone zoekopdracht. Wat zou je als eerste controleren?
6. De console toont de uitgevoerde query bij de `Statement`-versie. Waarom is het lastig om bij een `PreparedStatement` de daadwerkelijke query met ingevulde parameters te zien?

### Adaptation / Transfer

7. SQL injection is een voorbeeld van een breder probleem: injection-aanvallen. Kun je een vergelijkbaar probleem bedenken in een ander deel van een webapplicatie (niet de database)?
8. Als je deze applicatie zou migreren naar een ORM zoals Hibernate, zou je dan nog steeds op dit niveau moeten nadenken over SQL injection? Waarom wel of niet?

---
