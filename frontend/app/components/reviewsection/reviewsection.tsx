"use client";

import { useEffect, useState } from "react";
import "./reviewsection.css";
import Pagination from "../../catalog/pagination";
import { useAuth } from "../../context/AuthContext";
import ReviewCard from "./ReviewCard";
import ReportReviewModal from "./ReportReviewModal";
import ReviewForm from "./ReviewForm";
import type {
  ReportReason,
  ReviewSectionProps,
  ReviewSummary,
} from "./reviewTypes";

const REVIEWS_PER_PAGE = 5;

export default function ReviewSection({
  isbn,
  onReviewSubmitted,
}: ReviewSectionProps) {
  const { user } = useAuth();
  const canModerateReviewDelete =
    user?.role === "TEACHER" || user?.role === "BIBLIOTHEEKBEHEERDER";
  const [reviews, setReviews] = useState<ReviewSummary[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingReviewId, setEditingReviewId] = useState<number | null>(null);
  const [flaggingReviewId, setFlaggingReviewId] = useState<number | null>(null);
  const [deleteReviewId, setDeleteReviewId] = useState<number | null>(null);
  const [isDeletingReview, setIsDeletingReview] = useState(false);
  const [reportReviewId, setReportReviewId] = useState<number | null>(null);
  const [reportReason, setReportReason] = useState<ReportReason | "">("");
  const [reportError, setReportError] = useState("");
  const ownReview = user
    ? (reviews.find((review) => review.userId === user.id) ?? null)
    : null;
  const editingReview =
    editingReviewId !== null
      ? (reviews.find((review) => review.id === editingReviewId) ?? null)
      : null;

  async function extractErrorMessage(res: Response): Promise<string> {
    try {
      const payload = await res.clone().json();
      if (typeof payload?.message === "string" && payload.message.trim()) {
        return payload.message;
      }
      if (typeof payload?.error === "string" && payload.error.trim()) {
        return payload.error;
      }
      if (typeof payload?.detail === "string" && payload.detail.trim()) {
        return payload.detail;
      }
    } catch {
      // Fallback handled below.
    }

    try {
      const text = (await res.text()).trim();
      if (text) return text;
    } catch {
      // Fallback handled below.
    }

    return "Er liep iets fout. Probeer opnieuw.";
  }

  const totalPages = Math.ceil(reviews.length / REVIEWS_PER_PAGE);
  const paginatedReviews = reviews.slice(
    (currentPage - 1) * REVIEWS_PER_PAGE,
    currentPage * REVIEWS_PER_PAGE,
  );

  function fetchReviews() {
    setLoading(true);
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/reviews/book/${isbn}`, {credentials: "include"})
      .then((res) => res.json())
      .then((data: ReviewSummary[]) => {
        setReviews(data);
        setCurrentPage(1);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    fetchReviews();
  }, [isbn]);

  function handleSubmitted() {
    setEditingReviewId(null);
    setShowForm(false);
    fetchReviews();
    onReviewSubmitted?.();
  }

  function toggleForm() {
    if (showForm) {
      setShowForm(false);
      setEditingReviewId(null);
      return;
    }

    if (ownReview) {
      setEditingReviewId(ownReview.id);
    } else {
      setEditingReviewId(null);
    }

    setShowForm(true);
  }

  function openEditForm(reviewId: number) {
    setEditingReviewId(reviewId);
    setShowForm(true);
  }

  function openReportModal(reviewId: number) {
    setReportReviewId(reviewId);
    setReportReason("");
    setReportError("");
  }

  function closeReportModal() {
    if (flaggingReviewId !== null) {
      return;
    }
    setReportReviewId(null);
    setReportReason("");
    setReportError("");
  }

  async function handleFlag() {
    if (reportReviewId === null) {
      return;
    }
    if (!reportReason) {
      setReportError("Kies een reden voor de rapportage.");
      return;
    }

    const reviewId = reportReviewId;
    setFlaggingReviewId(reviewId);
    setReportError("");

    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reviews/${reviewId}/flag`,
        {
          method: "PATCH",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ reason: reportReason }),
        },
      );

      if (!res.ok) {
        setReportError(await extractErrorMessage(res));
        return;
      }

      closeReportModal();
      fetchReviews();
    } catch {
      setReportError("Er liep iets fout. Probeer opnieuw.");
    } finally {
      setFlaggingReviewId(null);
    }
  }

  function openDeleteConfirm(reviewId: number) {
    setDeleteReviewId(reviewId);
  }

  function closeDeleteConfirm() {
    if (isDeletingReview) {
      return;
    }
    setDeleteReviewId(null);
  }

  async function handleDeleteReview(reviewId: number) {
    if (isDeletingReview) {
      return;
    }

    setIsDeletingReview(true);

    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reviews/${reviewId}`,
        {
          method: "DELETE",
          credentials: "include",
        },
      );

      if (!res.ok) {
        return;
      }

      setDeleteReviewId(null);
      fetchReviews();
      onReviewSubmitted?.();
    } catch (error) {
      console.error("Delete review failed", error);
    } finally {
      setIsDeletingReview(false);
    }
  }

  return (
    <div className="reviewSection">
      <div className="reviewSectionHeader">
        <h2 className="reviewSectionTitle">Reviews</h2>
        <button className="placeReview" onClick={toggleForm}>
          {showForm
            ? "Annuleren"
            : ownReview
              ? "Bewerk review"
              : "Plaats review"}
        </button>
      </div>

      {showForm && (
        <ReviewForm
          isbn={isbn}
          onSubmitted={handleSubmitted}
          mode={editingReview ? "edit" : "create"}
          reviewId={editingReview?.id}
          initialText={editingReview?.text ?? ""}
          initialRating={editingReview?.rating ?? 0}
          initialSpoiler={editingReview?.spoiler ?? false}
        />
      )}

      {loading ? (
        <p className="reviewLoading">Reviews laden...</p>
      ) : reviews.length === 0 ? (
        <p className="reviewEmpty">Nog geen reviews, wees de eerste!</p>
      ) : (
        <>
          <div className="reviewList">
            {paginatedReviews.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                onFlag={openReportModal}
                isFlagging={flaggingReviewId === review.id}
                canEdit={user?.id === review.userId}
                onEdit={openEditForm}
                canDelete={
                  user?.id === review.userId || canModerateReviewDelete
                }
                onAskDelete={openDeleteConfirm}
                onCancelDelete={closeDeleteConfirm}
                onConfirmDelete={handleDeleteReview}
                isDeleteConfirmOpen={deleteReviewId === review.id}
                isDeleting={isDeletingReview && deleteReviewId === review.id}
              />
            ))}
          </div>

          {reviews.length > REVIEWS_PER_PAGE && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          )}
        </>
      )}

      <ReportReviewModal
        isOpen={reportReviewId !== null}
        reportReason={reportReason}
        reportError={reportError}
        isSubmitting={flaggingReviewId !== null}
        onReasonChange={setReportReason}
        onCancel={closeReportModal}
        onSubmit={handleFlag}
      />
    </div>
  );
}
