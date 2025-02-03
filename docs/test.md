# Openid Federation - Översikt

OpenId Federation är en implementation av specifikationen OpenID Federation 1.0 - draft 41.
Det finns tre primära systemkomponenter som ofta kallar federations noder eller moduler, openid-federaton-service,
registry och registry-admin.

### Openid-Federaton-Service

Implementation av specifikationen, har fyra moduler. Trustanchor(TA), Intermidiate(IM),Resolver,
TrustMarkIssuer(TMI). Även kallade federations-noder.

### Openid-Federation-Registry

Tjänst som sparar konfigurationen för de olika komponenterna och levererar ut till openid-federations-servicen.
Sparar sin information i en Maria DB databas.

Två restapi:er, service-api, används av service-admin. Federation-api levererar data till openid-federation-service.

### Openid-Federation-Registry-Admin

Detta är en egen tjänst med databas och webbgränssnitt som hjälper användaren att administrera federationen
eller enskilda noder/moduler.

_Entity konfiguration / Metadata hantering_

Möjlighet att lägga till nya entity konfigurationer. Men också möjligheten att lägga till hosted metadata.
I vanliga fall hanteras metadata av tjänsten själv genom att exponera den på en specifik endpoint.
För att underlätta en övergång erbjuds möjligheten att lägga in metadata direkt i federationen.

_Policy_

Lägg till och editera olika typer av OIDF Policys.

_TrustMarkSubject_

Dela ut trustmarks till olika subjects, kunna revokera, aktivera och deaktivera dessa vid vissa datum.

Exempel på trustmark: https://www.swedenconnect.se/loa3, ett TM som signalerar att en Openid_Provider har LOA3 nivå.

_Publish_

Tjänsten kan editera data, först när man väljer att publicera data kommer den att sparas i registret
och bli tillgänglig för federationen.

_Modul hantering(Kommande funktionalitet)_

Skapa upp och administrera federationsnoder.

_Säkerhet_

Registry-Admin följer flödet authorization codegrant. Registry federationsapi signerar sina utgående
request för att openid-federation-service skall kunna säkerställa dess äkthet.
Dessa endpoints förväntas vara skyddad bakom en brandvägg eller liknande. Data som kommer ut här är
inte hemlig.

## Användarroller för Metadatahantering

Här följer en beskrivning av de olika användarroller som registry-admin förväntas betjäna.

_SuperAdmin_

_Federations - Operatör (SuperUser)_
En operatör för en federation, möjlighet att lägga till subordinates, skapa trustmarks. Sätta upp nya IM etc

_Organisation - Admin_

Admin för en organisation, kan hantera alla funktioner som är knuten till en organisation.
Även hantering av behörigheter för organisationens användare.

_Organisation - Användare_

Vanlig användare i sin organisation eller flera, om det är en underleverantör som hanterar flera organisationer.
Denna användare kan inte hantera användarbehörigheter.

_Funktioner_

Nedan lista definerar vilka olika funktioner som finns i gränssnittet.

Här kommer också begreppet "Organisation" en person tillhöra en organisation

| Funktion                          | Beskrivning                                                      | FedOp | OrgAdmin | OrgUser |   |
|-----------------------------------|------------------------------------------------------------------|-------|----------|---------|---|
| Modul TrustAnchor                 | CRUD                                                             | CRUD  |          |         |   |
| Modul Resolver                    | CRUD                                                             | CRUD  |          |         |   |
| Modul Intermediate                | CRUD                                                             | CRUD  |          |         |   |
| Modul TrustMarkIssuer             | CRUD                                                             | CRUD  |          |         |   |
| Federation Policy                 | CRUD                                                             | CRUD  |          |         |   |
| Organisations Mappning            | CRUD för att lägga till en organisation och tillhörande entityid | CRUD  |          |         |   |
| Federation TrustMark              | CRUD                                                             | CRUD  |          |         |   |
| Federation TrustMarkSubject       | CRUD - Tilldela ett definierat TM till ett subject               | CRUD  | CRUD     |         |   |
| Instance                          | Skapas via property fil i registry                               | R     |          |         |   | 
| Federation Entity                 | CRUD för metadata hantering och lägga till subordinate           | CRUD  |          |         |   |
| Federation Entity Metadata        | Toggla hosted icke hosted / uppdatera metadata / nycklar         |       |          |         |   |
| Federation Entity TilldelaSubject | Tilldela ett subject från tex en TMI issuer                      |       |          |         |   |

Frågeställning:
En stor del i swedenconnect är att identifiera tekniska kontaktpersoner och knyta dem till en aktör.

_Icke funktionella krav_

* I användargränssnitt skall det tydligt framgå när information har skapats och uppdaterats.
  Även vilken inloggad användare som skapat och uppdaterat informationen.

* Serversidan skall ha en strukturerad audit-logg där information finns om förändrad data som är kopplad till
  inloggad användare. Inloggad användare hämtas från tokens sub claim samt aud:sub claim.

* Applikationslogggning skall finnas där tjänsten skriver ut viktig händelser.

