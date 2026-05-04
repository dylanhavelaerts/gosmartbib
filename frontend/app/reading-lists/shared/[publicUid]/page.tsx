"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "../../../context/AuthContext";
import "../../readinglistdetail.css";
import type {
  PublicReadingListDetail,
  ReadingListBookItem,
} from "@/app/interfaces/ReadingList";

const API_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

function formatRoleLabel(role?: string | null): string {
  switch (role) {
    case "STUDENT":
      return "Leerling";
    case "TEACHER":
      return "Leerkracht";
    case "BIBLIOTHEEKBEHEERDER":
      return "Bibliothecaris";
    case "ADMIN":
      return "Admin";
    default:
      return "Gebruiker";
  }
}

function formatDate(deadline?: string | null): string | null {
  if (!deadline) {
    return null;
  }

  const date = new Date(deadline);

  if (Number.isNaN(date.getTime())) {
    return deadline;
  }

  return date.toLocaleDateString("nl-BE", {
    day: "numeric",
    month: "long",
    year: "numeric",
  });
}

export default function SharedReadingListPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();
  const params = useParams<{ publicUid: string }>();

  const publicUid = params?.publicUid;

  const [detail, setDetail] = useState<PublicReadingListDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (authLoading) {
      return;
    }

    if (!user) {
      router.push("/login");
      return;
    }

    if (!publicUid) {
      setError("Publieke leeslijst niet gevonden.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    fetch(`${API_URL}/reading-lists/shared/${publicUid}`, {
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) {
          const body = await res.text();

          if (res.status === 404) {
            throw new Error("Publieke leeslijst niet gevonden.");
          }

          throw new Error(body || "Kon gedeelde leeslijst niet laden.");
        }

        return res.json();
      })
      .then((data: PublicReadingListDetail) => {
        setDetail(data);
      })
      .catch((err) => {
        console.error(err);
        setError(
          err instanceof Error
            ? err.message
            : "Kon gedeelde leeslijst niet laden.",
        );
      })
      .finally(() => {
        setLoading(false);
      });
  }, [authLoading, publicUid, router, user]);

  const totalCount = detail?.books.length ?? 0;
  const deadline = formatDate(detail?.deadline);

  return (
    <div className="rld-page">
      <button
        className="rld-back-btn"
        onClick={() => router.push("/reading-lists")}
      >
        ← Terug naar overzicht
      </button>

      {(authLoading || loading) && (
        <div className="rld-state">
          <div className="rld-spinner" />
          <p>Laden...</p>
        </div>
      )}

      {!authLoading && !loading && error && (
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

      {!authLoading && !loading && !error && detail && (
        <>
          <div className="rld-header-card">
            <div className="rld-header-top">
              <div className="rld-badges">
                <span className="rld-badge">Gedeelde leeslijst</span>
                <span className="rld-badge rld-badge--shared">Deelbaar</span>
              </div>
            </div>

            <h1>{detail.title}</h1>

            {detail.taskDescription && (
              <p className="rld-description">{detail.taskDescription}</p>
            )}

            <div className="rld-meta">
              {detail.creatorRole && (
                <span className="rld-meta-item">
                  Aangemaakt door:{" "}
                  <strong>{formatRoleLabel(detail.creatorRole)}</strong>
                </span>
              )}

              <span className="rld-meta-item">
                <strong>{totalCount}</strong>{" "}
                {totalCount === 1 ? "boek" : "boeken"}
              </span>
            </div>

            {deadline && (
              <div className="rld-deadline rld-deadline--normal">
                Deadline: {deadline}
              </div>
            )}
          </div>

          {totalCount === 0 ? (
            <div className="rld-state rld-state--empty">
              <p>Deze leeslijst bevat nog geen boeken.</p>
            </div>
          ) : (
            <div className="rld-book-grid">
              {detail.books.map((book: ReadingListBookItem) => {
                const isUnavailable = book.availableCopies === 0;

                return (
                  <div key={book.id} className="rld-book-card">
                    <div className="rld-cover">
                      {book.thumbnail ? (
                        <img src={book.thumbnail} alt={book.title} />
                      ) : (
                        <div className="rld-cover-placeholder">Geen cover</div>
                      )}
                    </div>

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
                      </div>
                    </div>

                    <div className="rld-book-badges">
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