────────────────────────────────────────────────────────

TILLGÄNGLIGHETSRAPPORT — OpenID Federation Registry Admin
Datum: 2026-06-24 (uppdaterad efter åtgärdsrunda)
Ursprunglig granskning: 2026-06-23
Standard: WCAG 2.1 nivå AA
────────────────────────────────────────────────────────

SAMMANFATTNING

Totalt antal ursprungliga fynd: 26
- Kritiska (AA-brott):       8  → 0 kvarstående  ✅
- Viktiga (försämrad UX):    8  → 1 kvarstående
- Rekommendationer:          4  → 1 kvarstående
- Kräver mänsklig bedömning: 6  → 6 kvarstående (oförändrade, kräver manuell testning)

Åtgärdade i denna sprint: 18 av 20 auto-fixbara fynd
Ej åtgärdat (kräver backend eller manuell insats): 2

Granskat: 24 Vue-komponenter/vyer, index.html, main.css, Spring ErrorHandler

  ---
ÅTGÄRDADE FYND

  ---
1. ✅ Ikonknappar utan tillgängligt namn

- Fil: ListField.vue; TrustmarkSourcesField.vue; FederationEntityEditView.vue; RegistrationFlowFormView.vue
- WCAG-kriterium: 4.1.2 Name, Role, Value (A)
- Åtgärd: Samtliga sju ikonknappar har fått :aria-label och v-icon har fått aria-hidden="true".

  | Fil                              | Knapp               | Ikon           | Status |
  |----------------------------------|---------------------|----------------|--------|
  | ListField.vue:41                 | Ta bort listelement | mdi-delete     | ✅     |
  | TrustmarkSourcesField.vue:45     | Ta bort issuer      | mdi-delete     | ✅     |
  | TrustmarkSourcesField.vue:71     | Ta bort trustmark   | mdi-close      | ✅     |
  | FederationEntityEditView.vue:262 | Avregistrera flöde  | mdi-close      | ✅     |
  | RegistrationFlowFormView.vue:147 | Flytta steg uppåt   | mdi-arrow-up   | ✅     |
  | RegistrationFlowFormView.vue:154 | Flytta steg neråt   | mdi-arrow-down | ✅     |
  | RegistrationFlowFormView.vue:161 | Ta bort steg        | mdi-delete     | ✅     |

  ---
2. ✅ Organisationsväljare utan label

- Fil: AppLayout.vue
- WCAG-kriterium: 1.3.1 / 4.1.2
- Åtgärd: label="Organization" och aria-label="Select organization" tillagda på v-select.

  ---
3. ✅ Klickbara tabellrader utan tangentbordsstöd

- Fil: RegistrationsListView.vue
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Åtgärd: role="button", tabindex="0", :aria-label, @keydown.enter.prevent och
  @keydown.space.prevent tillagda på samtliga klickbara tr-element.

  ---
4. ✅ Klickbara listposter utan tangentbordsstöd (JWKS-väljare)

- Fil: SubordinateFormView.vue; FederationEntityEditView.vue
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Åtgärd: tabindex="0" och @keydown.enter.prevent="applyJwksResult(item)" /
  applyResolverJwks(item) tillagda på v-list-item i båda JWKS-picker-dialogerna.

  ---
5. ✅ Dynamiska felmeddelanden saknar aria-live/role="alert"

- Fil: AppLayout.vue; LoginView.vue; EntityConfigurationViewer.vue; RegistrationDetailView.vue
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Åtgärd: role="alert" och aria-live="assertive" tillagda på alla villkorliga v-alert.

  ---
6. ✅ Dialogrutor saknar aria-labelledby

- Fil: HomeView.vue; SubordinatesListView.vue; TrustmarksListView.vue;
  TrustmarkSubjectsListView.vue; RegistrationFlowsListView.vue;
  FederationEntityEditView.vue (2 st); SubordinateFormView.vue;
  RegistrationDetailView.vue (2 st); EntityConfigurationViewer.vue
- WCAG-kriterium: 4.1.2 Name, Role, Value (A)
- Åtgärd: aria-labelledby tillagt på alla v-dialog, id tillagt på respektive v-card-title.

  ---
7. ✅ Sidtitel ändras inte vid navigation

- Fil: frontend/src/router/index.js
- WCAG-kriterium: 2.4.2 Page Titled (A)
- Åtgärd: meta: { title: '...' } tillagt på alla 21 route-definitioner.
  router.afterEach sätter document.title = `${to.meta.title} — OpenID Federation Admin`
  vid varje navigering.

  ---
8. ✅ Samtliga vyer saknar <h1>

- Fil: AppLayout.vue; frontend/src/router/index.js
- WCAG-kriterium: 1.3.1 / 2.4.6 (AA)
- Åtgärd: En gemensam <h1 class="sr-only"> placerades i AppLayout.vue inuti v-main.
  Den läser route.meta.title via en computed-property och täcker automatiskt alla vyer.
  CSS-klassen .sr-only lades till i main.css.

  ---
