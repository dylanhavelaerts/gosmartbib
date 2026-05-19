"use client";

import type { MeResponse, AdminUser, UserRole } from "@/app/interfaces/user";
import { useEffect, useState } from "react";
import Link from "next/link";
import "./userAdmin.css";
import Pagination from "@/app/catalog/pagination";
import SyncModal from "./SyncModal";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "STUDENT", label: "LEERLING" },
  { value: "TEACHER", label: "LEERKRACHT" },
  { value: "BIBLIOTHEEKBEHEERDER", label: "BEHEERDER" },
];

type DisplayNamesResponse = {
  success: boolean;
  requestedCount: number;
  resolvedCount: number;
  displayNames: Record<string, string>;
  unresolvedUids: string[];
  message: string;
};

type SyncStatus = "idle" | "confirm" | "loading" | "done";

type SchoolSyncResult = {
  schoolDomain: string;
  added: number;
  removed: number;
  errors: string[];
};

type SyncSummary = {
  totalAdded: number;
  totalRemoved: number;
  schools: SchoolSyncResult[];
};

const SYNC_STEPS = [
  "Verbinding maken met OneRoster...",
  "Gebruikers ophalen...",
  "Vergelijken met huidige data...",
  "Wijzigingen opslaan...",
  "Afronden...",
];

const replaceRoleName = (role: string): string => {
  switch (role) {
    case "BIBLIOTHEEKBEHEERDER":
      return "BEHEERDER";
    case "TEACHER":
      return "LEERKRACHT";
    case "STUDENT":
      return "LEERLING";
    default:
      return "-";
  }
};

type ApprovedSchool = { id: number; name: string };

