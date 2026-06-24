────────────────────────────────────────────────────────

TILLGÄNGLIGHETSRAPPORT — OpenID Federation Registry Admin
Datum: 2026-06-23
Standard: WCAG 2.1 nivå AA
────────────────────────────────────────────────────────

SAMMANFATTNING

Totalt antal fynd: 26

- Kritiska (AA-brott):       8
- Viktiga (försämrad UX):    8
- Rekommendationer:          4
- Kräver mänsklig bedömning: 6

Automatiskt åtgärdbara:     14
Kräver mänsklig bedömning:  12

Granskat: 24 Vue-komponenter/vyer, index.html, main.css, Spring ErrorHandler

  ---
KRITISKA FYND — WCAG 2.1 AA-BROTT

  ---

1. Ikonknappar utan tillgängligt namn

- Fil: ListField.vue rad 40–51; TrustmarkSourcesField.vue rad 45–51 och 71–80; FederationEntityEditView.vue rad 261–270;
  RegistrationFlowFormView.vue rad 145–169
- WCAG-kriterium: 4.1.2 Name, Role, Value (A)
- Problem: Samtliga sju ikonknappar nedan saknar aria-label och har enbart en MDI-ikon som innehåll. Skärmläsare kan
  inte förstå vad knapparna gör.

| Fil                              | Knapp               | Ikon           |
  |----------------------------------|---------------------|----------------|
| ListField.vue:41                 | Ta bort listelement | mdi-delete     |
| TrustmarkSourcesField.vue:45     | Ta bort issuer      | mdi-delete     |
| TrustmarkSourcesField.vue:71     | Ta bort trustmark   | mdi-close      |
| FederationEntityEditView.vue:262 | Avregistrera flöde  | mdi-close      |
| RegistrationFlowFormView.vue:147 | Flytta steg uppåt   | mdi-arrow-up   |
| RegistrationFlowFormView.vue:154 | Flytta steg neråt   | mdi-arrow-down |
| RegistrationFlowFormView.vue:161 | Ta bort steg        | mdi-delete     |

- Påverkan: Skärmläsaranvändare hör bara "knapp" utan kontext. Omöjligt att använda formuläret.
- Förslag:
  <!-- ListField.vue -->
  <v-btn icon size="small" color="error" variant="text"
         :aria-label="`Remove item ${index + 1}`"
         @click="removeItem(index)">
    <v-icon aria-hidden="true">mdi-delete</v-icon>
  </v-btn>

  <!-- RegistrationFlowFormView.vue – flytta upp/ned -->
  <v-btn icon="mdi-arrow-up" size="small" variant="text"
         :aria-label="`Move step ${index + 1} up`"
         :disabled="index === 0 || saving"
         @click="moveStep(index, -1)" />
  - Auto-fix: Ja

  ---

2. Organisationsväljare utan label

- Fil: AppLayout.vue rad 46–58
- WCAG-kriterium: 1.3.1 Info and Relationships (A) / 4.1.2 Name, Role, Value (A)
- Problem: v-select för att byta organisation saknar label-prop och aria-label. Det enda syftet är visuellt
  kontextuellt (den syns bara när en användare har flera organisationer).
  <v-select
      :model-value="userStore.orgNumber"
      :items="userStore.organizations"
      item-title="orgName"
      item-value="orgNumber"
      density="compact"
      variant="outlined"
      hide-details      <!-- <-- ingen label -->
  - Påverkan: Skärmläsaranvändare vet inte vad de väljer.
  - Förslag:
  <v-select
      label="Organization"
      aria-label="Select organization"
      ...
  - Auto-fix: Ja

---

3. Klickbara tabellrader utan tangentbordsstöd

- Fil: RegistrationsListView.vue rad 61–76
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Problem: <tr>-element med @click och cursor: pointer-CSS saknar tabindex, role="button" och
  tangentbordshändelsehanterare. Det går inte att navigera till eller aktivera raderna med tangentboard.
  <tr
      v-for="reg in filteredRegistrations"
      class="clickable-row"
      @click="openDetail(reg.registrationId)"   <!-- ingen @keydown -->
  >
  - Påverkan: Tangentbords- och skärmläsaranvändare kan inte komma åt registreringsdetaljerna.
  - Förslag: Antingen omvandla raderna till <a> / <RouterLink>, eller:
  <tr
      v-for="reg in filteredRegistrations"
      role="button"
      tabindex="0"
      :aria-label="`View registration for ${reg.entityIdentifier}`"
      @click="openDetail(reg.registrationId)"
      @keydown.enter.prevent="openDetail(reg.registrationId)"
      @keydown.space.prevent="openDetail(reg.registrationId)"
  >
  - Auto-fix: Ja

