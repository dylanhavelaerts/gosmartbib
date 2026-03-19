# Documentatie Authenticatieflow

Dit document beschrijft de authenticatieflow voor het **GoSmartLib** project. Het detailleert de interactie tussen de frontend, backend en de Smartschool OAuth2-provider. Het systeem is ontworpen om zowel een productieomgeving (met echte Smartschool login) als een lokale ontwikkelomgeving (met mock authenticatie) te ondersteunen.

## **Deel 1: De Controllers & Endpoints**

### **1. `AuthController`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/controllers/AuthController.java`

**Rol:** De API-surface voor de frontend. Het stelt endpoints beschikbaar om het loginproces te starten en user-informatie op te halen.

- **`login()`**
  - **Endpoint:** `GET /auth/login`
  - **Functie:** Initieert de OAuth2-loginflow.
  - **Details:** In plaats van zelf logica af te handelen, returnt het een `RedirectView` naar `/api/oauth2/authorization/smartschool`. Deze interne URL triggert de OAuth2 filter chain van Spring Security om de user te redirecten naar de loginpagina van Smartschool.

- **`me(@AuthenticationPrincipal OAuth2User user)`**
  - **Endpoint:** `GET /auth/me`
  - **Functie:** Returnt de details van de momenteel geauthenticeerde user naar de frontend.
  - **Details:**
    - Accepteert de `OAuth2User` principal die door Spring Security wordt geïnjecteerd (deze bevat de sessiedata van de user).
    - Checkt of de user null is (niet geautoriseerd).
    - Returnt een JSON map met daarin `userID`, `platform` en `isMainAccount`.
    - **Gebruik:** De frontend callt dit na een succesvolle login om de naam of het autorisatieniveau van de user weer te geven.

---

## **Deel 2: Productie Configuratie (Profiel: `prod`)**

### **2. `OAuth2Config`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/security/OAuth2Config.java`

**Rol:** Customized de OAuth2-request specifiek voor Smartschool, dat verouderde requirements heeft.

- **`pkceDisabledResolver(ClientRegistrationRepository repo)`**
  - **Type:** `@Bean`
  - **Functie:** Creëert een custom `OAuth2AuthorizationRequestResolver`.
  - **Waarom is dit nodig?** Spring Security 5+ schakelt standaard **PKCE** (Proof Key for Code Exchange) in voor security. De OAuth-implementatie van Smartschool ondersteunt geen PKCE en zal requests met `code_challenge` of `code_verifier` weigeren.
  - **Logica:**
    - Onderschept de vereenvoudigde OAuth-request.
    - Verwijdert `code_verifier`, `code_challenge` en `code_challenge_method` uit de parameters voordat de user naar Smartschool wordt gestuurd.

- **`authorizationRequestRepository()`**
  - **Type:** `@Bean`
  - **Functie:** Configureert waar de authorization request wordt opgeslagen terwijl de user zich bij Smartschool bevindt.
  - **Logica:** Gebruikt `HttpSessionOAuth2AuthorizationRequestRepository` om de state in de HTTP-sessie op te slaan. Dit zorgt ervoor dat wanneer de user terugkomt, de server nog weet wie ze zijn en wat ze hadden aangevraagd.

### **3. `SecurityConfig`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/security/SecurityConfig.java`

**Rol:** De belangrijkste security-configuratie voor de productieomgeving. Het koppelt de filters, handlers en regels aan elkaar.

