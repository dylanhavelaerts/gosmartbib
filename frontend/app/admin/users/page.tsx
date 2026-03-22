"use client"

import { UserRole } from "@/app/interfaces/user";
import type { MeResponse, AdminUser } from "@/app/interfaces/user";
import { useEffect, useState } from "react";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "STUDENT", label: "STUDENT" },
  { value: "TEACHER", label: "LEERKRACHT" },
  { value: "BIBLIOTHEEKBEHEERDER", label: "BIBLIOTHEEKBEHEERDER" },
];

export default function AdminUserPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [me, setMe] = useState<MeResponse | null>(null);
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [selectedRoles, setSelectedRoles] = useState<Record<number, UserRole>>({});

    useEffect(() => {
        const load = async() => {
            if(!API_URL) {
                setError("NEXT_PUBLIC_API_URL ontbreekt")
                setLoading(false);
                return
            }

            try {
                const meResponse = await fetch(`${API_URL}/auth/me`, {
                    credentials: "include"
                });

                if(!meResponse.ok) {
                    setError("Je bent niet ingelogd");
                    setLoading(false);
                    return;
                }

                const meData: MeResponse = await meResponse.json();
                setMe(meData);

                if(meData.role != "BIBLIOTHEEKBEHEERDER") {
                    setError("Je hebt geen toegang tot deze pagina");
                    setLoading(false);
                    return;
                }

                const userResponse = await fetch(`${API_URL}/admin/users`, {
                    credentials: "include"
                });

                if(!userResponse.ok) {
                    setError("Kon gebruikers niet ophalen");
                    setLoading(false);
                    return;
                }

                const userData: AdminUser[] = await userResponse.json();
                setUsers(userData);

                
            const nextSelectedRoles: Record<number, UserRole> = {};
            userData.forEach((user) => {
                nextSelectedRoles[user.id] = user.role;
            });
            setSelectedRoles(nextSelectedRoles);
            }
            catch {
                setError("Er ging iets mis met het laden");
            }
            finally {
                setLoading(false);
            }
        };

        load();
    }, []);


    if (loading) {
        return (
                <div>Gebruikers laden...</div>
        );
    }

    return (
        <main>
            {error && <p>{error}</p>}

            {!error && me?.role === "BIBLIOTHEEKBEHEERDER" && (
                <div>
                    <h1>Gebruikersbeheer voor jou school</h1>
                    <table>
                        <thead>
                            <tr>
                                <th>UID</th>
                                <th>Rol</th>
                                <th>Klassen</th>
                                <th>Nieuwe rol</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            {users.map((user) => {
                                const selectedRole = selectedRoles[user.id] ?? user.role;
                                const changed = selectedRole !== user.role;

                                return (
                                    <tr key={user.id}>
                                        <td>{user.smartschoolUid}</td>
                                        <td>{user.role}</td>
                                        <td>{user.classes.length === 0 ? "-" : user.classes.map((c) => c.name).join(", ")}</td>
                                        <td><select 
                                            value={selectedRole}
                                            onChange={(e) => {
                                                setSelectedRoles((prev) => ({
                                                    ...prev,
                                                    [user.id]: e.target.value as UserRole,
                                                }))
                                            }}
                                        >{ROLE_OPTIONS.map((role) => (
                                            <option key={role.value} value={role.value}>
                                                {role.label}
                                            </option>
                                        )) }
                                            </select>
                                            </td>
                                        <td>{changed && (
                                            <button>Opslaan</button>
                                        )}</td>
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>)}
        </main>
    )
}