---

4. Klickbara listposter utan tangentbordsstöd (JWKS-väljare)

- Fil: SubordinateFormView.vue rad 175–180; FederationEntityEditView.vue rad 499–505
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Problem: v-list-item med @click och style="cursor: pointer" saknar tabindex och tangentbordshändelsehanterare.
  JWKS-väljardialogen är ej tangentbordsanvändbar.
  <v-list-item
      v-for="item in jwksPickerItems"
      :key="item.entityId"
      :title="item.entityId"
      style="cursor: pointer"
      @click="applyJwksResult(item)"   <!-- ingen @keydown -->
  ></v-list-item>
  - Påverkan: Användare som enbart använder tangentbord kan inte välja en entitet i dialogrutan.
  - Förslag: Lägg till tabindex="0" och @keydown.enter.prevent="applyJwksResult(item)" på varje v-list-item.
  - Auto-fix: Ja

  ---

5. Dynamiska felmeddelanden saknar aria-live/role="alert"

- Fil: AppLayout.vue rad 92–100; LoginView.vue rad 22–28; EntityConfigurationViewer.vue rad 48–55;
  RegistrationDetailView.vue rad 133–140
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Problem: Alla v-alert-komponenter som visas villkorligt saknar role="alert" eller aria-live. Vuetify 3 lägger inte
  till dessa attribut automatiskt. Felmeddelanden meddelas inte skärmläsare när de dyker upp.
  <!-- AppLayout.vue – den globala felbanderollen -->
  <v-alert
      v-if="errorMessage"
      type="error"
      closable
      @click:close="clearError"
      class="mb-4"
  >
  - Påverkan: Skärmläsaranvändare missar alla dynamiska felmeddelanden.
  - Förslag:
  <v-alert
      v-if="errorMessage"
      type="error"
      role="alert"
      aria-live="assertive"
      ...
  >
  - Lägg till role="alert" på alla v-alert som visas villkorligt. För icke-brådskande statusar (t.ex. laddningsinfo) använd aria-live="polite" med role="status".
  - Auto-fix: Ja

  ---

6. Dialogrutor saknar aria-labelledby

- Fil: Alla v-dialog i: HomeView.vue (rad 155), SubordinatesListView.vue (rad 125), TrustmarksListView.vue (rad 117),
  TrustmarkSubjectsListView.vue (rad 113), RegistrationFlowsListView.vue (rad 91), FederationEntityEditView.vue (rad
  494, 517), SubordinateFormView.vue (rad 167),
  RegistrationDetailView.vue (rad 225), EntityConfigurationViewer.vue (rad 31)
- WCAG-kriterium: 4.1.2 Name, Role, Value (A)
- Problem: Vuetify 3 lägger automatiskt till role="dialog" och aria-modal="true", men kopplar inte automatiskt
  aria-labelledby till v-card-title. Skärmläsare vet inte vad dialogen heter.
- Påverkan: När dialogen öppnas hör skärmläsaranvändare bara "dialog" utan namn.
- Förslag: Lägg till id på titel och aria-labelledby på dialog. Exempelfix för borttagningsdialog (gäller samtliga):
  <v-dialog v-model="deleteDialog" max-width="500"
            aria-labelledby="delete-dialog-title">
    <v-card>
      <v-card-title id="delete-dialog-title" class="text-h5">
        Confirm Delete
      </v-card-title>
  - Auto-fix: Ja (formuläriskt — men kräver id-attribut på varje dialoginstans)

  ---

7. Sidtitel ändras inte vid navigation

- Fil: index.html rad 28; frontend/src/router/index.js
- WCAG-kriterium: 2.4.2 Page Titled (A)
- Problem: <title>OpenID Federation Admin</title> är statisk och ändras aldrig. Alla 24 routes har samma sidtitel,
  vilket gör det omöjligt för skärmläsaranvändare att veta var de befinner sig efter en navigering.
- Påverkan: Skärmläsaranvändare hör alltid samma titel oavsett vilken sida de besöker.
- Förslag: Lägg till en useHead/document.title-uppdatering per vy, eller hantera det globalt i router:
  // router/index.js
  router.afterEach((to) => {
  document.title = `${to.meta.title ?? to.name} — OpenID Federation Admin`;
  });
- Lägg till meta: { title: 'Entities' } etc. i varje route-definition.
- Auto-fix: Delvis (routing-konfiguration krävs per vy)

  ---

8. Samtliga vyer saknar <h1>

