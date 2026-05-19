"use client";

import Link from "next/link";
import { useAuth } from "../context/AuthContext";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import "./admin.css";

const teacherWidgets = [
  {
    iconSrc: "/smartschool/Module iconen/enquete_512x512.png",
    iconAlt: "Reviewmoderatie",
    title: "Reviewmoderatie",
    description:
      "Modereer gebruikersreviews en beheer de zichtbaarheid van inhoud.",
    href: "/admin/reviews",
  },
  {
    iconSrc: "/admin/buy.png",
    iconAlt: "Aankoopsuggesties",
    title: "Aankoopsuggesties",
    description:
      "Bekijk suggesties voor nieuwe boeken op basis van gebruikersfeedback",
    href: "/admin/purchase-request",
  },
  {
    iconSrc: "/smartschool/Module iconen/analytics_512x512.png",
    iconAlt: "Statistieken",
    title: "Statistieken",
    description:
      "Bekijk statistieken over populaire boeken en gebruikersactiviteit",
    href: "/admin/statistics",
  },
];

const beheerderWidgets = [
    {
    iconSrc: "/admin/settings.png",
    iconAlt: "Gebruikersbeheer",
    title: "Gebruikersbeheer",
    description: "Beheer gebruikers en hun rollen binnen het systeem",
    href: "/admin/users",
  },
  {
    iconSrc: "/save.png",
    iconAlt: "InDeKijker",
    title: "In de kijker",
    description:
      "Beheer welke boeken in de bibliotheek in de kijker staan en promoot bepaalde titels",
    href: "/admin/spotlight",
  },
  {
    iconSrc: "/admin/settings.png",
    iconAlt: "Bibliotheekinstellingen",
    title: "Bibliotheekinstellingen",
    description: "Beheer uitleentermijnen en algemene bibliotheekinstellingen",
    href: "/admin/librarySettings",
  },
  {
    iconSrc: "/book-icon.png",
    iconAlt: "Catalogusbeheer",
    title: "Catalogusbeheer",
    description:
      "Voeg nieuwe boeken toe, pas bestaande boeken aan of verwijder oude",
    href: "/admin/manageCatalog",
  },
  {
    iconSrc: "/book-closed.png",
    iconAlt: "Uitleningen",
    title: "Uitleningen",
    description:
      "Beheer hier nieuwe uitleningen en terugbrengingen van boeken in de bibliotheek",
    href: "/admin/loan-return",
  },
  {
    iconSrc: "/checl.png",
    iconAlt: "Verlengingsaanvragen",
    title: "Verlengingsaanvragen",
    description:
      "Keur aanvragen van leerlingen en leerkrachten goed of wijs ze af",
    href: "/admin/loan-extensions",
  },
];

const adminWidgets = [
  {
    iconSrc: "/admin/settings.png",
    iconAlt: "Gebruikersbeheer",
    title: "Gebruikersbeheer",
    description: "Beheer gebruikers en hun rollen binnen het systeem",
    href: "/admin/users",
  },
  {
    iconSrc: "/admin/settings.png",
    iconAlt: "Schoolintegratie",
    title: "Schoolintegratie",
    description: "Voeg scholen toe, keur wachtende scholen goed en beheer schoolintegraties",
    href: "/admin/schools",
  },
];

export default function AdminHome() {
  const { user } = useAuth();

  function getWidgets() {
    if (user?.role === "ADMIN") {
      return adminWidgets;
    } else if (user?.role === "BIBLIOTHEEKBEHEERDER") {
      return [...beheerderWidgets, ...teacherWidgets];
    } else if (user?.role === "TEACHER") {
      return teacherWidgets;
    } else {
      return [];
    }
  }

  const widgets = getWidgets();

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "BIBLIOTHEEKBEHEERDER", "ADMIN"]}>
      <div className="admin-page">
        <div className="admin-header">
          <h2 className="admin-header-title">Beheer</h2>
          <p className="admin-header-subtitle">
            Welkom bij het beheerdersdashboard. Hier kun je alle aspecten van de
            bibliotheek beheren, van de catalogus tot gebruikersreviews en
            uitleenbeheer. Kies een van de onderstaande opties om aan de slag te
            gaan.
          </p>
        </div>

        <div className="admin-widget-grid">
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
    </ProtectedRoute>
  );
}
