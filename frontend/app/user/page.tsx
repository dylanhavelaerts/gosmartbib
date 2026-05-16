"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import "./userHome.css";

const URGENT_THRESHOLD_DAYS = 5;

interface UrgentLoan {
  loanId: number;
  daysLeft: number;
  book: { title: string } | null;
}

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

function calcDaysLeft(dueDateString: string): number {
  const due = new Date(dueDateString);
  const now = new Date();
  due.setHours(0, 0, 0, 0);
  now.setHours(0, 0, 0, 0);
  return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

function formatDaysLabel(days: number): string {
  if (days < 0)
    return `${Math.abs(days)} dag${Math.abs(days) !== 1 ? "en" : ""} te laat`;
  if (days === 0) return "Vandaag inleveren";
  return `Nog ${days} dag${days !== 1 ? "en" : ""}`;
}

function badgeClass(days: number): string {
  return days < 0 ? "badge--overdue" : "badge--soon";
}

export default function UserHome() {
  const [urgentLoans, setUrgentLoans] = useState<UrgentLoan[]>([]);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/loans/active`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : []))
      .then(
        (
          loans: Array<{
            loanId: number;
            dueDate: string;
            book: { title: string } | null;
          }>,
        ) => {
          const urgent = loans
            .map((l) => ({
              loanId: l.loanId,
              book: l.book,
              daysLeft: calcDaysLeft(l.dueDate),
            }))
            .filter((l) => l.daysLeft <= URGENT_THRESHOLD_DAYS)
            .sort((a, b) => a.daysLeft - b.daysLeft);
          setUrgentLoans(urgent);
        },
      )
      .catch(() => {});
  }, []);

  return (
    <div className="user-page">
      <div className="user-header">
        <h2 className="user-header-title">Mijn account</h2>
        <button className="logout-btn" onClick={logoutUser}>
          Uitloggen
        </button>
      </div>

      {urgentLoans.length > 0 && (
        <div
          className="urgent-banner"
          role="alert"
          aria-label="Boeken die bijna ingeleverd moeten worden"
        >
          <h3 className="urgent-banner-title">Bijna in te leveren</h3>
          <ul className="urgent-list">
            {urgentLoans.map((loan) => (
              <li key={loan.loanId} className="urgent-list-item">
                <span className="urgent-book-title">
                  {loan.book?.title ?? "Onbekend boek"}
                </span>
                <span
                  className={`urgent-days-badge ${badgeClass(loan.daysLeft)}`}
                >
                  {formatDaysLabel(loan.daysLeft)}
                </span>
              </li>
            ))}
          </ul>
          <Link href="/lended-books" className="urgent-banner-link">
            Bekijk al je uitleningen →
          </Link>
        </div>
      )}

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
