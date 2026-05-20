"use client";

import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import SettingsModal, { type UserPreferences } from "./SettingsModal";
import "./userHome.css";

interface ActiveLoan {
  loanId: number;
  quantity: number;
  dueDate: string;
  book: {
    title: string;
    thumbnail: string | null;
    authors?: string[];
  } | null;
}

interface PersonalStats {
  totalBooksRead: number;
  topGenres: string[];
}

function formatDue(dueDateString: string): { text: string; cls: string } {
  const due = new Date(dueDateString);
  const now = new Date();
  due.setHours(0, 0, 0, 0);
  now.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((due.getTime() - now.getTime()) / 86_400_000);
  if (diffDays < 0)
    return { text: `${Math.abs(diffDays)}d te laat`, cls: "due-late" };
  if (diffDays <= 3) return { text: `Nog ${diffDays}d`, cls: "due-soon" };
  return { text: `Nog ${diffDays}d`, cls: "due-ok" };
}

async function logoutUser() {
  await fetch(`${process.env.NEXT_PUBLIC_API_URL}/users/logout`, {
    method: "POST",
    credentials: "include",
  });
  window.location.href = "/login";
}

function UserInfoWidget({
  user,
}: {
  user: ReturnType<typeof useAuth>["user"];
}) {
  const cls = user?.classes?.[0];

  return (
    <div className="widget widget--user-info">
      <p className="widget-label">Profiel</p>
      <div className="user-avatar">
        <img src="/user.png" alt="Gebruiker" className="user-avatar-icon" />
      </div>
      <p className="user-name">{user?.smartschoolUid ?? "—"}</p>
      {cls && (
        <div className="user-tags">
          {cls.name && <span className="user-tag">{cls.name}</span>}
          {cls.grade && <span className="user-tag">{cls.grade}</span>}
          {cls.schoolYear && <span className="user-tag">{cls.schoolYear}</span>}
        </div>
      )}
    </div>
  );
}

function BadgeWidget() {
  return (
    <div className="widget widget--badge">
      <p className="widget-label">Wat voor lezer ben jij?</p>
      <div className="badge-body">
        <div className="badge-placeholder">
          <img src="/badges/pink.png" alt="Badge" className="badge-icon-img" />
        </div>
        <div className="badge-text">
          <p className="badge-title">Avonturier</p>
          <p className="badge-desc">
            Als Avonturier verken je de bibliotheek in alle richtingen. Je duikt
            in onbekende verhalen, ontdekt nieuwe genres en geeft elk boek een
            eerlijke kans. Jouw nieuwsgierigheid is grenzeloos en elke pagina is
            een nieuw avontuur. Blijf lezen, blijf ontdekken — want de grootste
            verhalen wachten nog op jou.
          </p>
        </div>
      </div>
    </div>
  );
}

function CurrentLoansWidget({
  loans,
  loading,
}: {
  loans: ActiveLoan[];
  loading: boolean;
}) {
  const total = loans.reduce((s, l) => s + l.quantity, 0);

  return (
    <div className="widget widget--loans">
      <div className="widget-header-row">
        <p className="widget-label">Momenteel ontleend</p>
        {total > 0 && <span className="widget-count">{total}</span>}
      </div>

      {loading ? (
        <p className="widget-loading">Laden…</p>
      ) : loans.length === 0 ? (
        <p className="widget-empty">Geen actieve uitleningen</p>
      ) : (
        <div className="loans-list">
          {loans.slice(0, 4).map((loan) => {
            const due = formatDue(loan.dueDate);
            return (
              <div key={loan.loanId} className="loan-item">
                <div className="loan-cover">
                  {loan.book?.thumbnail ? (
                    <img src={loan.book.thumbnail} alt={loan.book.title} />
                  ) : (
                    <div className="loan-cover-empty" />
                  )}
                </div>
                <div className="loan-info">
                  <p className="loan-title">{loan.book?.title ?? "Onbekend"}</p>
                  <p className="loan-author">
                    {loan.book?.authors?.join(", ") ?? "Auteur onbekend"}
                  </p>
                  <span className={`loan-due loan-due--${due.cls}`}>
                    {due.text}
                  </span>
                </div>
              </div>
            );
          })}
          {loans.length > 4 && (
            <p className="loans-more">+{loans.length - 4} meer</p>
          )}
        </div>
      )}
    </div>
  );
}

