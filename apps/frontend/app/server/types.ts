export interface Saksbehandler {
  navn: string;
  epost: string;
  navIdent: string;
  accessToken?: string;
}

export interface AuthConfig {
  clientId: string;
  clientSecret: string;
  redirectUri: string;
}
