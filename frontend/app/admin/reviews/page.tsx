"use client";

import ProtectedRoute from "@/app/components/ProtectedRoute";
import Pagination from "@/app/catalog/pagination";
import { useEffect, useMemo, useState } from "react";
import "./adminReview.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const REVIEWS_PER_PAGE = 10;

type ReviewStatus = "AWAITING_MODERATION" | "APPROVED" | "REJECTED";
type SortOption = "newest" | "oldest";
type FilterStatus = "all" | "flagged" | "adminDeleted";
type ReportReason = "FOUT_TAALGEBRUIK" | "SPAM" | "ANDERE";

interface MeResponse {
  role: "STUDENT" | "TEACHER" | "BIBLIOTHEEKBEHEERDER" | "ADMIN" | "OTHER";
  school: {
    id: number;
    name: string;
  } | null;
}

interface ReviewFlagDetail {
  flaggerUid: string;
  reason: ReportReason;
}

interface ModerationReview {
  id: number;
  userId: number;
  userSmartschoolUid: string;
  userRole: string;
  schoolId: number;
  schoolName: string;
  bookISBN: string;
  bookTitle: string;
  text: string;
  reviewDate: string;
  reviewStatus: ReviewStatus;
  rating: number;
  flagCount: number;
  flagDetails: ReviewFlagDetail[];
  adminDeleted: boolean;
  adminDeleteNote: string | null;
}

function reasonLabel(reason: ReportReason): string {
  switch (reason) {
    case "FOUT_TAALGEBRUIK":
      return "Fout taalgebruik";
    case "SPAM":
      return "Spam";
    case "ANDERE":
      return "Andere";
    default:
      return "Onbekend";
  }
}

function statusChip(review: ModerationReview): FilterStatus {
  if (review.adminDeleted) {
    return "adminDeleted";
  }

  if (review.flagCount > 0) {
    return "flagged";
  }

  return "all";
}

function statusLabel(status: FilterStatus): string {
  switch (status) {
    case "flagged":
      return "Flagged";
    case "adminDeleted":
      return "Verwijderd";
    default:
      return "Goedgekeurd";
  }
}

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString("nl-BE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

function renderStars(rating: number): string {
  const full = Math.max(0, Math.min(5, Math.round(rating)));
  return `${"★".repeat(full)}${"☆".repeat(5 - full)}`;
}

function formatSchoolLabel(value: string): string {
  const trimmed = value.trim();
  if (!trimmed) {
    return "Onbekende school";
  }

  let hostOrName = trimmed;

  try {
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
      hostOrName = new URL(trimmed).hostname;
    } else if (!trimmed.includes(" ") && trimmed.includes(".")) {
      hostOrName = new URL(`https://${trimmed}`).hostname;
    }
  } catch {
    // Als parsing niet lukt -> gebruik originele waarde
  }

  const normalized = hostOrName.toLowerCase();
  if (normalized.endsWith(".smartschool.be")) {
    return normalized.replace(/\.smartschool\.be$/, "");
  }

  return hostOrName;
}

