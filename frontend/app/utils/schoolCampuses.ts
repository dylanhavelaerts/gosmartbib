import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";

export async function fetchSchoolCampuses(
  apiUrl: string,
  schoolId: number,
): Promise<SchoolCampusDTO[]> {
  const response = await fetch(`${apiUrl}/admin/schools/${schoolId}/campuses`, {
    credentials: "include",
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(body || "Campussen ophalen mislukt");
  }

  return response.json();
}

export function getCampusSelectOptions(
  campuses: SchoolCampusDTO[],
  currentCampus?: string | null,
): SchoolCampusDTO[] {
  const trimmedCurrentCampus = currentCampus?.trim() ?? "";

  if (!trimmedCurrentCampus) {
    return campuses;
  }

  const alreadyExists = campuses.some(
    (campus) =>
      campus.name.trim().toLowerCase() === trimmedCurrentCampus.toLowerCase(),
  );

  if (alreadyExists) {
    return campuses;
  }

  return [{ id: 0, name: trimmedCurrentCampus }, ...campuses];
}
