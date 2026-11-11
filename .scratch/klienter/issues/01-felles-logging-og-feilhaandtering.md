# 01: Samle logging og feilhåndtering i øvrige HTTP-klienter

Status: needs-triage

## Bakgrunn

I oppfølgingen av #149 flyttes logging, tidsmåling og feilhåndtering fra
`OppdragClient` til `infrastruktur/http/EksternTjenesteKaller.kt`.
Øvrige klienter har fortsatt egen håndtering. Oppryddingen i dem tas separat.

## Oppfølging

- [ ] Gå gjennom øvrige HTTP-klienter i `apps/sak`, blant annet `InfotrygdClient` og `OppgaveClient`.
- [ ] Bruk `kallEksternTjeneste` der det passer. Bruk `kallEksternTjenesteMedRessurs` bare for tjenester som svarer med `Ressurs`.
- [ ] Fjern overlappende logging og feilhåndtering. Sensitive detaljer og stacktracer skal bare til `secureLogger`, som sender til Team Logs.
- [ ] Bevar tokenvalg, headere og klientspesifikk validering. Avklar eventuelle endringer i feilmapping før migreringen, særlig forventede HTTP-statuskoder og tomme svar.
- [ ] Tilpass testene for forventede utfall, HTTP- og transportfeil og skillet mellom vanlig og skjermet logging.