- Fil: Alla vyer; AppLayout.vue
- WCAG-kriterium: 1.3.1 Info and Relationships (A) / 2.4.6 Headings and Labels (AA)
- Problem: Alla vyer börjar med <h2> som högsta rubriknivå (t.ex. "Entities", "Subordinates"). Det finns ingen <h1>
  någonstans i applikationen. v-card-title renderas som <div> (inte heading-element) av Vuetify. Huvud-<nav> i app-baren
  saknar rubrik.
- Påverkan: Skärmläsaranvändare kan inte hoppa till sidans huvudrubrik. Rubrikhierarkin är trasig i alla vyer.
- Förslag: Lägg till en visuellt dold <h1> per vy (antingen i AppLayout via router-meta, eller i varje vy):
  <!-- Varje vy, t.ex. HomeView.vue -->
  <h1 class="sr-only">Entities — OpenID Federation Registry</h1>
  <h2>Entities</h2>
  - CSS för .sr-only:
  .sr-only {
    position: absolute; width: 1px; height: 1px;
    padding: 0; margin: -1px; overflow: hidden;
    clip: rect(0,0,0,0); border: 0;
  }
  - Auto-fix: Delvis

  ---

VIKTIGA FYND — FÖRSÄMRAD UX FÖR HJÄLPMEDEL

  ---

9. Informationsikoner i tooltip-aktivatorer är ej fokuserbara

- Fil: SubordinatesListView.vue rad 79–89; TrustmarkSubjectsListView.vue rad 43–48; TrustmarksListView.vue rad 42–43
- WCAG-kriterium: 2.1.1 Keyboard (A)
- Problem: v-icon används som aktivatorer för v-tooltip men v-icon är inte fokuserbara (inget tabindex).
  Tangentbordsanvändare kan aldrig ta del av tooltip-informationen ("EC Location configured", "Remote entity").
  <v-tooltip text="EC Location configured" location="top">
    <template v-slot:activator="{ props }">
      <v-icon v-bind="props" size="small">mdi-link</v-icon>
      <!-- ej fokuserbar -->
    </template>
  </v-tooltip>
  - Förslag: Byt till ett fokusbart element eller exponera informationen direkt:
  <v-tooltip text="EC Location configured" location="top">
    <template v-slot:activator="{ props }">
      <span v-bind="props" tabindex="0" role="img"
            aria-label="EC Location configured">
        <v-icon aria-hidden="true" size="small">mdi-link</v-icon>
      </span>
    </template>
  </v-tooltip>
  - Auto-fix: Ja

---

10. API-länk öppnar ny flik utan förvarning

- Fil: AppLayout.vue rad 84
- WCAG-kriterium: 2.4.4 Link Purpose (A) / 3.2.2 On Input (A)
- Problem: <a :href="swaggerUiPath" target="_blank">API</a> öppnar Swagger UI i ny flik utan att indikera detta. Oväntad
  kontextförändring.
- Förslag:
  <a :href="swaggerUiPath" rel="noopener" target="_blank"
     class="nav-link"
     aria-label="API documentation (opens in new tab)">
    API
    <v-icon aria-hidden="true" size="x-small" class="ml-1">mdi-open-in-new</v-icon>
  </a>
  - Auto-fix: Ja

  ---

11. Laddningsstatus annonseras inte dynamiskt

- Fil: Alla vyer med v-progress-circular (minst 12 förekomster)
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Problem: Laddningsindikatorer med bredvidliggande text (<p class="mt-4 text-grey">Loading entities...</p>) har ingen
  role="status" eller aria-live. Visas villkorligt men annonseras inte av skärmläsare.
- Förslag:
  <div role="status" aria-live="polite">
    <v-progress-circular indeterminate aria-hidden="true" />
    <p class="mt-4 text-grey">Loading entities...</p>
  </div>
  - Auto-fix: Ja

---

12. Dekorativa ikoner saknar aria-hidden

- Fil: SubordinatesListView.vue rad 42–48; TrustmarksListView.vue rad 42–43; TrustmarkSubjectsListView.vue rad 43–48
- WCAG-kriterium: 1.1.1 Non-text Content (A)
- Problem: Dekorativa v-icon-element bredvid förklarande text saknar aria-hidden="true". Skärmläsare kan läsa upp
  ikonnamnen (t.ex. "certificate outline icon") onödigtvis.
  <v-icon class="mr-2">mdi-certificate-outline</v-icon>
  <span>Trustmark Issuer</span>  <!-- texten förklarar redan -->
  - Förslag: Lägg till aria-hidden="true" på alla dekorativa v-icon.
  - Auto-fix: Ja

  ---

13. SVG-logotyp saknar role="img"

