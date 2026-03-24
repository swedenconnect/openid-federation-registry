# Lösningsdesign för portallösning

## Problemställning

Anslutningen till federationen behöver fungera på ett sådant sätt att den är attraktiv för anslutande parter. Det ska
vara enkelt att uppdatera metadata och utföra andra operationer som är kopplade till livscykeln för en anslutande part.
Det ska också finnas en tydlig spårbarhet kring vilka operationer som har utförts i federationen, och självklart full
kontroll över vem som har utfört hanteringen.
Frågan om vem som utför operationer hanteras utanför denna lösning, i anslutningsprocessen, och behandlas därför inte
här.
Förutsättningen är att en inloggad användare finns tillgänglig med organisationens namn och organisationsnummer.
Olika typer av anslutning i olika miljöer
Federationen kommer att finnas i olika miljöer. Sweden Connect behöver tillhandahålla Sandbox, QA och Produktion.
I Sandbox-miljön är förutsättningen att metadata publiceras direkt utan manuell granskning av federationsoperatören.
I QA och Produktion behöver viss manuell kontroll ske.

## Administrativa operationer som behöver stödjas

### Hosta metadata

Detta är ett alternativ som federationsoperatören kan tillhandahålla för att underlätta anslutningen. Den anslutande
parten behöver då inte själv exponera ett entity statement.
Ansluta sig till en intermediate
För att kunna gå med i federationen behöver en part vid något tillfälle ansluta till en intermediate för att bli synlig
och kunna resolvas i federationen.
Ansöka om och tilldela trustmarks
Trustmarks används för att deklarera olika egenskaper hos en anslutande part i federationen. Ofta är detta kopplat till
olika avtal, och tilldelningen kan ske på olika sätt.
Lösningsförslag för att ansluta sig till en intermediate
Förutsättningen är att man har ett entityID där ett fungerande entity statement kan hämtas av Sweden Connects servrar.
Anslutningen sker genom att en IM utfärdar ett subordinate statement.
Förslaget är att ett nytt begrepp skapas i den tekniska komponenten registry, med namnet registration_flow.
Ett registration_flow är kopplat till en intermediate där olika regler definierar hur en anslutning kan ske.
Exempel på ett registration_flow för en sandbox-miljö
belongs_to_im/ta: https://fed.swedenconnect.se/registered-rp

registration_flow_name: "This is a flow that auto-registers RPs"

registration_policy: https://policy.sc.se/selfregister

registration_constraints:
  <functiongroup>, validateduser, <organization_org_nr>, other?

flow:
entitystatement_validation,
swedenconnect_metadata_validation,
jwks_validation,
  <manual_approval>
metadata_policy_generator_template
"metadata_policy": {
  "openid_relying_party": {
    "grant_types": {
      "default": [
        "authorization_code"
      ],
      "subset_of": [
        "authorization_code",
        "refresh_token"
      ],
      "superset_of": [
        "authorization_code"
      ]
    },
    "token_endpoint_auth_method": {
      "one_of": [
        "private_key_jwt",
        "self_signed_tls_client_auth"
      ],
      "essential": true
    },
    "token_endpoint_auth_signing_alg": {
      "one_of": [
        "PS256",
        "ES256"
      ]
    },
    "organization_name": {
      "value": "${currentuser.organization_name}"
    },
    "randomclaim": {
      "value": "${metadatavalue}"
    },
    "contacts": {
      "add": [
        "helpdesk@federation.example.org"
      ]
    }
  }
}
