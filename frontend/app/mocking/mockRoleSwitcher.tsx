"use client";

import { useState } from "react";
import { useAuth } from "@/app/context/AuthContext";
import "./mockRoleSwitcher.css";

const roles = ["Leerling", "Leerkracht", "Bibliotheekbeheerder", "admin"];

export default function MockRoleSwitcher() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
  const isDevEnvironment =
    apiUrl.includes("localhost") || apiUrl.includes("gosmartbib.tech");
  const { user } = useAuth();

  const [open, setOpen] = useState(false);

  if (!isDevEnvironment || !user) return null;

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
          {roles.map((role) => (
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
