"use client";

import "./card.css";
import { PurchaseRequest } from "../usePurchaseRequests";

interface SuggestionCardProps {
  request: PurchaseRequest;
  isBeheerder: boolean;
  approving: boolean;
  rejecting: boolean;
  deleting: boolean;
  onApprove: (id: number) => void;
  onReject: (id: number) => void;
  onDelete: (id: number) => void;
}

function statusLabel(status: string): string {
  switch (status) {
    case "PENDING":
      return "In behandeling";
    case "APPROVED":
      return "Goedgekeurd";
    case "REJECTED":
      return "Afgekeurd";
    default:
      return status;
  }
}

function statusClassName(status: string): string {
  switch (status) {
    case "PENDING":
      return "status-pending";
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

export default function SuggestionCard({
  request,
  isBeheerder,
  approving,
  rejecting,
  deleting,
  onApprove,
  onReject,
  onDelete,
}: SuggestionCardProps) {
  const isActionable = isBeheerder && request.status === "PENDING";
  const isBusy = approving || rejecting || deleting;

  return (
    <article className="suggestion-card">
      <div className="card-user">
        <p className="card-uid">{request.userSmartschoolUid}</p>
        <p className="card-date">{formatDate(request.requestDate)}</p>
      </div>
      <div className="card-body">
        <div className="card-top-row">
          <h2 className="card-title">{request.title}</h2>
          <div className="card-top-right">
            <div className="card-actions">
              {isActionable && (
                <>
                  <button
                    className="btn btn-approve"
                    onClick={() => onApprove(request.id)}
                    disabled={isBusy}
                  >
                    {approving ? "Bezig..." : "Goedkeuren"}
                  </button>
                  <button
                    className="btn btn-reject"
                    onClick={() => onReject(request.id)}
                    disabled={isBusy}
                  >
                    {rejecting ? "Bezig..." : "Afwijzen"}
                  </button>
                </>
              )}
              {isBeheerder && (
                <button
                  className="btn btn-delete"
                  onClick={() => onDelete(request.id)}
                  disabled={isBusy}
                >
                  {deleting ? "Bezig..." : "Verwijderen"}
                </button>
              )}
            </div>
            <span className={`card-status ${statusClassName(request.status)}`}>
              {statusLabel(request.status)}
            </span>
          </div>
        </div>

        {request.authors.length > 0 && (
          <p className="card-authors">
            <span className="card-authors-label">van </span>
            {request.authors.join(", ")}
          </p>
        )}
        {request.isbn && <p className="card-isbn">ISBN: {request.isbn}</p>}

        {request.note && (
          <div className="card-note">
            <span className="card-note-label">Nota:</span> {request.note}
          </div>
        )}
      </div>
    </article>
  );
}
