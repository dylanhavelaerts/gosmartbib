"use client";

import { useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import "../readinglistdetail.css";

interface BookItem {
  id: number;
  title: string;
  authors: string[];
  thumbnail?: string | null;
  isbn: string;
  availableCopies: number;
}

interface ReadingListDetail {
  id: number;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  listType: "CLASS" | "PERSONAL";
  ownList: boolean;
  creatorName?: string | null;
  books: BookItem[];
}

const STAFF_ROLES = ["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"];

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

  const userId = user?.id ?? 0;
  const storageKey = `reading-status:${userId}:${id}`;

  const [readStatus, setReadStatus] = useState<Record<number, boolean>>({});

  useEffect(() => {
    if (!id || !user) return;

    try {
      const raw = localStorage.getItem(storageKey);
      setReadStatus(raw ? JSON.parse(raw) : {});
    } catch {
      setReadStatus({});
    }

    setLoading(true);
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
            err.message || "Er ging iets mis bij het laden van de leeslijst.",
          ),
        );
      })
      .finally(() => setLoading(false));
  }, [id, user]);

  const toggleRead = (bookId: number) => {
    setReadStatus((prev) => {
      const next = { ...prev, [bookId]: !prev[bookId] };
      try {
        localStorage.setItem(storageKey, JSON.stringify(next));
      } catch {
        // localStorage unavailable — read state lives only in memory this session
      }
      return next;
    });
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

  return (
    <div className="rld-page">
      {/* ── Back ── */}
      <button
        className="rld-back-btn"
        onClick={() => router.push("/reading-lists")}
      >
        ← Terug naar overzicht
      </button>

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
            <div className="rld-badges">
              <span className="rld-badge">
                {detail.listType === "CLASS"
                  ? "Klasleeslijst"
                  : "Eigen leeslijst"}
              </span>
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
              <p>Deze leeslijst bevat nog geen boeken.</p>
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
                      {isUnavailable && (
                        <span className="rld-unavailable-badge">
                          Niet beschikbaar
                        </span>
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
                      </div>
                    </div>

                    {/* Read status badge */}
                    <span
                      className={`rld-status-badge ${
                        isRead
                          ? "rld-status-badge--read"
                          : "rld-status-badge--unread"
                      }`}
                    >
                      {isRead ? "Gelezen" : "Nog te lezen"}
                    </span>
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
