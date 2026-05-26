interface HardDeleteConfirmModalProps {
  isOpen: boolean;
  isDeleting: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

export default function HardDeleteConfirmModal({
  isOpen,
  isDeleting,
  onCancel,
  onConfirm,
}: HardDeleteConfirmModalProps) {
  if (!isOpen) return null;

  return (
    <div className="adminDeleteModalOverlay" role="dialog" aria-modal="true">
      <div className="adminDeleteModalBox">
        <h3 className="adminDeleteModalTitle">Review permanent verwijderen</h3>
        <p className="adminDeleteModalText">
          Deze review wordt permanent verwijderd. Deze actie kan je niet
          ongedaan maken.
        </p>
        <div className="adminDeleteModalActions">
          <button
            className="adminDeleteCancelBtn"
            onClick={onCancel}
            disabled={isDeleting}
          >
            Annuleren
          </button>
          <button
            className="adminDeleteSubmitBtn"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? "Bezig..." : "Permanent verwijderen"}
          </button>
        </div>
      </div>
    </div>
  );
}
