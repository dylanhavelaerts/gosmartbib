"use client";

import { useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import SettingsModal, { type UserPreferences } from "./SettingsModal";
import UserInfoWidget from "./widgets/UserInfoWidget";
import ReaderProfileWidget, {
  type ReaderProfile,
} from "./widgets/ReaderProfileWidget";
import CurrentLoansWidget, {
  type ActiveLoan,
} from "./widgets/CurrentLoansWidget";
import BookStatsWidget, { type PersonalStats } from "./widgets/BookStatsWidget";
import AchievementsWidget, {
  type Achievement,
  computeOverallTier,
} from "./widgets/AchievementsWidget";
import "./userHome.css";

const PROFILE_TINTS: Record<string, string> = {
  AVONTURIER: "#f9fffa",
  PIONIER: "#fff7f9",
  SPRINTER: "#fffff8",
  TITAN: "#f8fbff",
};

/**
 * Logt de huidige gebruiker uit.
 *
 * De backend wist de Spring Security sessie en verwijdert de sessiecookies.
 * Daarna stuurt de frontend de gebruiker terug naar de loginpagina.
 */

async function logoutUser() {
  await fetch(`${process.env.NEXT_PUBLIC_API_URL}/users/logout`, {
    method: "POST",
    credentials: "include",
  });
  window.location.href = "/login";
}

export default function UserHome() {
  const { user } = useAuth();
  const [activeLoans, setActiveLoans] = useState<ActiveLoan[]>([]);
  const [stats, setStats] = useState<PersonalStats | null>(null);
  const [profile, setProfile] = useState<ReaderProfile | null>(null);
  const [distribution, setDistribution] = useState<Record<
    string,
    number
  > | null>(null);
  const [loansLoading, setLoansLoading] = useState(true);
  const [statsLoading, setStatsLoading] = useState(true);
  const [profileLoading, setProfileLoading] = useState(true);
  const [achievements, setAchievements] = useState<Achievement[]>([]);
  const [achievementsLoading, setAchievementsLoading] = useState(true);
  const [prefs, setPrefs] = useState<UserPreferences>({
    anonymousLeaderboard: false,
  });
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [savingPrefs, setSavingPrefs] = useState(false);
  const [displayName, setDisplayName] = useState<string | null>(null);

  useEffect(() => {
    const api = process.env.NEXT_PUBLIC_API_URL ?? "";
    if (!user?.smartschoolUid) return;
    fetch(`${api}/users/display-names`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ uids: [user.smartschoolUid] }),
    })
      .then((r) => (r.ok ? r.json() : null))
      .then((data) => {
        if (data?.success && data.displayNames?.[user.smartschoolUid!]) {
          setDisplayName(data.displayNames[user.smartschoolUid!]);
        }
      })
      .catch(() => {});
  }, [user?.smartschoolUid]);

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

    fetch(`${api}/user-stats/reader-profile`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then(setProfile)
      .catch(() => setProfile(null))
      .finally(() => setProfileLoading(false));

    fetch(`${api}/user-stats/profile-distribution`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : null))
      .then(setDistribution)
      .catch(() => setDistribution(null));

    fetch(`${api}/user-stats/achievements`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : []))
      .then(setAchievements)
      .catch(() => setAchievements([]))
      .finally(() => setAchievementsLoading(false));
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

  const pageTint = profile?.profileType
    ? (PROFILE_TINTS[profile.profileType] ?? "#fff")
    : "#fff";

  const overallTier = computeOverallTier(achievements);

  return (
    <div
      className="user-page"
      style={{ background: pageTint, transition: "background 0.4s ease" }}
    >
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
        <UserInfoWidget
          user={user}
          displayName={displayName}
          overallTier={overallTier}
          profileType={profile?.profileType ?? null}
          profileLabel={profile?.profileLabel ?? null}
        />
        <ReaderProfileWidget
          profile={profile}
          distribution={distribution}
          loading={profileLoading}
        />
        <CurrentLoansWidget loans={activeLoans} loading={loansLoading} />
        <BookStatsWidget stats={stats} loading={statsLoading} />
        <AchievementsWidget
          achievements={achievements}
          loading={achievementsLoading}
        />
      </div>
    </div>
  );
}