- **`filterChain(HttpSecurity http)`**
  - **Type:** `@Bean`
  - **Functie:** Definieert de security-regels voor HTTP-requests.
  - **Belangrijkste Configuraties:**
    - **CORS:** Schakelt Cross-Origin Resource Sharing in zodat de frontend (op een ander domein) requests kan maken.
    - **CSRF:** Uitgeschakeld (gebruikelijk voor stateless REST API's, hoewel het met sessiecookies vaak wordt ingeschakeld; hier is het uitgeschakeld voor eenvoud).
    - **Autorisatie:**
      - Staat publieke toegang toe tot `/public/**`, `/auth/login` en `/login/**`.
      - Vereist authenticatie (`.authenticated()`) voor **alle andere requests**.
    - **OAuth2 Login:**
      - Stelt de custom resolver in (`pkceDisabledResolver`).
      - Stelt de custom success handler in (`oAuth2LoginSuccessHandler`).
      - Definieert een failure handler om te redirecten naar `/login?error=true` als de authenticatie faalt.

- **`corsConfigurationSource()`**
  - **Type:** `@Bean`
  - **Functie:** Definieert de precieze CORS-regels.
  - **Logica:**
    - Staat alleen requests toe vanaf de `frontendUrl` (gedefinieerd in `application.properties`).
    - Staat standaard HTTP-methods toe (GET, POST, PUT, DELETE, OPTIONS).
    - **Cruciaal:** `setAllowCredentials(true)` staat toe dat cookies en authenticatie-headers worden doorgegeven tussen frontend en backend.

### **4. `OAuth2LoginSuccessHandler`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/security/OAuth2LoginSuccessHandler.java`

**Rol:** Handelt het kritieke moment af _nadat_ de user succesvol is ingelogd bij Smartschool en teruggestuurd is naar de backend.

- **`onAuthenticationSuccess(...)`**
  - **Functie:** Wordt automatisch getriggerd door Spring Security na een succesvolle login.
  - **Stappen:**
    1.  **Extract User:** Haalt de `OAuth2User` principal op uit het authenticatietoken.
    2.  **Logging:** Itereert door alle attributen die door Smartschool worden teruggestuurd (zoals user ID, naam, rollen) en logt deze. Dit is essentieel voor het debuggen van welke data Smartschool daadwerkelijk verstuurt.
    3.  **Redirect:** Construeert de target URL (de homepagina van de frontend) en redirect de browser van de user daarheen.
    4.  **Error Handling:** Als er iets crasht tijdens dit proces, redirect het de user naar `/login?error=true`.

---

## **Deel 3: Lokale Ontwikkel Configuratie (Profiel: `local`)**

### **5. `LocalSecurityConfig`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/security/mocksecurity/LocalSecurityConfig.java`

**Rol:** Een vereenvoudigde security-setup voor developers die lokaal werken, waarmee de noodzaak voor een echt Smartschool-account wordt omzeild.

- **`localFilterChain(HttpSecurity http)`**
  - **Type:** `@Bean`
  - **Functie:** Configureert een open security chain.
  - **Logica:**
    - Schakelt CSRF uit.
    - **Permit All:** Staat toegang tot elk endpoint toe zonder echte login (`.anyRequest().permitAll()`).
    - **Inject Mock:** Voegt de `MockAuth` filter toe _vóór_ het standaard username/password filter. Dit creëert de "fake" ingelogde state.

### **6. `MockAuth`**

**Locatie:** `backend/src/main/java/edu/ap/testbackend/security/mocksecurity/MockAuth.java`

**Rol:** Een filter dat requests onderschept en een fake user-sessie injecteert.

- **`doFilterInternal(...)`**
  - **Functie:** Runnt eenmalig voor elke request.
  - **Logica:**
    1.  Checkt of er al een geauthenticeerde user in de `SecurityContext` aanwezig is.
    2.  Indien **Leeg**: Het creëert een mock user.
        - **Attributen:** Stelt fake data in die overeenkomt met de structuur van Smartschool (`userID: "mock-user-local-dev"`, `name: "Local"`, `surname: "Developer"`, etc.).
        - **Token:** Wrapt deze user in een `OAuth2AuthenticationToken`.
        - **Context:** Plaats dit token in de `SecurityContextHolder`.
    3.  **Resultaat:** Wanneer de controller vraagt "Wie is er ingelogd?", antwoordt Spring Security met deze mock user, waardoor de app normaal kan functioneren zonder internet of externe afhankelijkheden.
