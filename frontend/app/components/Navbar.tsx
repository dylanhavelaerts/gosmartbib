"use client";

import Link from "next/link";
import { useAuth } from "../context/AuthContext";
import { useState } from "react";
import "./Navbar.css";
import { usePathname } from "next/navigation";

export default function Navbar() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const pathname = usePathname();

  const isActive = (href: string) => pathname === href;

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
              <button
                onClick={() => setIsOpen(false)}
                className={isActive("/") ? "active" : ""}
              >
                Startpagina
              </button>
            </Link>
            <Link href="/catalog">
              <button
                onClick={() => setIsOpen(false)}
                className={isActive("/catalog") ? "active" : ""}
              >
                Catalogus
              </button>
            </Link>
            {user?.role !== "ADMIN" && (
              <>
                <Link href="/leaderboard">
                  <button
                    onClick={() => setIsOpen(false)}
                    className={isActive("/leaderboard") ? "active" : ""}
                  >
                    Ranglijst
                  </button>
                </Link>
                <Link href="/reading-lists">
                  <button
                    onClick={() => setIsOpen(false)}
                    className={isActive("/reading-lists") ? "active" : ""}
                  >
                    Mijn leeslijsten
                  </button>
                </Link>
                <Link href="/lended-books">
                  <button
                    onClick={() => setIsOpen(false)}
                    className={isActive("/lended-books") ? "active" : ""}
                  >
                    Mijn ontleningen
                  </button>
                </Link>{" "}
              </>
            )}
            {(user?.role === "BIBLIOTHEEKBEHEERDER" ||
              user?.role === "ADMIN" ||
              user?.role === "TEACHER") && (
              <>
                <Link href="/admin">
                  <button
                    onClick={() => setIsOpen(false)}
                    className={isActive("/admin") ? "active" : ""}
                  >
                    Beheer
                  </button>
                </Link>
              </>
            )}
          </div>
          {user?.role !== "ADMIN" && (
            <Link
              href="/user"
              className={`user-icon-link${isActive("/user") ? " user-icon-active" : ""}`}
              onClick={() => setIsOpen(false)}
              title="Profiel"
            >
              <img src="/user.png" alt="Profiel" className="user-icon-img" />
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
