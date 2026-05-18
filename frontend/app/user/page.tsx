"use client";

import Link from "next/link";
import "./userHome.css";

const widgets = [
  {
    iconSrc: "/book-icon.png",
    iconAlt: "Leeslijsten",
    title: "Leeslijsten",
    description: "Bekijk en beheer je persoonlijke leeslijsten",
    href: "/reading-lists",
  },
  {
    iconSrc: "/checl.png",
    iconAlt: "Mijn uitleningen",
    title: "Mijn uitleningen",
    description:
      "Overzicht van boeken die je momenteel en in het verleden hebt geleend",
    href: "/lended-books",
  },
  {
    iconSrc: "/user.png",
    iconAlt: "Account info",
    title: "Account info",
    description: "Bekijk en bewerk je profielgegevens",
    href: "/account",
  },
];

async function logoutUser() {
  await fetch(`${process.env.NEXT_PUBLIC_API_URL}/users/logout`, {
    method: "POST",
    credentials: "include",
  });
  window.location.href = "/login";
}

export default function UserHome() {
  return (
    <div className="user-page">
      <div className="user-header">
        <h2 className="user-header-title">Mijn account</h2>
        <button className="logout-btn" onClick={logoutUser}>
          Uitloggen
        </button>
      </div>

      <div className="widget-grid">
        {widgets.map((w) => (
          <Link key={w.href} href={w.href} className="widget-card">
            <div className="widget-icon">
              <img
                className="widget-icon-img"
                src={w.iconSrc}
                alt={w.iconAlt}
              />
            </div>
            <p className="widget-title">{w.title}</p>
            <p className="widget-description">{w.description}</p>
          </Link>
        ))}
      </div>
    </div>
  );
}
