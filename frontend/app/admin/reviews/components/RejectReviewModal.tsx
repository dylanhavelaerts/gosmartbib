import { ChangeEvent } from "react";

interface RejectReviewModalProps {
  isOpen: boolean;
  reason: string;
  reasonError: string;
  isSubmitting: boolean;
  onReasonChange: (value: string) => void;
  onCancel: () => void;
  onSubmit: () => void;
}

export default function RejectReviewModal({
  isOpen,
  reason,
  reasonError,
  isSubmitting,
  onReasonChange,
  onCancel,
  onSubmit,
}: RejectReviewModalProps) {
  if (!isOpen) {
    return null;
  }

  function handleReasonChange(event: ChangeEvent<HTMLTextAreaElement>) {
    onReasonChange(event.target.value);
  }

  return (
    <div className="adminDeleteModalOverlay" role="dialog" aria-modal="true">
      <div className="adminDeleteModalBox">
        <h3 className="adminDeleteModalTitle">Review afkeuren</h3>
        <p className="adminDeleteModalText">
          Je kan optioneel een nota meegeven waarom deze review afgekeurd wordt.
        </p>

        <textarea
          className="adminDeleteModalInput"
          value={reason}
          onChange={handleReasonChange}
          placeholder="Bijv. ongepaste inhoud of niet conform richtlijnen"
          rows={4}
          disabled={isSubmitting}
        />

        {reasonError && <p className="adminDeleteModalError">{reasonError}</p>}

        <div className="adminDeleteModalActions">
          <button
            className="adminDeleteCancelBtn"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Annuleren
          </button>
          <button
            className="adminDeleteSubmitBtn"
            onClick={onSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? "Bezig..." : "Afkeuren"}
          </button>
        </div>
      </div>
    </div>
  );
}