* På begäran komprimera svaret enligt http standarden
* Fungera med fler en en instans
* Transaktions säker, vid skrivande operationer
* Hantera en rimligt höglast minst 5-10request/sec

## Funktionalitet gällande Metadata hantering

_Modul hantering_

Lägga till och tabort olika moduler så som TM,IM,Resolver och TMI.

_Trustmark hantering_

Skapa upp nya trustmarks enligt specifikation.

_Trustmark Subject hantering_
Tilldela redan skapade trustmarkt till olika subjects. Revokera dessa, eller använda en datum funktion för publicering
eller revokering.

_import/export fil format_

Kunna exportera ut sin konfiguration av alla inställningar för en organisation.

_Metadata - Validering_

Frågeställning:
Hur fungerar det med validering? Här tror jag att vi behöver dels grundläggande att syntaxen är korrekt men även att
viss validering sker av kritiska attribut gentemot federationens regler. ex. avtal, ID attribut, byte av org namn ex.

Diskussion:
Validera eventuella JWK:er som finns i metadatat genom att kontrollera att det är den publika delen.

Förstagången man vill lägga till metadata kan en dropdown finnas med metadataexempel för de vanligaste användarfallen.
Tex relying_party. Det skall fortsatt vara möjligt att lägga in fri väl formaterad json.

_Metadata - Policy Filtrering_

Kunna granska sin metadata efter att en vald policy har applicerats och filtrerat sin metadata.

Diskussion:
Kan metadatat valideras mot ett jsonschema som pekas ut av policyn?

_Publisering / Granskning_

Frågeställning:
Ska all eller vissa publiceringar gå igenom någon form av manuell granskning före publicering? Om ja,
så behöver det finnas någon form av request for publish funktion där någon kan gå in och granska och styra publicering.

Tidsaspekten, som det fungerar idag vill en del ha sin metadata publicerad vid visst datum eller tidpunkt då andra
system etc kan vara beroende. Hur fungerar det inom oidc, någon form möjlighet till tidsstyrning kan nog vara bra.

Diskussion:
Det finns en publish funktion, men ett gransknings förfarande anses inte behövas.
Men fult möjligt att lägga till då funktionen publish redan finns.

Tror att vi behöver avvakta tidsstyrd publisering. Det är fullt möjligt att lägg atill vid behov.
Om man själv kan trycka på publiserings knappen för behovet av funtionen bli låg.

_Avtals efterlevnad_

Frågeställning:
Hur fungerar det med ev. avtal mm? Det här är ett stort problem idag i SC där metadatat måste stämmas av mot
information som finns i avtal. Om det finns liknande inom oidc att olika organisationer kan ha olika avtal
så bör vi ha någon form av stöd att särskilja, validera att rätt avtal är deklarerat och att inte data som
"tillhör" ett annat avtal kan publiceras för organisation. Här är vore det bra om det finns stöd att göra
detta maskinellt med att hämta info via något api eller liknande och sen behandla den info vi får tillbaka.

Diskussion:
OIDF har en funktion som kallas för Trustmarks(TM). Det han liknas vid ett avtal. TrustMarkSubjects kan liknas vid
underskriften
av en aktör som ingår avtalet.

Om vi har ett avtal för Valfrihets systemet kan detta översättas till ett TM vid
namn https://www.swedenconnect.se/valfrid.
Detta TM kan sedan tilldelas(TrustmarkSubject) till en aktör: https://www.digg.se/ourpublicservice.
Ett TrustMarksSubject(TMS) kan ha en giltighetsperiod med start och stopp.

En möjlig lösning är en integrations komponent som kopplar upp sig till ett avtals register och översätter de olika
avtalen till TM och TMS. Den använder Registryts API för att uppdatera och revokera TMS.

Den andra lösningen är att det sker via admin gränssnittet där man också kan hantera detta.

_Hjälp texter i GUI_

Hjälp texter som kan visas upp när man klickar på ett frågetecken vid fältet som skall matas in.

## Begrepps lista

| Begrepp         | Betydelse                                                                                        |
|-----------------|--------------------------------------------------------------------------------------------------|
| TA              | TrustAnchor nod                                                                                  |
| TMI             | TrustMarkIssuer nod                                                                              |
| Res/Resolver    | Resolver Nod som räknar ut tillitskedjan                                                         |
| IM              | Intermidiate nod                                                                                 |
| TMS             | TrustMarkSubject                                                                                 |
| TM              | TrustMark                                                                                        |
| JWKS            | JSON representation av public asymetrisk nyckel                                                  |
| Metadata        | Här syftar vi till den metadata element som återfinns i EntityStatement                          | 
| EntityStatement | En json struktur som specificeras av OpenID federationen                                         |
| HostedMetadata  | När metadata exponeras från en IM                                                                |
| Nod/Modul       | Syftar till en modul i federationen ex TM,TMI,Resolver,IM                                        |
| JWT             | En signerad JSON struktur. De flesta endpoints från federations noderna svarar med detta format. |