9. ✅ Informationsikoner i tooltip-aktivatorer är ej fokuserbara

- Fil: SubordinatesListView.vue
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Åtgärd: v-icon ersatt med <span tabindex="0" role="img" aria-label="..."> som
  aktivator för v-tooltip på "EC Location configured" och "Remote entity"-ikonerna.

  ---
10. ✅ API-länk öppnar ny flik utan förvarning

- Fil: AppLayout.vue
- WCAG-kriterium: 2.4.4 Link Purpose (A) / 3.2.2 On Input (A)
- Åtgärd: aria-label="API documentation (opens in new tab)" tillagt. mdi-open-in-new-ikon
  med aria-hidden="true" tillagd för visuell indikation.

  ---
11. ✅ Laddningsstatus annonseras inte dynamiskt

- Fil: Alla vyer med v-progress-circular (11 förekomster)
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Åtgärd: Laddningsbehållaren omsluten med <div role="status" aria-live="polite">.
  aria-hidden="true" tillagt på v-progress-circular i: HomeView, SubordinatesListView,
  TrustmarksListView, TrustmarkSubjectsListView, RegistrationFlowsListView,
  FederationEntityEditView, SubordinateFormView, RegistrationFlowFormView,
  RegistrationsListView, RegistrationDetailView, EntityConfigurationViewer.

  ---
12. ✅ Dekorativa ikoner saknar aria-hidden

- Fil: SubordinatesListView.vue; TrustmarksListView.vue; TrustmarkSubjectsListView.vue
- WCAG-kriterium: 1.1.1 Non-text Content (A)
- Åtgärd: aria-hidden="true" tillagt på mdi-shield-check, mdi-transit-connection-variant,
  mdi-certificate-outline och mdi-tag-outline i kontextkortens informationsrad.

  ---
13. ✅ SVG-logotyp saknar role="img"

- Fil: AppLayout.vue
- WCAG-kriterium: 1.1.1 Non-text Content (A)
- Åtgärd: role="img" tillagt på <svg>-elementet. aria-label="Sweden Connect" var
  redan satt sedan tidigare.

  ---
14. ✅ Valideringsfel för pipeline-steg annonseras inte

- Fil: RegistrationFlowFormView.vue
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Åtgärd: Felmeddelandet "No steps selected" visas nu via ett separat <span> med
  role="alert" aria-live="assertive" som renderas med v-if enbart när
  stepsValidationAttempted är true. Det neutrala tillståndet visas via ett
  eget <span> utan roll.

  ---
15. ✅ Focus-visible-styling saknas för navigeringslänkar

- Fil: AppLayout.vue (scoped CSS)
- WCAG-kriterium: 2.4.7 Focus Visible (AA)
- Åtgärd:
  .nav-link:focus-visible {
    outline: 2px solid #4a6741;
    outline-offset: 2px;
    border-radius: 2px;
  }

  ---
17. ✅ Skip-navigation-länk saknas

- Fil: AppLayout.vue; main.css
- Åtgärd: <a href="#main-content" class="skip-link">Skip to main content</a> placerad
  som första fokuserbara element. id="main-content" tillagt på <v-main>.
  .skip-link och .sr-only CSS-klasser tillagda i main.css.

  ---
19. ✅ Tabeller saknar caption/aria-label

- Fil: HomeView.vue; SubordinatesListView.vue; TrustmarksListView.vue;
  TrustmarkSubjectsListView.vue; RegistrationFlowsListView.vue; RegistrationsListView.vue
- Åtgärd: <caption class="sr-only">...</caption> tillagd i alla v-table:
    - "List of entities"
    - "List of subordinates"
    - "List of trustmarks"
    - "List of trustmark subjects"
    - "List of registration flows"
    - "List of registrations"

  ---
20. ✅ Navigationslandmärke saknar aria-label

- Fil: AppLayout.vue
- Åtgärd: aria-label="Main navigation" tillagt på <nav>.

  ---
KVARSTÅENDE FYND

  ---
16. ⚠️ Beskrivning av stegkonfigurationsfält (cfg.key) — KRÄVER BACKEND

- Fil: RegistrationFlowFormView.vue
- WCAG-kriterium: 2.4.6 Headings and Labels (AA)
- Status: Ej åtgärdat. Backend returnerar inte något displayName-fält per
  config-egenskap. Frontend visar cfg.key som label (t.ex. "allowSelfSigned",
  "timeoutSeconds").
- Åtgärd krävs: Backend-API:t behöver utökas med ett displayName-fält i
  step-config-svaret. Frontend är redo att använda cfg.displayName ?? cfg.key
  så fort backend stödjer det.

  ---
18. ⚠️ lang-attribut för svenska texter — VUETIFY-BEGRÄNSNING

- Fil: RegistrationFlowFormView.vue (hint: "Beskrivning av flödet på svenska")
- WCAG-kriterium: 3.1.2 Language of Parts (AA)
- Status: Ej åtgärdat. Vuetify renderar hint-text som en intern <div> utan
  möjlighet att sätta lang-attribut via prop.
