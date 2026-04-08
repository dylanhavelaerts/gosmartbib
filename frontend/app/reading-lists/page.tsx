"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../context/AuthContext";
import ProtectedRoute from "../components/ProtectedRoute";
import "./readingLists.css";

type ListType = "CLASS" | "PERSONAL";

interface ReadingListOverview {
  id: number;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  listType: ListType;
  archived: boolean;
  ownList: boolean;
  creatorName?: string | null;
  bookIds: number[];
  bookCount?: number;
}

const STAFF_ROLES = ["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"];

export default function ReadingListsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const userRole = user?.role || "";
  const isStaff = STAFF_ROLES.includes(userRole);

  const [lists, setLists] = useState<ReadingListOverview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<"all" | "active" | "archived">("all");
  const [searchQuery, setSearchQuery] = useState("");
  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);

  const fetchLists = () => {
    setLoading(true);
    setError(null);

    fetch(`${apiUrl}/reading-lists`, { credentials: "include" })
      .then((res) => {
        if (!res.ok) throw new Error("Kon leeslijsten niet laden.");
        return res.json();
      })
      .then((data: ReadingListOverview[]) => {
        setLists(Array.isArray(data) ? data : []);
      })
      .catch((err) => {
        console.error(err);
        setError("Er ging iets mis bij het laden van de leeslijsten.");
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    if (user) fetchLists();
  }, [user]);

  const handleArchive = async (id: number) => {
    setActionLoading(id);
    try {
      const res = await fetch(`${apiUrl}/reading-lists/class/${id}/archive`, {
        method: "PATCH",
        credentials: "include",
      });
      if (!res.ok) throw new Error();
      fetchLists();
    } catch {
      alert("Archiveren mislukt.");
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeletePersonal = async (id: number) => {
    setActionLoading(id);
    try {
      const res = await fetch(`${apiUrl}/reading-lists/personal/${id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!res.ok) throw new Error();
      setLists((prev) => prev.filter((l) => l.id !== id));
    } catch {
      alert("Verwijderen mislukt.");
    } finally {
      setActionLoading(null);
      setDeleteConfirm(null);
    }
  };

  const filtered = lists.filter((list) => {
    const matchesFilter =
      filter === "all" ||
      (filter === "active" && !list.archived) ||
      (filter === "archived" && list.archived);

    const q = searchQuery.toLowerCase();
    const matchesSearch =
      !q ||
      list.title?.toLowerCase().includes(q) ||
      list.taskDescription?.toLowerCase().includes(q) ||
      (list.creatorName || "").toLowerCase().includes(q);

    return matchesFilter && matchesSearch;
  });

  const formatDeadline = (deadline?: string | null) => {
    if (!deadline) return null;
    const date = new Date(deadline);
    const now = new Date();
    const diffDays = Math.ceil(
      (date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24),
    );
    const label = date.toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
    if (diffDays < 0)
      return { label: `Verlopen: ${label}`, urgency: "overdue" as const };
    if (diffDays <= 7)
      return { label: `Binnenkort: ${label}`, urgency: "soon" as const };
    return { label, urgency: "normal" as const };
  };

  return (
    <div className="rl-page">
      <div className="rl-header">
        <div className="rl-header-text">
          <h1>Leeslijsten</h1>
          <p>Je eigen persoonlijke lijsten en klaslijsten in één overzicht.</p>
        </div>

        <div className="rl-header-actions">
          {isStaff && (
            <button
              className="rl-btn-primary"
              onClick={() => router.push("/reading-lists/create")}
            >
              Nieuwe klaslijst
            </button>
          )}
          <button
            className="rl-btn-primary"
            onClick={() => router.push("/reading-lists/personal")}
          >
            Mijn persoonlijke lijst
          </button>
        </div>
      </div>

      <div className="rl-toolbar">
        <div className="rl-search-wrap">
          <input
            type="text"
            className="rl-search"
            placeholder="Zoeken op titel, beschrijving of maker..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="rl-filter-tabs">
          {(["all", "active", "archived"] as const).map((f) => (
            <button
              key={f}
              className={`rl-filter-tab ${filter === f ? "active" : ""}`}
              onClick={() => setFilter(f)}
            >
              {f === "all" ? "Alle" : f === "active" ? "Actief" : "Archief"}
            </button>
          ))}
        </div>
      </div>

      {loading && (
        <div className="rl-state-container">
          <div className="rl-spinner" />
          <p>Leeslijsten laden...</p>
        </div>
      )}

      {!loading && error && (
        <div className="rl-state-container rl-error">
          <p>{error}</p>
          <button className="rl-btn-secondary" onClick={fetchLists}>
            Opnieuw proberen
          </button>
        </div>
      )}

      {!loading && !error && filtered.length === 0 && (
        <div className="rl-state-container rl-empty">
          <p>
            {searchQuery
              ? "Geen leeslijsten gevonden voor je zoekopdracht."
              : "Er zijn nog geen leeslijsten beschikbaar."}
          </p>
        </div>
      )}

      {!loading && !error && filtered.length > 0 && (
        <div className="rl-grid">
          {filtered.map((list) => {
            const deadline = formatDeadline(list.deadline);
            const bookCount = list.bookIds?.length ?? list.bookCount ?? 0;
            const isClass = list.listType === "CLASS";
            const canEditPersonal =
              list.listType === "PERSONAL" && list.ownList;
            const canArchiveClass = isStaff && isClass && !list.archived;

            return (
              <div
                key={list.id}
                className={`rl-card ${list.archived ? "rl-card--archived" : ""} ${
                  isClass ? "rl-card--class" : "rl-card--personal"
                }`}
              >
                <div className="rl-card-badge">
                  <span
                    className={`badge ${isClass ? "badge--class" : "badge--personal"}`}
                  >
                    {isClass ? "Klas lijst" : "Eigen lijst"}
                  </span>
                  {list.archived && (
                    <span className="badge badge--archived">Archief</span>
                  )}
                </div>

                <h2 className="rl-card-title">{list.title}</h2>
                {list.taskDescription && (
                  <p className="rl-card-desc">{list.taskDescription}</p>
                )}

                <div className="rl-card-meta">
                  <span className="rl-meta-item">
                    {bookCount} {bookCount === 1 ? "boek" : "boeken"}
                  </span>
                  {list.creatorName && (
                    <span className="rl-meta-item">
                      Maker: {list.creatorName}
                    </span>
                  )}
                </div>

                {deadline && (
                  <div
                    className={`rl-deadline rl-deadline--${deadline.urgency}`}
                  >
                    {deadline.label}
                  </div>
                )}

                <div className="rl-card-actions">
                  <button
                    className="rl-btn-outline"
                    onClick={() => router.push(`/reading-lists/${list.id}`)}
                  >
                    Bekijken
                  </button>

                  {canEditPersonal && (
                    <button
                      className="rl-btn-outline"
                      onClick={() =>
                        router.push(`/reading-lists/personal?edit=${list.id}`)
                      }
                    >
                      Bewerken
                    </button>
                  )}

                  {canArchiveClass && (
                    <button
                      className="rl-btn-outline"
                      disabled={actionLoading === list.id}
                      onClick={() => handleArchive(list.id)}
                    >
                      {actionLoading === list.id ? "Bezig..." : "Archiveren"}
                    </button>
                  )}

                  {canEditPersonal &&
                    (deleteConfirm === list.id ? (
                      <div className="rl-delete-confirm">
                        <span>Zeker verwijderen?</span>
                        <button
                          className="rl-btn-danger"
                          disabled={actionLoading === list.id}
                          onClick={() => handleDeletePersonal(list.id)}
                        >
                          {actionLoading === list.id ? "Bezig..." : "Ja"}
                        </button>
                        <button
                          className="rl-btn-ghost"
                          onClick={() => setDeleteConfirm(null)}
                        >
                          Nee
                        </button>
                      </div>
                    ) : (
                      <button
                        className="rl-btn-ghost rl-btn-ghost--danger"
                        onClick={() => setDeleteConfirm(list.id)}
                      >
                        Verwijderen
                      </button>
                    ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {!loading && !error && lists.length > 0 && (
        <div className="rl-footer">
          {filtered.length} van {lists.length}{" "}
          {lists.length === 1 ? "leeslijst" : "leeslijsten"} weergegeven
        </div>
      )}
    </div>
  );
}
