"use client";

import { useState, useEffect, useMemo } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import "../readinglistdetail.css";
import NotificationBell from "@/app/components/Notifications/Notification";
import type {
  ReadingListDetail,
  UpdateReadingListVisibilityPayload,
} from "@/app/interfaces/ReadingList";
import { formatReadingListTargets } from "@/app/utils/readingListTargets";

const STAFF_ROLES = ["TEACHER", "ADMIN", "LIBRARIAN"];

export default function ReadingListDetailPage() {
  const { user } = useAuth();
  const router = useRouter();
  const params = useParams();

  const id = params?.id as string;
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const userRole = user?.role || "";
  const isStaff = STAFF_ROLES.includes(userRole);

  const [detail, setDetail] = useState<ReadingListDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [visibilityLoading, setVisibilityLoading] = useState(false);
  const [shareFeedback, setShareFeedback] = useState<string | null>(null);

  const userId = user?.id ?? 0;
  const storageKey = `reading-status:${userId}:${id}`;

  const [readStatus, setReadStatus] = useState<Record<number, boolean>>({});

  const hasUnavailableBooks = detail?.books.some(
    (b) => b.availableCopies === 0,
  );

  const sharedUrl = useMemo(() => {
    if (typeof window === "undefined" || !detail?.publicUid) {
      return "";
    }

    return `${window.location.origin}/reading-lists/shared/${detail.publicUid}`;
  }, [detail?.publicUid]);

  useEffect(() => {
    if (!id || !user) return;

    try {
      const raw = localStorage.getItem(storageKey);
      setReadStatus(raw ? JSON.parse(raw) : {});
    } catch {
      setReadStatus({});
    }

    setLoading(true);
    setError(null);

    fetch(`${apiUrl}/reading-lists/${id}`, { credentials: "include" })
      .then(async (res) => {
        if (!res.ok) {
          const body = await res.text();
          throw new Error(`HTTP ${res.status}: ${body}`);
        }
        return res.json();
      })
      .then((data: ReadingListDetail) => setDetail(data))
      .catch((err) => {
        console.error(err);
        setError(
          String(
            err.message || "Er ging iets mis bij het laden van de leeslijst",
          ),
        );
      })
      .finally(() => setLoading(false));
  }, [apiUrl, id, storageKey, user]);

  const toggleRead = (bookId: number) => {
    setReadStatus((prev) => {
      const next = { ...prev, [bookId]: !prev[bookId] };
      try {
        localStorage.setItem(storageKey, JSON.stringify(next));
      } catch {}
      return next;
    });
  };

  const togglePublicVisibility = async () => {
    if (!detail || !detail.ownList || detail.listType !== "PERSONAL") {
      return;
    }

    const nextPublicVisible = !Boolean(detail.publicVisible);

    const payload: UpdateReadingListVisibilityPayload = {
      publicVisible: nextPublicVisible,
    };

    setVisibilityLoading(true);
    setShareFeedback(null);

    try {
      const response = await fetch(
        `${apiUrl}/reading-lists/personal/${detail.id}/visibility`,
        {
          method: "PATCH",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        },
      );

      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || "Kon deelinstelling niet aanpassen");
      }

      const updated = await response.json();

      setDetail((current) => {
        if (!current) {
          return current;
        }

        return {
          ...current,
          publicUid: updated.publicUid ?? current.publicUid,
          publicVisible: Boolean(updated.publicVisible),
        };
      });

      setShareFeedback(
        nextPublicVisible
          ? "Deze leeslijst is nu deelbaar via de link"
          : "Deze leeslijst is weer privé",
      );
    } catch (err) {
      setShareFeedback(
        err instanceof Error
          ? err.message
          : "Kon deelinstelling niet aanpassen",
      );
    } finally {
      setVisibilityLoading(false);
    }
  };

  const removeBook = async (bookId: number) => {
    if (!detail) return;

    const endpoint =
      detail.listType === "PERSONAL"
        ? `${apiUrl}/reading-lists/personal/${detail.id}`
        : `${apiUrl}/reading-lists/class/${detail.id}`;

    const updatedBookIds = detail.books
      .filter((b) => b.id !== bookId)
      .map((b) => b.id);

    const payload = {
      title: detail.title,
      taskDescription: detail.taskDescription ?? "",
      deadline: detail.deadline ?? null,
      bookIds: updatedBookIds,
      targetType: detail.targetType ?? null,
      targetStudentIds: detail.targetStudentIds ?? [],
      targetClassIds: detail.targetClassIds ?? [],
      targetYears: detail.targetYears ?? [],
      targetGrades: detail.targetGrades ?? [],
      targetAllSchools: detail.targetAllSchools ?? false,
    };

    try {
      const res = await fetch(endpoint, {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error();
      setDetail((prev) =>
        prev
          ? { ...prev, books: prev.books.filter((b) => b.id !== bookId) }
          : prev,
      );
    } catch {
      setError("Kon het boek niet verwijderen uit de leeslijst");
    }
  };

  const copySharedUrl = async () => {
    if (!sharedUrl) {
      return;
    }

    try {
      await navigator.clipboard.writeText(sharedUrl);
      setShareFeedback("Link gekopieerd.");
    } catch {
      setShareFeedback("Kopiëren is niet gelukt. Selecteer de link handmatig.");
    }
  };

  const formatDeadline = (deadline?: string | null) => {
    if (!deadline) return null;
    const date = new Date(deadline);
    const diffDays = Math.ceil(
      (date.getTime() - Date.now()) / (1000 * 60 * 60 * 24),
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

  const readCount = detail?.books.filter((b) => readStatus[b.id]).length ?? 0;
  const totalCount = detail?.books.length ?? 0;
  const deadline = formatDeadline(detail?.deadline);
  const showTargetSummary =
    detail?.listType === "CLASS" &&
    detail.targetType !== null &&
    detail.targetType !== undefined;

  return (
    <div className="rld-page">
      {loading && (
        <div className="rld-state">
          <div className="rld-spinner" />
          <p>Laden...</p>
        </div>
      )}

      {!loading && error && (
        <div className="rld-state rld-state--error">
          <p>{error}</p>
          <button
            className="rld-btn-outline"
            onClick={() => router.push("/reading-lists")}
          >
            Terug naar overzicht
          </button>
        </div>
      )}

      {!loading && !error && detail && (
        <>
          {/* ── Header card ── */}
          <div className="rld-header-card">
            <div className="rld-header-top">
              <div className="rld-badges">
                <span className="rld-badge">
                  {detail.listType === "CLASS"
                    ? "Klasleeslijst"
                    : "Eigen leeslijst"}
                </span>
                {detail.listType === "PERSONAL" && detail.publicVisible && (
                  <span className="rld-badge rld-badge--shared">Deelbaar</span>
                )}
              </div>
              {hasUnavailableBooks && (
                <NotificationBell
                  apiPath={`/reading-lists/${id}/notification`}
                />
              )}
            </div>

            <h1>{detail.title}</h1>

            {detail.taskDescription && (
              <p className="rld-description">{detail.taskDescription}</p>
            )}

            <div className="rld-meta">
              {detail.creatorName && (
                <span className="rld-meta-item">
                  Aangemaakt door: <strong>{detail.creatorName}</strong>
                </span>
              )}
              <span className="rld-meta-item">
                <strong>{totalCount}</strong>{" "}
                {totalCount === 1 ? "boek" : "boeken"}
              </span>
            </div>

            {showTargetSummary && (
              <div className="rld-target-summary">
                {formatReadingListTargets(detail)}
              </div>
            )}

            {deadline && (
              <div className={`rld-deadline rld-deadline--${deadline.urgency}`}>
                {deadline.label}
              </div>
            )}

            {isStaff && detail.ownList && detail.listType === "CLASS" && (
              <div className="rld-header-actions">
                <button
                  className="rld-btn-outline"
                  onClick={() =>
                    router.push(`/reading-lists/class-edit/${detail.id}`)
                  }
                >
                  Bewerken
                </button>
              </div>
            )}
            {detail.ownList && detail.listType === "PERSONAL" && (
              <div className="rld-share-panel">
                <div className="rld-share-header">
                  <div>
                    <h2>Delen via link</h2>
                    <p>
                      {detail.publicVisible
                        ? "Iedere ingelogde gebruiker met deze link kan deze leeslijst bekijken"
                        : "Deze persoonlijke leeslijst is momenteel alleen zichtbaar voor jou"}
                    </p>
                  </div>

                  <button
                    className={
                      detail.publicVisible
                        ? "rld-btn-danger"
                        : "rld-btn-primary"
                    }
                    onClick={togglePublicVisibility}
                    disabled={visibilityLoading}
                  >
                    {visibilityLoading
                      ? "Bezig..."
                      : detail.publicVisible
                        ? "Privé maken"
                        : "Deelbaar maken"}
                  </button>
                </div>

                {detail.publicVisible && sharedUrl && (
                  <div className="rld-share-url-row">
                    <input
                      value={sharedUrl}
                      readOnly
                      aria-label="Deelbare leeslijstlink"
                    />

                    <button className="rld-btn-outline" onClick={copySharedUrl}>
                      Kopiëren
                    </button>

                    <button
                      className="rld-btn-outline"
                      onClick={() =>
                        router.push(`/reading-lists/shared/${detail.publicUid}`)
                      }
                    >
                      Openen
                    </button>
                  </div>
                )}

                {shareFeedback && (
                  <p className="rld-share-feedback">{shareFeedback}</p>
                )}
              </div>
            )}
          </div>

          {/* ── Progress bar ── */}
          {detail.listType === "CLASS" && totalCount > 0 && (
            <div className="rld-progress-wrap">
              <div className="rld-progress-label">
                Voortgang: {readCount} / {totalCount} gelezen
              </div>
              <div className="rld-progress-track">
                <div
                  className="rld-progress-fill"
                  style={{ width: `${(readCount / totalCount) * 100}%` }}
                />
              </div>
            </div>
          )}

          {/* ── Books ── */}
          {totalCount === 0 ? (
            <div className="rld-state rld-state--empty">
              <p>Deze leeslijst bevat nog geen boeken</p>
            </div>
          ) : (
            <div className="rld-book-grid">
              {detail.books.map((book) => {
                const isRead = readStatus[book.id] ?? false;
                const isUnavailable = book.availableCopies === 0;
                return (
                  <div
                    key={book.id}
                    className={`rld-book-card ${isRead ? "rld-book-card--read" : ""}`}
                  >
                    {/* Cover */}
                    <div className="rld-cover">
                      {book.thumbnail ? (
                        <img src={book.thumbnail} alt={book.title} />
                      ) : (
                        <div className="rld-cover-placeholder">Geen cover</div>
                      )}
                    </div>

                    {/* Body */}
                    <div className="rld-book-body">
                      <h3 className="rld-book-title">{book.title}</h3>
                      <p className="rld-book-author">
                        {book.authors?.join(", ") || "Onbekend"}
                      </p>
                      {book.isbn && (
                        <span className="rld-isbn">ISBN: {book.isbn}</span>
                      )}

                      <div className="rld-book-actions">
                        <button
                          className="rld-open-btn"
                          onClick={() => router.push(`/detailpage/${book.id}`)}
                        >
                          Openen
                        </button>
                        <button
                          className={`rld-read-btn ${isRead ? "rld-read-btn--done" : ""}`}
                          onClick={() => toggleRead(book.id)}
                        >
                          {isRead
                            ? "Markeer als ongelezen"
                            : "Markeer als gelezen"}
                        </button>
                        {detail.ownList && (
                          <button
                            className="rld-btn-danger"
                            onClick={() => removeBook(book.id)}
                          >
                            Verwijderen
                          </button>
                        )}
                      </div>
                    </div>
                    {/* Badges */}
                    <div className="rld-book-badges">
                      <span
                        className={`rld-status-badge ${
                          isRead
                            ? "rld-status-badge--read"
                            : "rld-status-badge--unread"
                        }`}
                      >
                        {isRead ? "Gelezen" : "Nog te lezen"}
                      </span>
                      {isUnavailable && (
                        <span className="rld-status-badge rld-status-badge--unavailable">
                          Niet beschikbaar
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
