"use client";

import Link from "next/link";
import { useAuth } from "../context/AuthContext";
import { useState } from "react";
import "./Navbar.css";

export default function Navbar() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);

  return (
    <header className="main-header">
      <div className="header-inner">
        {/* LOGO LINKS */}
        <Link href="/" className="logo-container">
          <img
            src="https://helpdesk.go-antwerpen.be/logo.php"
            className="goLogo"
            alt="GO! Antwerpen logo"
          />
        </Link>

        {/* NAVIGATIE RECHTS */}
        <nav id="headerNav">
          <button className="burger-menu" onClick={() => setIsOpen(!isOpen)}>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="24"
              height="24"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>

          <div className={`nav-links ${isOpen ? "open" : ""}`}>
            <Link href="/">
              <button onClick={() => setIsOpen(false)}>Startpagina</button>
            </Link>
            <Link href="/catalog">
              <button onClick={() => setIsOpen(false)}>Catalogus</button>
            </Link>
            <Link href="/spotlight">
              <button onClick={() => setIsOpen(false)}>In de kijker</button>
            </Link>
            <Link href="/lending">
              <button onClick={() => setIsOpen(false)}>Mijn uitleningen</button>
            </Link>
            <Link href="/reading-lists">
              <button onClick={() => setIsOpen(false)}>Mijn leeslijst</button>
            </Link>

            {user?.role === "ADMIN" && (
              <>
                <Link href="/admin/users">
                  <button onClick={() => setIsOpen(false)}>Gebruikers</button>
                </Link>
                <Link href="/manageCatalog">
                  <button onClick={() => setIsOpen(false)}>Boeken</button>
                </Link>
              </>
            )}
          </div>
        </nav>
      </div>
    </header>
  );
}
