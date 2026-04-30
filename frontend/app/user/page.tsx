"use client";

import "./userHome.css";

const widgets = [
  {
    iconSrc: "/book-icon.png",
    iconAlt: "Leeslijsten",
    title: "Leeslijsten",
    description: "Bekijk en beheer je persoonlijke leeslijsten.",
    href: "/reading-lists",
  },
  {
    iconSrc: "/checl.png",
    iconAlt: "Actieve leningen",
    title: "Actieve leningen",
    description: "Overzicht van boeken die je momenteel hebt geleend.",
    href: "/loans",
  },
  {
    iconSrc: "/user.png",
    iconAlt: "Account info",
    title: "Account info",
    description: "Bekijk en bewerk je profielgegevens.",
    href: "/account",
  },
  {
    iconSrc: "/history.png",
    iconAlt: "Leenhistoriek",
    title: "Leenhistoriek",
    description: "Alle boeken die je in het verleden hebt geleend.",
    href: "/loan-history",
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
          <div key={w.href} className="widget-card">
            <div className="widget-icon">
              <img
                className="widget-icon-img"
                src={w.iconSrc}
                alt={w.iconAlt}
              />
            </div>
            <p className="widget-title">{w.title}</p>
            <p className="widget-description">{w.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