export default function AdminUserPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [me, setMe] = useState<MeResponse | null>(null);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [displayNames, setDisplayNames] = useState<Record<string, string>>({});
  const [selectedRoles, setSelectedRoles] = useState<Record<number, UserRole>>(
    {},
  );
  const [savingUserId, setSavingUserId] = useState<number | null>(null);
  const [succes, setSucces] = useState("");
  const [searchQuery, setSearchQuery] = useState("");

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const [syncStatus, setSyncStatus] = useState<SyncStatus>("idle");
  const [syncStep, setSyncStep] = useState(0);
  const [syncResult, setSyncResult] = useState<SyncSummary | null>(null);

  const [approvedSchools, setApprovedSchools] = useState<ApprovedSchool[]>([]);
  const [selectedSchoolId, setSelectedSchoolId] = useState<number | null>(null);

  useEffect(() => {
    const init = async () => {
      if (!API_URL) {
        setError("NEXT_PUBLIC_API_URL ontbreekt");
        setLoading(false);
        return;
      }
      try {
        const meRes = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });
        if (!meRes.ok) {
          setError("Je bent niet ingelogd");
          setLoading(false);
          return;
        }
        const meData: MeResponse = await meRes.json();
        setMe(meData);

        if (meData.role !== "ADMIN" && meData.role !== "BIBLIOTHEEKBEHEERDER") {
          setError("Je hebt geen toegang tot deze pagina");
          setLoading(false);
          return;
        }

        if (meData.role === "ADMIN") {
          const schoolRes = await fetch(`${API_URL}/admin/schools`, {
            credentials: "include",
          });
          if (schoolRes.ok) {
            const all: (ApprovedSchool & { adminApproved: boolean })[] =
              await schoolRes.json();
            const approved = all.filter((s) => s.adminApproved);
            setApprovedSchools(approved);
            if (approved.length > 0) setSelectedSchoolId(approved[0].id);
          }
          setLoading(false);
          return;
        }

        setLoading(false);
      } catch {
        setError("Er ging iets mis met het laden");
        setLoading(false);
      }
    };
    init();
  }, []);

  useEffect(() => {
    if (!me) return;
    if (me.role === "ADMIN" && selectedSchoolId === null) return;

    const load = async () => {
      setLoading(true);
      try {
        const params = new URLSearchParams();
        params.append("page", String(currentPage - 1));
        params.append("size", String(pageSize));
        if (searchQuery) params.append("name", searchQuery);
        if (me.role === "ADMIN" && selectedSchoolId !== null)
          params.append("schoolId", String(selectedSchoolId));

        const userRes = await fetch(
          `${API_URL}/admin/users?${params.toString()}`,
          { credentials: "include" },
        );

        if (!userRes.ok) {
          setError("Kon gebruikers niet ophalen");
          return;
        }

        const userData = await userRes.json();
        setUsers(userData.content);
        setTotalPages(userData.totalPages);
        setTotalElements(userData.totalElements);

        const nextSelectedRoles: Record<number, UserRole> = {};
        userData.content.forEach((user: AdminUser) => {
          nextSelectedRoles[user.id] = user.role;
        });
        setSelectedRoles(nextSelectedRoles);

        const uniqueUids = Array.from(
          new Set(
            userData.content
              .map((user: AdminUser) => user.smartschoolUid?.trim())
              .filter((uid?: string): uid is string => !!uid && uid !== ""),
          ),
        );

        if (uniqueUids.length > 0) {
          try {
            const displayNamesRes = await fetch(
              `${API_URL}/users/display-names`,
              {
                method: "POST",
                credentials: "include",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ uids: uniqueUids }),
              },
            );
            if (displayNamesRes.ok) {
              const displayNamesData: DisplayNamesResponse =
                await displayNamesRes.json();
              setDisplayNames(displayNamesData.displayNames ?? {});
            }
          } catch (e) {
            console.error("Display names ophalen mislukt", e);
          }
        }
      } catch {
        setError("Er ging iets mis met het laden");
      } finally {
        setLoading(false);
      }
    };

    const timer = setTimeout(load, 300);
    return () => clearTimeout(timer);
  }, [me, selectedSchoolId, currentPage, pageSize, searchQuery]);

  const handleSave = async (userId: number) => {
    if (!API_URL) return;

    const role = selectedRoles[userId];
    if (!role) return;

    try {
      setSavingUserId(userId);
      setError("");
      setSucces("");

      // Admin must pass schoolId as a query param (no school on their session)
      const roleParams = new URLSearchParams();
      if (me?.role === "ADMIN" && selectedSchoolId !== null)
        roleParams.append("schoolId", String(selectedSchoolId));

      const response = await fetch(
        `${API_URL}/admin/users/${userId}/role?${roleParams.toString()}`,
        {
          method: "PATCH",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ role }),
        },
      );

      if (!response.ok) {
        setError("Rol aanpassen mislukt");
        return;
      }

      const updatedUser: AdminUser = await response.json();

      setUsers((prev) =>
        prev.map((user) => (user.id === updatedUser.id ? updatedUser : user)),
      );

      setSelectedRoles((prev) => ({
        ...prev,
        [updatedUser.id]: updatedUser.role,
      }));

      setSucces(
        `Rol van ${
          displayNames[updatedUser.smartschoolUid]
            ? displayNames[updatedUser.smartschoolUid]
            : updatedUser.smartschoolUid
        } aangepast`,
      );
    } catch (e) {
      setError("Er ging iets mis bij het opslaan");
      console.error(e);
    } finally {
      setSavingUserId(null);
    }
  };

  const handleSync = async () => {
    setSyncStatus("loading");
    setSyncStep(0);

    const stepInterval = setInterval(() => {
      setSyncStep((prev) => (prev < SYNC_STEPS.length - 1 ? prev + 1 : prev));
    }, 4000);

    try {
      const res = await fetch(`${API_URL}/api/sync`, {
        method: "POST",
        credentials: "include",
      });
      if (!res.ok) throw new Error("Sync mislukt");
      const data: SyncSummary = await res.json();
      setSyncResult(data);
    } catch {
      setSyncResult(null);
    } finally {
      clearInterval(stepInterval);
      setSyncStatus("done");
    }
  };

  if (loading) {
    return <div>Gebruikers laden...</div>;
  }

  return (
    <main>
      {error && <p className="adminMessage adminMessageError">{error}</p>}
      {succes && <p className="adminMessage adminMessageSuccess">{succes}</p>}

      {!error &&
        (me?.role === "ADMIN" || me?.role === "BIBLIOTHEEKBEHEERDER") && (
          <div id="userMain">
            <div className="adminPageHeader">
              <div>
                {/* Bibbeheerder sees their own school name; admin picks from a dropdown */}
                {me?.role === "ADMIN" ? (
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.75rem",
                    }}
                  >
                    <h1>Gebruikersbeheer</h1>
                    <select
                      className="pageSizeSelect"
                      value={selectedSchoolId ?? ""}
                      onChange={(e) => {
                        setSelectedSchoolId(Number(e.target.value));
                        setCurrentPage(1);
                      }}
                    >
                      {approvedSchools.map((s) => (
                        <option key={s.id} value={s.id}>
                          {s.name}
                        </option>
                      ))}
                    </select>
                  </div>
                ) : (
                  <h1>Gebruikersbeheer {me?.school?.name}</h1>
                )}
              </div>
              {me?.role === "ADMIN" && (
                <div style={{ display: "flex", gap: "0.75rem" }}>
                  <button
                    className="adminPrimaryButton"
                    onClick={() => setSyncStatus("confirm")}
                  >
                    Synchroniseer
                  </button>
                  <Link
                    href="/admin/school-integration"
                    className="adminPrimaryLink"
                  >
                    <button className="adminPrimaryButton">
                      Schoolintegratie
                    </button>
                  </Link>
                </div>
              )}
            </div>

            <div className="adminSearchbar">
              <input
                type="text"
                placeholder="Zoek op naam"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setCurrentPage(1);
                }}
              />
              <button id="searchButton" aria-label="Zoeken">
                🔎︎
              </button>
            </div>

            <div className="bookListToolbar">
              <span className="resultCount">{totalElements} gebruikers</span>
              <div className="pageSizeSelector">
                <span className="pageSizeLabel">Per pagina:</span>
                <select
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setCurrentPage(1);
                  }}
                  className="pageSizeSelect"
                >
                  <option value={10}>10</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                </select>
              </div>
            </div>

            <table className="adminTable">
              <thead>
                <tr>
                  <th className="adminHeader">Gebruiker</th>
                  <th className="fullScreen adminHeader">Rol</th>
                  <th className="fullScreen adminHeader">Klassen</th>
                  <th className="adminHeader">Nieuwe rol</th>
                  <th className="adminHeader"></th>
                </tr>
              </thead>

              <tbody>
                {users.map((user) => {
                  const selectedRole = selectedRoles[user.id] ?? user.role;
                  const changed = selectedRole !== user.role;
                  const resolvedName = displayNames[user.smartschoolUid];

                  return (
                    <tr key={user.id}>
                      <td className="adminCell uidCell">
                        {resolvedName ? (
                          <div>
                            <div>{resolvedName}</div>
                          </div>
                        ) : (
                          user.smartschoolUid
                        )}
                      </td>

                      <td className="fullScreen adminCell">
                        {replaceRoleName(user.role)}
                      </td>

                      <td className="fullScreen adminCell">
                        {user.classes.length === 0
                          ? "-"
                          : user.classes.map((c) => c.name).join(", ")}
                      </td>

                      <td className="adminCell">
                        <select
                          value={selectedRole}
                          onChange={(e) => {
                            setSelectedRoles((prev) => ({
                              ...prev,
                              [user.id]: e.target.value as UserRole,
                            }));
                          }}
                        >
                          {ROLE_OPTIONS.map((role) => (
                            <option key={role.value} value={role.value}>
                              {role.label}
                            </option>
                          ))}
                        </select>
                      </td>

                      <td className="saveButton adminCell">
                        <button
                          className={`adminTableButton saveRoleButton ${
                            changed ? "" : "saveRoleButtonHidden"
                          }`}
                          onClick={() => handleSave(user.id)}
                          disabled={!changed || savingUserId === user.id}
                          tabIndex={changed ? 0 : -1}
                        >
                          {savingUserId === user.id ? "Opslaan..." : "Opslaan"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </div>
        )}
      <SyncModal
        syncStatus={syncStatus}
        syncStep={syncStep}
        syncResult={syncResult}
        syncSteps={SYNC_STEPS}
        onConfirm={handleSync}
        onClose={() => {
          setSyncStatus("idle");
          setSyncResult(null);
        }}
      />
    </main>
  );
}