function BadgesWidget() {
  return (
    <div className="widget widget--badges">
      <p className="widget-label">Badges &amp; prestaties</p>
      <div className="badges-coming-soon">
        <img src="/badges/pink.png" alt="" className="badges-preview-icon" />
        <img src="/badges/blue.png" alt="" className="badges-preview-icon" />
        <img src="/badges/yellow.png" alt="" className="badges-preview-icon" />
        <img src="/badges/green.png" alt="" className="badges-preview-icon" />
      </div>
      <p className="badges-title">Binnenkort beschikbaar</p>
      <p className="badges-desc">
        Verdien badges door boeken te lezen, genres te verkennen en
        uitleendoelen te halen. Hoe meer je leest, hoe meer je ontgrendelt.
      </p>
    </div>
  );
}

function BookStatsWidget({
  stats,
  loading,
}: {
  stats: PersonalStats | null;
  loading: boolean;
}) {
  return (
    <div className="widget widget--stats">
      <p className="widget-label">Leesstatistieken</p>

      {loading ? (
        <p className="widget-loading">Laden…</p>
      ) : (
        <>
          <div className="stats-row">
            <div className="stat-block">
              <span className="stat-value">{stats?.totalBooksRead ?? 0}</span>
              <span className="stat-label">Boeken gelezen</span>
            </div>
            <div className="stat-block">
              <span className="stat-value">{stats?.topGenres?.[0] ?? "—"}</span>
              <span className="stat-label">Favoriete genre</span>
            </div>
          </div>

          {stats?.topGenres && stats.topGenres.length > 1 && (
            <div className="genre-tags">
              {stats.topGenres.slice(1).map((g) => (
                <span key={g} className="genre-tag">
                  {g}
                </span>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

export default function UserHome() {
  const { user } = useAuth();
  const [activeLoans, setActiveLoans] = useState<ActiveLoan[]>([]);
  const [stats, setStats] = useState<PersonalStats | null>(null);
  const [loansLoading, setLoansLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [prefs, setPrefs] = useState<UserPreferences>({
    anonymousLeaderboard: false,
  });
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [savingPrefs, setSavingPrefs] = useState(false);

  useEffect(() => {
    const api = process.env.NEXT_PUBLIC_API_URL ?? "";

    fetch(`${api}/loans/active`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : []))
      .then(setActiveLoans)
      .catch(() => setActiveLoans([]))
      .finally(() => setLoansLoading(false));

    fetch(`${api}/user-stats/personal`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then(setStats)
      .catch(() => setStats(null))
      .finally(() => setStatsLoading(false));

    fetch(`${api}/user/preferences`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (data) setPrefs(data);
      })
      .catch(() => {});
  }, []);

  async function handleToggleLeaderboard(anonymous: boolean) {
    const previous = prefs;
    setPrefs({ anonymousLeaderboard: anonymous });
    setSavingPrefs(true);
    const api = process.env.NEXT_PUBLIC_API_URL ?? "";
    try {
      const res = await fetch(`${api}/user/preferences`, {
        method: "PATCH",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ anonymousLeaderboard: anonymous }),
      });
      if (!res.ok) setPrefs(previous);
    } catch {
      setPrefs(previous);
    } finally {
      setSavingPrefs(false);
    }
  }

  return (
    <div className="user-page">
      {settingsOpen && (
        <SettingsModal
          prefs={prefs}
          saving={savingPrefs}
          onToggle={handleToggleLeaderboard}
          onClose={() => setSettingsOpen(false)}
        />
      )}
      <div className="user-header">
        <h2 className="user-header-title">Mijn profiel</h2>
        <div className="user-header-actions">
          <button
            className="settings-btn"
            onClick={() => setSettingsOpen(true)}
            aria-label="Instellingen"
          >
            <img
              src="/admin/settings.png"
              alt=""
              className="settings-btn-icon"
            />
          </button>
          <button className="logout-btn" onClick={logoutUser}>
            Uitloggen
          </button>
        </div>
      </div>

      <div className="widgets-container">
        <UserInfoWidget user={user} />
        <BadgeWidget />
        <CurrentLoansWidget loans={activeLoans} loading={loansLoading} />
        <BookStatsWidget stats={stats} loading={statsLoading} />
        <BadgesWidget />
      </div>
    </div>
  );
}
