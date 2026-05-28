"use client";

import { useState } from "react";
import { useAuth } from "@/app/context/AuthContext";
import "./mockRoleSwitcher.css";

const roles: { label: string; value: string }[] = [
  { label: "Leerling", value: "leerling" },
  { label: "Leerkracht", value: "leerkracht" },
  { label: "Bibliotheekbeheerder", value: "librarian" },
];

export default function MockRoleSwitcher() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL ?? "";
  const isDevEnvironment = apiUrl.includes("localhost");
  const { user } = useAuth();

  const [open, setOpen] = useState(false);

  if (!isDevEnvironment || !user) return null;

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
