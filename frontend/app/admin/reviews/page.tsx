"use client";

import ReviewCard from "./components/ReviewCard";
import RejectReviewModal from "./components/RejectReviewModal";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import Pagination from "@/app/catalog/pagination";
import { useEffect, useMemo, useState } from "react";
import { ModerationReview, useReviewModeration } from "./useReviewModeration";
import HardDeleteConfirmModal from "./components/HardDeleteConfirmModal";
import "./adminReview.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const REVIEWS_PER_PAGE = 10;

type SortOption = "newest" | "oldest";
type FilterStatus = "all" | "awaitingModeration" | "approved" | "rejected";

/**
 * Beheerpagina voor reviewmoderatie, enkel toegankelijk voor BIBLIOTHEEKBEHEERDER.
 * Toont reviews van de eigen school met filter- en sorteeropties.
 */
export default function AdminReviewsPage() {
  const {
    me,
    reviews,
    loading,
    error,
    actionError,
    deleteReasonError,
    isDeleting,
    approvingReviewId,
    hardDeletingReviewId,
    setActionError,
    setDeleteReasonError,
    approveReview,
    softRejectReview,
    hardDeleteReview,
  } = useReviewModeration(API_URL);

  const [deleteReviewId, setDeleteReviewId] = useState<number | null>(null);
  const [deleteReason, setDeleteReason] = useState("");

  const [filterStatus, setFilterStatus] = useState<FilterStatus>("all");
  const [sortOption, setSortOption] = useState<SortOption>("newest");
  const [currentPage, setCurrentPage] = useState(1);
  const [hardDeleteTargetId, setHardDeleteTargetId] = useState<number | null>(
    null,
  );

  async function submitAdminDelete() {
    const wasSuccessful = await softRejectReview(deleteReviewId, deleteReason);
    if (!wasSuccessful) {
      return;
    }

    setDeleteReviewId(null);
    setDeleteReason("");
  }

  function openHardDeleteModal(reviewId: number) {
    setHardDeleteTargetId(reviewId);
  }

  function closeHardDeleteModal() {
    if (hardDeletingReviewId !== null) return;
    setHardDeleteTargetId(null);
  }
  async function confirmHardDelete() {
    if (hardDeleteTargetId === null) return;
    await hardDeleteReview(hardDeleteTargetId);
    setHardDeleteTargetId(null);
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

  /** "Afgekeurd"-filter omvat ook adminDeleted-reviews. */
  const filteredAndSorted = useMemo(() => {
    const byStatus = reviews.filter((review) => {
      if (filterStatus === "all") {
        return true;
      }

      if (filterStatus === "awaitingModeration") {
        return review.reviewStatus === "AWAITING_MODERATION";
      }

      if (filterStatus === "approved") {
        return review.reviewStatus === "APPROVED";
      }

      return review.reviewStatus === "REJECTED" || review.adminDeleted;
    });

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
    { key: "awaitingModeration", label: "In afwachting" },
    { key: "approved", label: "Goedgekeurd" },
    { key: "rejected", label: "Afgekeurd" },
  ];

  return (
    <ProtectedRoute allowedRoles={["LIBRARIAN"]}>
      <main className="adminReviewPage">
        <h1 className="adminReviewTitle">Reviewmoderatie</h1>
        <p className="adminReviewSubtitle">
          Overzicht van alle reviews van bijhorende school
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
            Geen reviews gevonden voor deze filter
          </p>
        )}

        {!loading && !error && filteredAndSorted.length > 0 && (
          <>
            <div className="adminReviewList">
              {paginatedReviews.map((review: ModerationReview) => (
                <ReviewCard
                  key={review.id}
                  review={review}
                  approving={approvingReviewId === review.id}
                  rejecting={
                    isDeleting &&
                    deleteReviewId === review.id &&
                    deleteReviewId !== null
                  }
                  hardDeleting={hardDeletingReviewId === review.id}
                  onApprove={approveReview}
                  onOpenReject={openDeleteModal}
                  onHardDelete={openHardDeleteModal}
                />
              ))}
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

        <RejectReviewModal
          isOpen={deleteReviewId !== null}
          reason={deleteReason}
          reasonError={deleteReasonError}
          isSubmitting={isDeleting}
          onReasonChange={setDeleteReason}
          onCancel={closeDeleteModal}
          onSubmit={submitAdminDelete}
        />

        <HardDeleteConfirmModal
          isOpen={hardDeleteTargetId !== null}
          isDeleting={hardDeletingReviewId !== null}
          onCancel={closeHardDeleteModal}
          onConfirm={confirmHardDelete}
        />
      </main>
    </ProtectedRoute>
  );
}