export default function AdminReviewsPage() {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [reviews, setReviews] = useState<ModerationReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");

  const [deleteReviewId, setDeleteReviewId] = useState<number | null>(null);
  const [deleteReason, setDeleteReason] = useState("");
  const [deleteReasonError, setDeleteReasonError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);

  const [filterStatus, setFilterStatus] = useState<FilterStatus>("all");
  const [sortOption, setSortOption] = useState<SortOption>("newest");
  const [currentPage, setCurrentPage] = useState(1);

  async function fetchModerationReviews() {
    if (!API_URL) {
      setError("NEXT_PUBLIC_API_URL ontbreekt");
      setLoading(false);
      return;
    }

    const response = await fetch(`${API_URL}/reviews/moderation`, {
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error("Kon reviewmeldingen niet ophalen");
    }

    const data: ModerationReview[] = await response.json();
    setReviews(data);
  }

  useEffect(() => {
    async function load() {
      if (!API_URL) {
        setError("NEXT_PUBLIC_API_URL ontbreekt");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");

        const meResponse = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          setError("Je bent niet ingelogd");
          return;
        }

        const meData: MeResponse = await meResponse.json();
        setMe(meData);

        await fetchModerationReviews();
      } catch {
        setError("Er ging iets mis tijdens het laden van reviewmoderatie");
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  async function submitAdminDelete() {
    if (!API_URL || deleteReviewId === null || isDeleting) {
      return;
    }

    const trimmedReason = deleteReason.trim();

    try {
      setIsDeleting(true);
      setDeleteReasonError("");
      setActionError("");

      const response = await fetch(
        `${API_URL}/reviews/${deleteReviewId}/admin-delete`,
        {
          method: "PATCH",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ reason: trimmedReason || null }),
        },
      );

      if (!response.ok) {
        let serverMessage = "";
        try {
          const contentType = response.headers.get("content-type") ?? "";
          if (contentType.includes("application/json")) {
            const payload = await response.json();
            serverMessage =
              payload?.message ?? payload?.error ?? payload?.details ?? "";
          } else {
            serverMessage = (await response.text()).trim();
          }
        } catch {
          // Ignore parsing errors and fall back to status text.
        }

        const fallback = `Kon review niet verwijderen (HTTP ${response.status}).`;
        setDeleteReasonError(serverMessage || fallback);
        return;
      }

      await fetchModerationReviews();
      setDeleteReviewId(null);
      setDeleteReason("");
    } catch {
      setActionError("Er ging iets mis tijdens admin delete.");
    } finally {
      setIsDeleting(false);
    }
  }

  function openDeleteModal(reviewId: number) {
    setDeleteReviewId(reviewId);
    setDeleteReason("");
    setDeleteReasonError("");
    setActionError("");
  }

  function closeDeleteModal() {
    if (isDeleting) {
      return;
    }

    setDeleteReviewId(null);
    setDeleteReason("");
    setDeleteReasonError("");
    setActionError("");
  }

  const filteredAndSorted = useMemo(() => {
    const byStatus =
      filterStatus === "all"
        ? reviews.filter((review) => !review.adminDeleted)
        : reviews.filter((review) => statusChip(review) === filterStatus);

    return [...byStatus].sort((a, b) => {
      const left = new Date(a.reviewDate).getTime();
      const right = new Date(b.reviewDate).getTime();
      return sortOption === "newest" ? right - left : left - right;
    });
  }, [reviews, filterStatus, sortOption]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredAndSorted.length / REVIEWS_PER_PAGE),
  );

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const paginatedReviews = filteredAndSorted.slice(
    (currentPage - 1) * REVIEWS_PER_PAGE,
    currentPage * REVIEWS_PER_PAGE,
  );

  const filters: { key: FilterStatus; label: string }[] = [
    { key: "all", label: "Alle" },
    { key: "flagged", label: "Flagged" },
    { key: "adminDeleted", label: "Verwijderd" },
  ];

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "ADMIN"]}>
      <main className="adminReviewPage">
        <h1 className="adminReviewTitle">Reviewmoderatie</h1>
        <p className="adminReviewSubtitle">
          Overzicht van alle reviews van bijhorende school.
        </p>

        {me?.school?.name && (
          <p className="adminReviewSchool">School: {me.school.name}</p>
        )}

        <div className="adminReviewToolbar">
          <div className="adminReviewFilters">
            <span className="filterLabel">Filter op status:</span>
            {filters.map((filter) => (
              <button
                key={filter.key}
                className={`filterChip ${filterStatus === filter.key ? "active" : ""}`}
                onClick={() => {
                  setFilterStatus(filter.key);
                  setCurrentPage(1);
                }}
              >
                {filter.label}
              </button>
            ))}
          </div>

          <div className="adminReviewSort">
            <label htmlFor="review-sort">Sorteer:</label>
            <select
              id="review-sort"
              value={sortOption}
              onChange={(event) => {
                setSortOption(event.target.value as SortOption);
                setCurrentPage(1);
              }}
            >
              <option value="newest">Nieuwste eerst</option>
              <option value="oldest">Oudste eerst</option>
            </select>
          </div>
        </div>

        {loading && <p className="adminReviewInfo">Reviewmeldingen laden...</p>}
        {!loading && error && <p className="adminReviewError">{error}</p>}
        {!loading && !error && actionError && (
          <p className="adminReviewError">{actionError}</p>
        )}
        {!loading && !error && filteredAndSorted.length === 0 && (
          <p className="adminReviewInfo">
            Geen reviews gevonden voor deze filter.
          </p>
        )}

        {!loading && !error && filteredAndSorted.length > 0 && (
          <>
            <div className="adminReviewList">
              {paginatedReviews.map((review) => {
                const cardStatus = statusChip(review);

                return (
                  <article
                    key={review.id}
                    className={`adminReviewCard ${review.adminDeleted ? "adminDeletedCard" : ""}`}
                  >
                    <div className="reviewUserBlock">
                      <p className="reviewUid">{review.userSmartschoolUid}</p>
                      <p className="reviewMeta">{review.userRole}</p>
                      <p className="reviewMeta">
                        {formatSchoolLabel(review.schoolName)}
                      </p>
                    </div>

                    <div className="reviewBody">
                      <div className="reviewCardTopRow">
                        <h2 className="reviewBookTitle">{review.bookTitle}</h2>
                        <div className="reviewTopActions">
                          <div className="reviewActions">
                            <button
                              className="adminDeleteBtn"
                              onClick={() => openDeleteModal(review.id)}
                              disabled={review.adminDeleted}
                            >
                              {review.adminDeleted
                                ? "Reeds verwijderd"
                                : "Verwijder review"}
                            </button>
                          </div>
                          <span className={`statusBadge status-${cardStatus}`}>
                            {statusLabel(cardStatus)}
                          </span>
                        </div>
                      </div>

                      <div className="reviewRatingRow">
                        <span className="reviewStars">
                          {renderStars(review.rating)}
                        </span>
                        <span className="reviewRatingValue">
                          {review.rating.toFixed(1)}/5
                        </span>
                        <span className="reviewDate">
                          {formatDate(review.reviewDate)}
                        </span>
                      </div>

                      <p className="reviewText">
                        {review.text || "(Geen tekst)"}
                      </p>

                      <div className="reviewFlagBlock">
                        {review.adminDeleted && review.adminDeleteNote && (
                          <p className="adminDeleteNote">
                            Admin delete reden: {review.adminDeleteNote}
                          </p>
                        )}

                        <p className="flagHeader">
                          Gemeld: {review.flagCount}{" "}
                          {review.flagCount === 1 ? "keer" : "keer"}
                        </p>

                        {review.flagCount > 0 &&
                          review.flagDetails.length > 0 && (
                            <ul className="flagList">
                              {review.flagDetails.map((detail, index) => (
                                <li
                                  key={`${review.id}-${detail.flaggerUid}-${index}`}
                                >
                                  <span className="flagUid">
                                    {detail.flaggerUid}
                                  </span>
                                  <span className="flagReason">
                                    {reasonLabel(detail.reason)}
                                  </span>
                                </li>
                              ))}
                            </ul>
                          )}

                        {review.flagCount > 0 &&
                          review.flagDetails.length === 0 && (
                            <p className="flagFallback">
                              Deze review is gemeld, maar er is geen detail
                              beschikbaar.
                            </p>
                          )}
                      </div>
                    </div>
                  </article>
                );
              })}
            </div>

            {filteredAndSorted.length > REVIEWS_PER_PAGE && (
              <div className="adminReviewPagination">
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={setCurrentPage}
                />
              </div>
            )}
          </>
        )}

        {deleteReviewId !== null && (
          <div
            className="adminDeleteModalOverlay"
            role="dialog"
            aria-modal="true"
          >
            <div className="adminDeleteModalBox">
              <h3 className="adminDeleteModalTitle">
                Review admin verwijderen
              </h3>
              <p className="adminDeleteModalText">
                Je kan optioneel een nota meegeven waarom deze review admin
                deleted wordt.
              </p>

              <textarea
                className="adminDeleteModalInput"
                value={deleteReason}
                onChange={(event) => setDeleteReason(event.target.value)}
                placeholder="Bijv. ongepaste inhoud of niet conform richtlijnen"
                rows={4}
                disabled={isDeleting}
              />

              {deleteReasonError && (
                <p className="adminDeleteModalError">{deleteReasonError}</p>
              )}

              <div className="adminDeleteModalActions">
                <button
                  className="adminDeleteCancelBtn"
                  onClick={closeDeleteModal}
                  disabled={isDeleting}
                >
                  Annuleren
                </button>
                <button
                  className="adminDeleteSubmitBtn"
                  onClick={submitAdminDelete}
                  disabled={isDeleting}
                >
                  {isDeleting ? "Bezig..." : "Verwijder met nota"}
                </button>
              </div>
            </div>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}
