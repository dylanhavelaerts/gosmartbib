export interface SmartschoolUser {
  smartschoolUserId: string;
  name?: string; // Optioneel, we gebruiken dit enkel in de UI, wordt niet naar DB gestuurd
  classGroup: string;
  school: string;
  schoolId: string;
}