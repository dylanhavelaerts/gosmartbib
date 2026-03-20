"use client";

import { useState } from "react";
import "./MockRoleSwitcher.css";

const roles = ["Leerling", "Leerkracht", "Bibliotheekbeheerder"];

export default function MockRoleSwitcher() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
  const isLocal = apiUrl.includes("localhost");

  const [open, setOpen] = useState(false);

  if (!isLocal) return null;

  const switchRole = async (role: string) => {
    await fetch(`${apiUrl}/auth/mock-role/${role.toLowerCase()}`, {
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
          {roles.map(role => (
            <div
              key={role}
              className="mockRoleOption"
              onClick={() => switchRole(role)}
            >
              {role}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}