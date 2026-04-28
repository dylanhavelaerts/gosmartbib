"use client";

import type { MeResponse, AdminUser, UserRole } from "@/app/interfaces/user";
import { useEffect, useState } from "react";
import Link from "next/link";
import "./userAdmin.css";
import Pagination from "@/app/catalog/pagination";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "STUDENT", label: "LEERLING" },
  { value: "TEACHER", label: "LEERKRACHT" },
  { value: "BIBLIOTHEEKBEHEERDER", label: "BEHEERDER" },
  { value: "ADMIN", label: "ADMINISTRATOR" },
];

type DisplayNamesResponse = {
  success: boolean;
  requestedCount: number;
  resolvedCount: number;
  displayNames: Record<string, string>;
  unresolvedUids: string[];
  message: string;
};

const replaceRoleName = (role: string): string => {
  switch (role) {
    case "BIBLIOTHEEKBEHEERDER":
      return "BEHEERDER";
    case "TEACHER":
      return "LEERKRACHT";
    case "STUDENT":
      return "LEERLING";
    case "ADMIN":
      return "ADMINISTRATOR";
    default:
      return "-";
  }
};

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

  // Paging
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    const load = async () => {
      if (!API_URL) {
        setError("NEXT_PUBLIC_API_URL ontbreekt");
        setLoading(false);
        return;
      }

      try {
        const meResponse = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          setError("Je bent niet ingelogd");
          setLoading(false);
          return;
        }

        const meData: MeResponse = await meResponse.json();
        setMe(meData);

        if (meData.role !== "ADMIN") {
          setError("Je hebt geen toegang tot deze pagina");
          setLoading(false);
          return;
        }

        const params = new URLSearchParams();
        params.append("page", String(currentPage - 1));
        params.append("size", String(pageSize));
        if (searchQuery) {
          params.append("name", searchQuery);
        }

        const userResponse = await fetch(
          `${API_URL}/admin/users?${params.toString()}`,
          {
            credentials: "include",
          },
        );

        if (!userResponse.ok) {
          setError("Kon gebruikers niet ophalen");
          setLoading(false);
          return;
        }

        const userData = await userResponse.json();
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
            const displayNamesResponse = await fetch(
              `${API_URL}/users/display-names`,
              {
                method: "POST",
                credentials: "include",
                headers: {
                  "Content-Type": "application/json",
                },
                body: JSON.stringify({ uids: uniqueUids }),
              },
            );

            if (displayNamesResponse.ok) {
              const displayNamesData: DisplayNamesResponse =
                await displayNamesResponse.json();
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

    const timer = setTimeout(() => {
      load();
    }, 300);

    return () => clearTimeout(timer);
  }, [currentPage, pageSize, searchQuery]);

  const handleSave = async (userId: number) => {
    if (!API_URL) return;

    const role = selectedRoles[userId];
    if (!role) return;

    try {
      setSavingUserId(userId);
      setError("");
      setSucces("");

      const response = await fetch(`${API_URL}/admin/users/${userId}/role`, {
        method: "PATCH",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ role }),
      });

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

  if (loading) {
    return <div>Gebruikers laden...</div>;
  }

  return (
    <main>
      {error && <p className="adminMessage adminMessageError">{error}</p>}
      {succes && <p className="adminMessage adminMessageSuccess">{succes}</p>}

      {!error && me?.role === "ADMIN" && (
        <div id="userMain">
          <div className="adminPageHeader">
            <h1>Gebruikersbeheer {me?.school?.name}</h1>

            <Link href="/admin/school-integration" className="adminPrimaryLink">
              <button className="adminPrimaryButton">Schoolintegratie</button>
            </Link>
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

                    {!changed && <td className="adminCell"></td>}

                    {changed && (
                      <td className="saveButton adminCell">
                        <button
                          className="adminTableButton"
                          onClick={() => {
                            handleSave(user.id);
                          }}
                        >
                          {savingUserId === user.id ? "Opslaan..." : "Opslaan"}
                        </button>
                      </td>
                    )}
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
    </main>
  );
}
