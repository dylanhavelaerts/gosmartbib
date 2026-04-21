export interface SchoolIntegrationDTO {
  schoolId: number;
  onerosterBaseUrl: string;
  onerosterClientId: string;
  onerosterEnabled: boolean;
  clientSecretConfigured: boolean;
  lastTestSuccessfulAt: string | null;
  lastSyncAt: string | null;
  lastError: string | null;
}

export interface UpsertSchoolIntegrationRequest {
  onerosterBaseUrl: string;
  onerosterClientId: string;
  onerosterClientSecret: string;
  onerosterEnabled: boolean;
}

export interface SchoolIntegrationTestResponse {
  success: boolean;
  tokenReceived: boolean;
  schoolsEndpointReachable: boolean;
  schoolCount: number;
  message: string;
}

export interface SchoolIntegrationLiveUsersResponse {
  success: boolean;
  userCount: number;
  users: Record<string, unknown>[];
  message: string;
}

export interface SchoolIntegrationLiveClassesResponse {
  success: boolean;
  classCount: number;
  classes: Record<string, unknown>[];
  message: string;
}
