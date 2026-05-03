import { ChangeEvent } from "react";

interface ActionModalProps {
  isOpen: boolean;
  action: "approve" | "reject";
  note: string;
  isSubmitting: boolean;
  onNoteChange: (value: string) => void;
  onCancel: () => void;
  onSubmit: () => void;
}

export default function ActionModal({
  isOpen,
  action,
  note,
  isSubmitting,
  onNoteChange,
  onCancel,
  onSubmit,
}: ActionModalProps) {
  if (!isOpen) return null;

  const isApprove = action === "approve";

  function handleChange(event: ChangeEvent<HTMLTextAreaElement>) {
    onNoteChange(event.target.value);
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal-box">
        <h3 className="modal-title">
          {isApprove ? "Aankoopverzoek goedkeuren" : "Aankoopverzoek afwijzen"}
        </h3>
        <p className="modal-text">Nota (optioneel)</p>
        <textarea
          className="modal-input"
          value={note}
          onChange={handleChange}
          placeholder="Optionele nota..."
          rows={3}
          disabled={isSubmitting}
        />
        <div className="modal-actions">
          <button
            className="modal-cancel-btn"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Annuleren
          </button>
          <button
            className={`modal-submit-btn ${isApprove ? "modal-submit-approve" : "modal-submit-reject"}`}
            onClick={onSubmit}
            disabled={isSubmitting}
          >
            {isSubmitting ? "Bezig..." : isApprove ? "Goedkeuren" : "Afwijzen"}
          </button>
        </div>
      </div>
    </div>
  );
}