- Möjlig workaround: Flytta den svenska hint-texten till ett separat
  <span lang="sv"> utanför v-textarea. Kräver designbeslut om layout.

  ---
KRÄVER MÄNSKLIG BEDÖMNING — OFÖRÄNDRAT

  ---
Alt-texter

- AppLayout.vue: SVG-logotypen har aria-label="Sweden Connect".
  Verifiera att detta är korrekt i sammanhanget — ska det vara
  "Sweden Connect logo" eller inkludera texten "OpenID Federation Registry"?

  ---
Domänspecifika komponenter (kräver testning med skärmläsare)

- RegistrationDetailView.vue — Tabbar: v-tabs/v-window renderas av Vuetify
  som korrekt ARIA-tab-mönster, men kräver manuell genomgång att
  panelinnehållet (Entity Statement, Metadata Policy JSON, Trustmark requests)
  är begripligt utan visuellt stöd.

- FederationEntityEditView.vue — Expansion panels: v-expansion-panel hanteras
  av Vuetify med aria-expanded, men kräver testning med skärmläsare att
  flödet för att lägga till/ta bort moduler är logiskt i sekventiell läsordning.

- Alla <pre class="json-block">-element: Råa JSON-block i RegistrationDetailView
  och EntityConfigurationViewer är svåra att läsa med skärmläsare. Bör det
  erbjudas ett alternativt format (t.ex. strukturerad tabell)?

- Färgkontrast: Primärfärg #5a6751 (grön) mot vit bakgrund och #cd7a6e
  (rosa/secondary) kräver verifiering mot WCAG 1.4.3 (4.5:1 normal text,
  3:1 stor text). Använd axe eller Colour Contrast Analyser.

- text-grey-klasser: Vuetify renderar text-grey med potentiellt låg kontrast.
  Platshållare och hinttextar (text-medium-emphasis) behöver kontrastkontroll.

  ---
Feltexter från Spring (oförändrat)

Följande felmeddelanden returneras från Spring-backendens ErrorHandler och
visas direkt i den globala felbanderollen utan omformulering.

┌─────────────────────────────────┬──────────────────────────────────────────────────────┬────────────────────────────────────────────────────┐
│            Undantag             │                 Aktuellt detail-fält                 │                      Problem                       │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ DataIntegrityViolationException │ "Dataconstraint violation error, consult serverlogs" │ Teknisk text, uppmanar att konsultera serverloggar │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ MethodArgumentNotValidException │ "MethodArgumentNotValidException"                    │ Klassnamn i klartext, ej användarvänligt           │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ PropertyValidationFailException │ e.getMessage() (tekniskt undantagsmeddelande)        │ Beror på implementationsdetaljerna                 │
└─────────────────────────────────┴──────────────────────────────────────────────────────┴────────────────────────────────────────────────────┘

Viktigt: Fältspecifika valideringsfel (cause-arrayens field/detail-objekt)
parsas inte av frontend (request.js läser bara json.value.detail).
Valideringsfel visas aldrig bredvid det fält de rör — allt hamnar i den
globala banderollen.

  ---
FÖRESLAGNA NÄSTA STEG

1. Backend (sprint 2):
   - Utöka step-config-svar med displayName per property (åtgärdar fynd 16).
   - Revidera ErrorHandler.java för användarvänliga feltexter på
     DataIntegrityViolationException och MethodArgumentNotValidException.
   - Implementera frontend-parsning av cause-arrayen för fältspecifika fel.

2. Design (sprint 2):
   - Besluta om SVG-logotypens aria-label ska inkludera "OpenID Federation Registry".
   - Utvärdera workaround för svenska hint-texter (fynd 18).

3. Manuell granskning:
   - Boka session med NVDA/VoiceOver för att verifiera dialogflöden,
     tab-komponent och JSON-block.
   - Kontrastkontroll av #5a6751 och #cd7a6e med axe eller
     Colour Contrast Analyser.

4. Automatiserade regressionstester:
   - Sätt upp axe-core via Playwright eller Vitest-komponenttester.
   - Minst kritikalitetsgrad "critical" och "serious" bör fela CI-byggena.

  ---
ÄNDRADE FILER I DENNA SPRINT

frontend/src/assets/main.css
frontend/src/components/AppLayout.vue
frontend/src/components/EntityConfigurationViewer.vue
frontend/src/components/ListField.vue
frontend/src/components/TrustmarkSourcesField.vue
frontend/src/router/index.js
frontend/src/views/FederationEntityEditView.vue
frontend/src/views/HomeView.vue
frontend/src/views/LoginView.vue
frontend/src/views/RegistrationDetailView.vue
frontend/src/views/RegistrationFlowFormView.vue
frontend/src/views/RegistrationFlowsListView.vue
frontend/src/views/RegistrationsListView.vue
frontend/src/views/SubordinateFormView.vue
frontend/src/views/SubordinatesListView.vue
frontend/src/views/TrustmarkSubjectsListView.vue
frontend/src/views/TrustmarksListView.vue
service/src/main/resources/static/ (ny dist-build)
