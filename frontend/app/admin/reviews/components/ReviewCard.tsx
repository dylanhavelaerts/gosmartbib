import {
  ModerationReview,
  ReportReason,
  ReviewStatus,
} from "../useReviewModeration";

interface ReviewCardProps {
  review: ModerationReview;
  approving: boolean;
  rejecting: boolean;
  hardDeleting: boolean;
  onApprove: (reviewId: number) => void;
  onOpenReject: (reviewId: number) => void;
  onHardDelete: (reviewId: number) => void;
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

function statusChip(review: ModerationReview): ReviewStatus {
  if (review.adminDeleted) {
    return "REJECTED";
  }

  return review.reviewStatus;
}

function statusLabel(status: ReviewStatus): string {
  switch (status) {
    case "AWAITING_MODERATION":
      return "In afwachting";
    case "APPROVED":
      return "Goedgekeurd";
    case "REJECTED":
      return "Afgekeurd";
    default:
      return "Onbekend";
  }
}

function statusClassName(status: ReviewStatus): string {
  switch (status) {
    case "AWAITING_MODERATION":
      return "status-awaiting";
    case "APPROVED":
      return "status-approved";
    case "REJECTED":
      return "status-rejected";
    default:
      return "";
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

export function formatSchoolLabel(value: string): string {
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

export default function ReviewCard({
  review,
  approving,
  rejecting,
  hardDeleting,
  onApprove,
  onOpenReject,
  onHardDelete,
}: ReviewCardProps) {
  const cardStatus = statusChip(review);

  return (
    <article
      className={`adminReviewCard ${review.adminDeleted ? "adminDeletedCard" : ""}`}
    >
      <div className="reviewUserBlock">
        <p className="reviewUid">{review.userSmartschoolUid}</p>
        <p className="reviewMeta">{review.userRole}</p>
        <p className="reviewMeta">{formatSchoolLabel(review.schoolName)}</p>
      </div>

      <div className="reviewBody">
        <div className="reviewCardTopRow">
          <h2 className="reviewBookTitle">{review.bookTitle}</h2>
          <div className="reviewTopActions">
            <div className="reviewActions">
              {!review.adminDeleted && review.reviewStatus !== "APPROVED" && (
                <button
                  className="approveBtn"
                  onClick={() => onApprove(review.id)}
                  disabled={approving}
                >
                  {approving ? "Bezig..." : "Accepteer"}
                </button>
              )}
              <button
                className="adminDeleteBtn"
                onClick={() => onOpenReject(review.id)}
                disabled={review.adminDeleted || rejecting || hardDeleting}
              >
                {review.adminDeleted ? "Reeds afgekeurd" : "Afkeuren"}
              </button>
              <button
                className="hardDeleteBtn"
                onClick={() => onHardDelete(review.id)}
                disabled={hardDeleting}
              >
                {hardDeleting ? "Bezig..." : "Permanent verwijderen"}
              </button>
            </div>
            <span className={`statusBadge ${statusClassName(cardStatus)}`}>
              {statusLabel(cardStatus)}
            </span>
          </div>
        </div>

        <div className="reviewRatingRow">
          <span className="reviewStars">{renderStars(review.rating)}</span>
          <span className="reviewRatingValue">
            {review.rating.toFixed(1)}/5
          </span>
          <span className="reviewDate">{formatDate(review.reviewDate)}</span>
        </div>

        <p className="reviewText">{review.text || "(Geen tekst)"}</p>

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

          {review.flagCount > 0 && review.flagDetails.length > 0 && (
            <ul className="flagList">
              {review.flagDetails.map((detail, index) => (
                <li key={`${review.id}-${detail.flaggerUid}-${index}`}>
                  <span className="flagUid">{detail.flaggerUid}</span>
                  <span className="flagReason">
                    {reasonLabel(detail.reason)}
                  </span>
                </li>
              ))}
            </ul>
          )}

          {review.flagCount > 0 && review.flagDetails.length === 0 && (
            <p className="flagFallback">
              Deze review is gemeld, maar er is geen detail beschikbaar.
            </p>
          )}
        </div>
      </div>
    </article>
  );
}
