/**
 * Configuratie en status van de OneRoster-integratie van een school.
 */
export interface SchoolIntegrationDTO {
  schoolId: number;
  schoolName: string;
  schoolDomain: string;
  schoolBaseUrl: string;
  onerosterClientId: string;
  onerosterEnabled: boolean;
  clientSecretConfigured: boolean;
  smartschoolAccesscodeConfigured: boolean;
  lastTestSuccessfulAt: string | null;
  lastSyncAt: string | null;
  lastError: string | null;
}

/**
 * Payload waarmee een platformbeheerder de OneRoster-integratie opslaat.
 *
 * De client secret en Smartschool accesscode worden naar de backend gestuurd,
 * maar worden om veiligheidsredenen niet opnieuw zichtbaar teruggegeven.
 */
export interface UpsertSchoolIntegrationRequest {
  schoolBaseUrl: string;
  onerosterClientId: string;
  onerosterClientSecret: string;
  onerosterEnabled: boolean;
  smartschoolAccesscode?: string;
}

/**
 * Resultaat van het testen van de OneRoster-integratie.
 */
export interface SchoolIntegrationTestResponse {
  success: boolean;
  tokenReceived: boolean;
  schoolsEndpointReachable: boolean;
  schoolCount: number;
  message: string;
}

/**
 * Response met live gebruikers uit OneRoster.
 */
export interface SchoolIntegrationLiveUsersResponse {
  success: boolean;
  userCount: number;
  users: Record<string, unknown>[];
  message: string;
}

/**
 * Response met live klassen uit OneRoster.
 */
export interface SchoolIntegrationLiveClassesResponse {
  success: boolean;
  classCount: number;
  classes: Record<string, unknown>[];
  message: string;
}

export interface SchoolCampusDTO {
  id: number;
  name: string;
}
