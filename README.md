# GoSmartLib

## Beschrijving

GoSmartLib (ook wel GoSmartBib) is een modern, uitgebreid bibliotheekbeheersysteem dat specifiek is ontworpen voor scholen. Het platform biedt leerlingen, leerkrachten en beheerders de mogelijkheid om een digitale boekencatalogus te raadplegen, boeken te lenen, leeslijsten te beheren en aankoopsuggesties te doen. 

Het systeem onderscheidt zich door een diepe integratie met Smartschool. Dit stelt scholen in staat om gebruikers, klassen en campussen te synchroniseren via OneRoster, en om berichten te verzenden via SOAP. Daarnaast maakt het platform het mogelijk voor gebruikers om veilig in te loggen via Smartschool OAuth2.

## Functionaliteiten

- **Catalogus en Inventaris**: Zoeken en beheren van boeken met automatische data-aanvulling via de Google Books API.
- **Uitleenbeheer**: Boeken uitlenen, inleveren, uitleentermijnen verlengen en het beheren van uitleenbeleid.
- **Leeslijsten**: Aanmaken van leeslijsten voor specifieke klassen, graden of individuele studenten.
- **Smartschool Integratie**: 
  - Single Sign-On (SSO) voor veilige toegang.
  - Automatische synchronisatie van campussen, klassen en leerlingen/leerkrachten via OneRoster.
  - Verzenden van notificaties/berichten via SOAP.
- **Reviews en Beoordelingen**: Leerlingen kunnen boeken beoordelen. Het systeem bevat tevens een automatische moderatie om ongepaste woorden te blokkeren.
- **Notificaties**: Geautomatiseerde interne meldingen (bijvoorbeeld voor te laat ingeleverde boeken).
- **Aankoopverzoeken**: Functionaliteit voor leerlingen en leerkrachten om nieuwe boeken aan te vragen bij de bibliotheekbeheerder.
- **Bulk Import**: Ondersteuning voor het in bulk importeren van boeken via Excel-bestanden.

## Technologieën

Dit project maakt gebruik van een moderne tech-stack:

**Frontend**
- Next.js (v16.1.6)
- React (v19)
- TypeScript

**Backend**
- Java 17
- Spring Boot (v4.0.2)
- Spring Security & OAuth2
- Spring Data JPA
- Maven
- Apache POI (voor Excel verwerking)

**Infrastructuur & Database**
- Docker & Docker Compose
- Traefik (Reverse proxy met Let's Encrypt automatische SSL-certificaten)
- MySQL (v8.0)

## Vereisten (Prerequisites)

Om het project te draaien via de voorziene containers, heb je het volgende nodig:
- Docker
- Docker Compose

Voor lokale ontwikkeling zonder Docker:
- Node.js (v20 of hoger)
- Java Development Kit (JDK 17)
- Een draaiende MySQL 8.0 server

## Installatie en Setup (Productie / Docker)

1. **Kloon de repository**
```bash
git clone [https://gitlab.apstudent.be/bachelor-it/software-project/25-26/team-08/gosmartlib.git](https://gitlab.apstudent.be/bachelor-it/software-project/25-26/team-08/gosmartlib.git)
cd gosmartlib
```
2. **Secrets instellen**

Het docker-compose.yml bestand maakt gebruik van Docker secrets voor veilige opslag van inloggegevens. Zorg ervoor dat de volgende omgevingsvariabelen of bestanden zijn geconfigureerd voordat je de containers start:

- `GOOGLE_BOOKS_API_KEY`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_ROOT_PASSWORD`
- `SMARTSCHOOL_CLIENT_ID`
- `SMARTSCHOOL_CLIENT_SECRET`
- `APP_CRYPTO_PASSWORD`
- `APP_CRYPTO_SALT`

3. **Start de applicatie**

Maak het externe netwerk aan (zoals gedefinieerd in de compose file) en start de containers:

```bash
docker network create proxy
docker-compose up -d --build
```


Traefik zal automatisch het verkeer routeren naar gosmartbib.tech voor de frontend en gosmartbib.tech/api voor de backend.


**Auteurs en Erkenning**

Dit project is ontwikkeld door Team 08 als onderdeel van het bachelor Software Project (Academiejaar 25-26).