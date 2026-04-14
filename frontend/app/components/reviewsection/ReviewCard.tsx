"use client";

import StarRating from "./StarRating";
import type { ReviewSummary } from "./reviewTypes";

interface ReviewCardProps {
  review: ReviewSummary;
  onFlag: (reviewId: number) => void;
  isFlagging: boolean;
  canEdit: boolean;
  onEdit: (reviewId: number) => void;
  canDelete: boolean;
  onAskDelete: (reviewId: number) => void;
  onCancelDelete: () => void;
  onConfirmDelete: (reviewId: number) => void;
  isDeleteConfirmOpen: boolean;
  isDeleting: boolean;
}

export default function ReviewCard({
  review,
  onFlag,
  isFlagging,
  canEdit,
  onEdit,
  canDelete,
  onAskDelete,
  onCancelDelete,
  onConfirmDelete,
  isDeleteConfirmOpen,
  isDeleting,
}: ReviewCardProps) {
  const formattedDate = new Date(review.reviewDate).toLocaleDateString(
    "nl-BE",
    {
      day: "numeric",
      month: "long",
      year: "numeric",
    },
  );

  const roleLabel: Record<string, string> = {
    STUDENT: "Student",
    TEACHER: "Leerkracht",
    LIBRARIAN: "Bibliothecaris",
  };

  return (
    <div className="reviewCard">
      <div className="reviewCardHeader">
        <div className="reviewCardMeta">
          <span className="reviewRole">
            {roleLabel[review.userRole] ?? review.userRole}
          </span>
          <span className="reviewDate">{formattedDate}</span>
        </div>
        <StarRating value={review.rating} />
      </div>
      {review.text && <p className="reviewText">{review.text}</p>}
      <div className="reviewCardActions">
        {canEdit && (
          <button
            className="editReviewBtn"
            onClick={() => onEdit(review.id)}
            title="Bewerk review"
            aria-label="Bewerk review"
          >
            ✎
          </button>
        )}
        {canDelete && (
          <button
            className="deleteReviewBtn"
            onClick={() => onAskDelete(review.id)}
            disabled={isDeleting}
            title="Verwijder review"
            aria-label="Verwijder review"
          >
            {isDeleting ? "..." : "🗑︎"}
          </button>
        )}
        <button
          className="flagReviewBtn"
          onClick={() => onFlag(review.id)}
          disabled={isFlagging}
          title="Meld review"
          aria-label="Meld review"
        >
          {isFlagging ? "..." : "⚑"}
        </button>
      </div>
      {isDeleteConfirmOpen && (
        <div className="deleteConfirmRow">
          <span className="deleteConfirmText">Review verwijderen?</span>
          <button
            className="deleteConfirmCancel"
            onClick={onCancelDelete}
            disabled={isDeleting}
          >
            Annuleren
          </button>
          <button
            className="deleteConfirmDelete"
            onClick={() => onConfirmDelete(review.id)}
            disabled={isDeleting}
          >
            {isDeleting ? "Bezig..." : "Verwijder"}
          </button>
        </div>
      )}
    </div>
  );
}