- Fil: AppLayout.vue rad 22–33
- WCAG-kriterium: 1.1.1 Non-text Content (A)
- Problem: SVG-logotypen har aria-label="Sweden Connect" men saknar role="img". Stödet för aria-label på <svg> utan
  role="img" varierar mellan webbläsare och skärmläsare.
- Förslag: <svg ... aria-label="Sweden Connect" role="img">
- Auto-fix: Ja

  ---

14. Valideringsfel för pipeline-steg annonseras inte

- Fil: RegistrationFlowFormView.vue rad 128–131
- WCAG-kriterium: 4.1.3 Status Messages (AA)
- Problem: Valideringsfelet "No steps selected" visas genom att ändra CSS-klass (text-error / text-grey) men saknar
  role="alert". Skärmläsare missar felet vid formulärsändning.
  <span :class="stepsValidationAttempted ? 'text-error' : 'text-grey'">
    No steps selected. Add at least one step above.
  </span>
  - Förslag: <span role="alert" aria-live="assertive" v-if="stepsValidationAttempted && selectedSteps.length === 0">
  - Auto-fix: Ja

  ---

15. Focus-visible-styling saknas för navigeringslänkar

- Fil: AppLayout.vue (scoped CSS) rad 195–215
- WCAG-kriterium: 2.4.7 Focus Visible (AA)
- Problem: .nav-link-klassen definierar :hover men inte :focus-visible. Tangentbordsanvändare saknar synlig
  fokusindikator på navigeringslänkarna.
  .nav-link:hover { color: #4a6741; }
  /* saknas: .nav-link:focus-visible { ... } */
- Förslag:
  .nav-link:focus-visible {
  outline: 2px solid #4a6741;
  outline-offset: 2px;
  border-radius: 2px;
  }
- Auto-fix: Ja

  ---

16. Beskrivning av stegkonfigurationsfält (cfg.key)

- Fil: RegistrationFlowFormView.vue rad 195–205
- WCAG-kriterium: 2.4.6 Headings and Labels (AA)
- Problem: Formulärfält för stegkonfiguration använder det tekniska backend-attributet cfg.key som label (t.ex. "
  allowSelfSigned", "timeoutSeconds"). Dessa nyckelnamn är inte användarvänliga.
- Förslag: Backend bör returnera ett displayName-fält per config-egenskap. Frontend använder cfg.displayName ?? cfg.key.
- Auto-fix: Nej (kräver backendändring)

  ---

REKOMMENDATIONER — BEST PRACTICE
  
---

17. Lägg till skip-navigation-länk

- Fil: index.html / AppLayout.vue
- Förslag: Placera en visuellt dold länk som "Skip to main content" som första fokuserbara element på sidan:
  <!-- AppLayout.vue, första barn i <v-app> -->
  <a href="#main-content" class="skip-link">Skip to main content</a>
  <!-- ... -->
  <v-main id="main-content">
  .skip-link { position: absolute; transform: translateY(-100%); }
  .skip-link:focus { transform: translateY(0); }

  ---

18. Globalt err-meddelande: lägg till lang-attribut för svenska texter

- Fil: RegistrationFlowFormView.vue rad 66 (hint: "Beskrivning av flödet på svenska")
- WCAG-kriterium: 3.1.2 Language of Parts (AA)
- Förslag: Vuetify renderar hint-text som en <div>; det går inte att sätta lang-attribut via prop. Flytta den svenska
  beskrivningstexten till ett separat <span lang="sv"> bredvid fältet.

  ---

19. Explicit <caption> eller aria-label på tabeller

- Fil: Alla vyer med v-table (HomeView, SubordinatesListView, etc.)
- Förslag: Vuetify 3:s v-table lägger inte till <caption>. Lägg till en visuellt dold caption som förklarar tabellens
  syfte:
  <v-table>
    <caption class="sr-only">List of entities</caption>

  ---

20. Navigationslandmärke saknar aria-label

- Fil: AppLayout.vue rad 80–85
- Förslag: <nav>-elementet i app-barens extension-slot bör ha aria-label för att skilja det från eventuell framtida
  ytterligare navigation:
  <nav v-if="userStore.isAuthorized" aria-label="Main navigation" class="nav-bar">

  ---

KRÄVER MÄNSKLIG BEDÖMNING

  ---
Alt-texter

- AppLayout.vue, rad 22–33: SVG-logotypen har aria-label="Sweden Connect". Kontrollera att detta är korrekt i
  sammanhanget — ska det vara "Sweden Connect logo" eller inkludera texten "OpenID Federation Registry"?

  ---

Domänspecifika komponenter

- RegistrationDetailView.vue — Tabbar: Komponenterna v-tabs / v-window renderas av Vuetify som korrekt ARIA-tab-mönster,
  men kräver manuell genomgång i assistive technology att panelinnehållet (Entity Statement, Metadata Policy JSON-block,
  Trustmark requests) är begripligt utan
  visuellt stöd.
- FederationEntityEditView.vue — Expansion panels: v-expansion-panel hanteras av Vuetify med aria-expanded, men kräver
  testning med skärmläsare att flödet för att lägga till/ta bort moduler är logiskt i sekventiell läsordning.
- Alla <pre class="json-block">-element: Råa JSON-block i RegistrationDetailView och EntityConfigurationViewer är svåra
  att läsa med skärmläsare. Bör det erbjudas ett alternativt format (t.ex. en strukturerad tabell)?
- Färgkontrast: Applikationens primärfärg #5a6751 (grön) mot vit bakgrund och #cd7a6e (rosa/secondary) kräver rendering
  för att kontrastkontrolleras mot WCAG 1.4.3 (4.5:1 för normal text, 3:1 för stor text). Bör verifieras med ett verktyg
  som axe eller Colour Contrast Analyser.
- text-grey-klasser: Vuetify renderar text-grey med låg kontrast. Platshållare och hinttextar (text-medium-emphasis)
  behöver kontrastkontroll.

  ---

Feltexter från Spring

Följande felmeddelanden returneras från Spring-backendens ErrorHandler och visas direkt i den globala felbanderollen
utan omformulering. De kan behöva revideras för att vara begripliga för slutanvändare:

┌─────────────────────────────────┬──────────────────────────────────────────────────────┬────────────────────────────────────────────────────┐
│ Undantag │ Aktuellt detail-fält │ Problem │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ DataIntegrityViolationException │ "Dataconstraint violation error, consult serverlogs" │ Teknisk text, uppmanar att
konsultera serverloggar │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ MethodArgumentNotValidException │ "MethodArgumentNotValidException"                    │ Klassnamn i klartext, ej
användarvänligt │
├─────────────────────────────────┼──────────────────────────────────────────────────────┼────────────────────────────────────────────────────┤
│ PropertyValidationFailException │ e.getMessage() (tekniskt undantagsmeddelande)        │ Beror på
implementationsdetaljerna │
└─────────────────────────────────┴──────────────────────────────────────────────────────┴────────────────────────────────────────────────────┘

Viktigt: Fältspecifika valideringsfel (cause-arrayens field/detail-objekt) parsas inte av frontend (request.js läser
bara json.value.detail). Valideringsfel visas aldrig bredvid det fält de rör — allt hamnar i den globala banderollen.

  ---
FILER UTAN FYND

Följande filer granskades och bedömdes inte innehålla egna tillgänglighetsproblem (deras problem härrör från
återanvändning av mönster redovisade ovan, eller är komponentbiblioteket Vuetifys ansvar):

- frontend/index.html — lang="en" finns, <title> finns (men statisk, se fynd 7)
- frontend/src/App.vue — enkel wrapper-komponent
- frontend/src/views/PolicyListView.vue och PolicyFormView.vue — inga unika mönster utöver de som identifierats ovan
- frontend/src/views/TrustmarkSubjectFormView.vue — Vuetify-formulär med korrekt required-prop och label-koppling
- frontend/src/components/ChipField.vue — Vuetify v-combobox hanterar accessibility internt
- frontend/src/stores/errorStore.js / userStore.js — inga UI-element

  ---

FÖRESLAGNA NÄSTA STEG

1. Kritiska fynd — sprint 1 (auto-fixbara): Åtgärda fynd 1–6 och 8–15. Samtliga är enstaka kodändringar. Föreslå att
   agenten kör implementeringsfasen för dessa.
2. Kritiska fynd — sprint 2 (kräver design): Fynd 7 (dynamisk sidtitel) och 8 (h1-hierarki) kräver en
   vy-till-rubrik-mappning och möjligen en komponentarkitekturförändring.
3. Backend: Revidera ErrorHandler.java så att DataIntegrityViolationException och MethodArgumentNotValidException
   returnerar användarvänliga detail-texter. Implementera frontend-parsning av cause-arrayen för att visa fältspecifika
   fel bredvid respektive formulärfält.
4. Manuell granskning: Boka session med NVDA/VoiceOver för att verifiera dialogflöden, tab-komponent och JSON-block (
   punkterna under "Kräver mänsklig bedömning").
5. Automatiserade regressionstester: Sätt upp axe-core via Playwright eller Vitest-komponenttester. Minst
   kritikalitetsgrad "critical" och "serious" bör fela CI-byggena. Prioritera formulärvyer och dialogmönster.
