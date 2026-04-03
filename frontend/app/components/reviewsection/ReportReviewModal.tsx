"use client";

import { REPORT_REASON_OPTIONS, type ReportReason } from "./reviewTypes";

interface ReportReviewModalProps {
  isOpen: boolean;
  reportReason: ReportReason | "";
  reportError: string;
  isSubmitting: boolean;
  onReasonChange: (reason: ReportReason) => void;
  onCancel: () => void;
  onSubmit: () => void;
}

export default function ReportReviewModal({
  isOpen,
  reportReason,
  reportError,
  isSubmitting,
  onReasonChange,
  onCancel,
  onSubmit,
}: ReportReviewModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <div className="reportModalOverlay" role="dialog" aria-modal="true">
      <div className="reportModalBox">
        <h3 className="reportModalTitle">Reden van rapportage</h3>

        <div className="reportReasonList">
          {REPORT_REASON_OPTIONS.map((option) => (
            <label key={option.value} className="reportReasonOption">
              <input
                type="radio"
                name="review-report-reason"
                value={option.value}
                checked={reportReason === option.value}
                onChange={() => onReasonChange(option.value)}
                disabled={isSubmitting}
              />
              <span>{option.label}</span>
            </label>
          ))}
        </div>

        {reportError && <p className="reportError">{reportError}</p>}

        <div className="reportModalActions">
          <button
            className="reportCancelBtn"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Annuleren
          </button>
          <button
            className="reportSubmitBtn"
            onClick={onSubmit}
            disabled={isSubmitting || !reportReason}
          >
            {isSubmitting ? "Bezig..." : "Rapporteer"}
          </button>
        </div>
      </div>
    </div>
  );
}
