"use client";

import { useState } from "react";
import { useAuth } from "@/app/context/AuthContext";
import "./mockRoleSwitcher.css";

const roles: { label: string; value: string }[] = [
  { label: "Leerling", value: "leerling" },
  { label: "Leerkracht", value: "leerkracht" },
  { label: "Bibliotheekbeheerder", value: "librarian" },
];

/**
 * Developmentcomponent om lokaal snel tussen mockrollen te wisselen.
 *
 * Deze component is alleen bedoeld voor development/testing. Hij roept het
 * backend mock-role endpoint aan, waarna de pagina herlaadt en AuthContext de
 * nieuwe mockgebruiker via /auth/me ophaalt.
 */

export default function MockRoleSwitcher() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
  const isDevEnvironment = apiUrl.includes("localhost");
  const { user } = useAuth();

  const [open, setOpen] = useState(false);

  if (!isDevEnvironment || !user) return null;

  /**
   * Wisselt de actieve mockrol in de lokale backend.
   *
   * Na de POST-call wordt de pagina herladen zodat alle role-based UI opnieuw
   * berekend wordt op basis van de nieuwe /auth/me response.
   *
   * @param role de gewenste mockrol, bijvoorbeeld leerling of bibliotheekbeheerder
   */

  const switchRole = async (role: string) => {
    await fetch(`${apiUrl}/auth/mock-role/${role}`, {
      method: "POST",
      credentials: "include",
    });
    setOpen(false);
    window.location.reload();
  };

  return (
    <div className="mockRoleSwitcher">
      <button className="mockRoleSwitcherButton" onClick={() => setOpen(!open)}>
        Dev: Switch rol
      </button>
      {open && (
        <div className="mockRoleDropdown">
          {roles.map((role) => (
            <div
              key={role.value}
              className="mockRoleOption"
              onClick={() => switchRole(role.value)}
            >
              {role.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
