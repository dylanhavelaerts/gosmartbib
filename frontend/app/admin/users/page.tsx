"use client"

import { UserRole } from "@/app/interfaces/User";
import type { MeResponse, AdminUser } from "@/app/interfaces/User";
import { useEffect, useState } from "react";
import "./userAdmin.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "STUDENT", label: "STUDENT" },
  { value: "TEACHER", label: "LEERKRACHT" },
  { value: "BIBLIOTHEEKBEHEERDER", label: "BEHEERDER" },
];

const replaceRoleName = (role: string):string => {
    switch(role){
        case "BIBLIOTHEEKBEHEERDER":
            return "BEHEERDER";
        case "TEACHER":
            return "LEERKRACHT";
        case "STUDENT":
            return "STUDENT";
        default:
            return "-";
    }
}

export default function AdminUserPage() {
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [me, setMe] = useState<MeResponse | null>(null);
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [selectedRoles, setSelectedRoles] = useState<Record<number, UserRole>>({});
    const [savingUserId, setSavingUserId] = useState<number | null>(null);
    const [succes, setSucces] = useState("");

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

        const handleSave = async (userId: number) => {
        if(!API_URL) return;

        const role = selectedRoles[userId];
        if(!role) return;

        try {
            setSavingUserId(userId);
            setError("");
            setSucces("");

            const response = await fetch(`${API_URL}/admin/users/${userId}/role`, {
                method: "PATCH",
                credentials: "include",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({role})
            });
            
            if(!response.ok) {
                setError("Rol aanpassen mislukt");
                return;
            }

            const updatedUser: AdminUser = await response.json();

            setUsers((prev) => 
                prev.map((user) => (user.id === updatedUser.id ? updatedUser : user))
            );

            setSelectedRoles((prev) => ({
                ...prev,
                [updatedUser.id]: updatedUser.role,
            }));

            setSucces(`Rol van ${updatedUser.smartschoolUid} aangepast`);
        }
        catch(e) {
            setError("Er ging iets mis bij het opslaan");
            console.error(e);
        }
        finally{
            setSavingUserId(null);
        }
    }


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
                    <h1>Gebruikersbeheer {me?.school?.name}</h1>
                    <table className="adminTable">
                        <thead>
                            <tr>
                                <th className="adminHeader">UID</th>
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

                                return (
                                    <tr key={user.id}>
                                        <td className="adminCell uidCell">{user.smartschoolUid}</td>
                                        <td className="fullScreen adminCell">{replaceRoleName(user.role)}</td>
                                        <td className="fullScreen adminCell">{user.classes.length === 0 ? "-" : user.classes.map((c) => c.name).join(", ")}</td>
                                        <td className="adminCell"><select 
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
                                            {!changed && (
                                                <td className="adminCell"></td>
                                            )}{changed && (
                                                <td className="saveButton adminCell">
                                                    <button className="adminTableButton" onClick={() => {handleSave(user.id)}}>{savingUserId === user.id ? "Opslaan..." : "Opslaan"}</button>
                                                </td>
                                        )}
                                    </tr>
                                )
                            })}
                        </tbody>
                    </table>
                </div>)}
        </main>
    )
}