export type UserRole = "STUDENT" | "TEACHER" | "BIBLIOTHEEKBEHEERDER" | "ADMIN";

export interface MeResponse {
  id: number;
  role: UserRole;
  school: {
    id: number;
    name: string;
    domain: string;
  } | null;
  classes: {
    id: number;
    name: string;
    grade: string;
    schoolYear: string;
  }[];
}

export interface AdminUser {
  id: number;
  smartschoolUid: string;
  role: UserRole;
  active: boolean;
  school: {
    id: number;
    name: string;
    domain: string;
  };
  classes: {
    id: number;
    name: string;
    grade: string;
    schoolYear: string;
  }[];
}
