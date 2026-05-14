"use client";

import "./SyncModal.css";

type SyncStatus = "idle" | "confirm" | "loading" | "done";

type SchoolSyncResult = {
  schoolDomain: string;
  added: number;
  removed: number;
  errors: string[];
};

type SyncSummary = {
  totalAdded: number;
  totalRemoved: number;
  schools: SchoolSyncResult[];
};

type Props = {
  syncStatus: SyncStatus;
  syncStep: number;
  syncResult: SyncSummary | null;
  syncSteps: string[];
  onConfirm: () => void;
  onClose: () => void;
};

export default function SyncModal({
  syncStatus,
  syncStep,
  syncResult,
  syncSteps,
  onConfirm,
  onClose,
}: Props) {
  if (syncStatus === "idle") return null;

  return (
    <div className="syncModalOverlay">
      <div className="syncModalBox">
        {syncStatus === "confirm" && (
          <>
            <div className="syncModalHeader">
              <h2>Synchronisatie starten?</h2>
            </div>
            <div className="syncModalBody">
              <p>
                Dit synchroniseert alle gebruikers met OneRoster. Verwijderde
                gebruikers worden geanonimiseerd en nieuwe gebruikers worden
                aangemaakt.
              </p>
              <p>
                Alleen gebruikers van schooldomeinen die gekoppeld zijn aan een
                OneRoster-integratie worden gesynchroniseerd.
              </p>
            </div>
            <div className="syncModalFooter">
              <button
                style={{ background: "none", border: "none", cursor: "pointer", color: "#555" }}
                onClick={onClose}
              >
                Annuleren
              </button>
              <button className="adminPrimaryButton" onClick={onConfirm}>
                Starten
              </button>
            </div>
          </>
        )}

        {syncStatus === "loading" && (
          <>
            <div className="syncModalHeader">
              <h2>Synchronisatie bezig...</h2>
            </div>
            <div className="syncModalBody">
              <p className="syncModalWarning">Klik niet weg</p>
              <div className="syncProgressBar">
                <div className="syncProgressBarFill" />
              </div>
              <p>{syncSteps[syncStep]}</p>
            </div>
          </>
        )}

        {syncStatus === "done" && (
          <>
            <div className="syncModalHeader">
              <h2>
                {syncResult
                  ? "Synchronisatie voltooid"
                  : "Synchronisatie mislukt"}
              </h2>
            </div>
            <div className="syncModalBody">
              {syncResult ? (
                <>
                  <p>
                    {syncResult.totalAdded} gebruiker(s) toegevoegd,{" "}
                    {syncResult.totalRemoved} verwijderd.
                  </p>
                  {syncResult.schools.map((school) => (
                    <div key={school.schoolDomain} className="syncResultItem">
                      <span>{school.schoolDomain}</span>
                      <span>
                        +{school.added} / -{school.removed}
                        {school.errors.length > 0 && (
                          <span className="syncResultError">
                            {" "}
                            ({school.errors.length} fout(en))
                          </span>
                        )}
                      </span>
                    </div>
                  ))}
                </>
              ) : (
                <p>
                  Er ging iets mis tijdens de synchronisatie. Probeer het later
                  opnieuw.
                </p>
              )}
            </div>
            <div className="syncModalFooter">
              <button className="adminPrimaryButton" onClick={onClose}>
                Sluiten
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
