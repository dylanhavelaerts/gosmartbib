"use client";

import Link from "next/link";
import MockRoleSwitcher from "./mocking/mockRoleSwitcher";
import { useAuth } from "./context/AuthContext";

export default function LayoutShell({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user } = useAuth();

  return (
    <>
      <header>
        <Link href={"/"}>
          <img
            src="https://helpdesk.go-antwerpen.be/logo.php"
            className="goLogo"
            alt="GO! Antwerpen logo"
          />
        </Link>
      </header>

      <nav id="headerNav">
        <Link href="/">
          <button>Startpagina</button>
        </Link>

        <Link href="/catalog">
          <button>Catalogus</button>
        </Link>

        <Link href="/spotlight">
          <button>In de kijker</button>
        </Link>

        <Link href="/">
          <button>Mijn uitleningen</button>
        </Link>

        <Link href="/">
          <button>Mijn leeslijst & favorieten</button>
        </Link>

        {user?.role === "ADMIN" && (
          <Link href="/admin/users">
            <button>Gebruikersbeheer</button>
          </Link>
        )}

        <MockRoleSwitcher />
      </nav>

      <div id="backgroundImage"></div>

      {children}
    </>
  );
}