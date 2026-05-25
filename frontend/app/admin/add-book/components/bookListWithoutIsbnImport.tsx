"use client";

import { useEffect, useState } from "react";
import "./bookListImport.css";
import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";
import type { MeResponse } from "@/app/interfaces/user";
import { fetchSchoolCampuses } from "@/app/utils/schoolCampuses";
import type {
  BulkImportResult,
  DuplicateWarning,
} from "@/app/interfaces/Book";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export default function BookListWithoutIsbnImport() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<BulkImportResult | null>(null);
  const [message, setMessage] = useState("");
  const [campusLoadError, setCampusLoadError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [campus, setCampus] = useState("");
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [duplicateWarnings, setDuplicateWarnings] = useState<DuplicateWarning[]>([]);
  const [selectedDuplicateRows, setSelectedDuplicateRows] = useState<number[]>([]);
  const [confirmingDuplicates, setConfirmingDuplicates] = useState(false);

  useEffect(() => {
    const loadCampusesForCurrentSchool = async () => {
      if (!API_URL) {
        setCampusLoadError("NEXT_PUBLIC_API_URL ontbreekt");
        return;
      }

      try {
        setLoadingCampuses(true);
        setCampusLoadError("");

        const meResponse = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          throw new Error("Kon de ingelogde gebruiker niet ophalen");
        }

        const meData: MeResponse = await meResponse.json();

        if (!meData.school?.id) {
          throw new Error("Geen school gevonden voor de ingelogde gebruiker");
        }

        const campusData = await fetchSchoolCampuses(API_URL, meData.school.id);
        setCampuses(campusData);
      } catch (error) {
        console.error(error);
        setCampusLoadError(
          error instanceof Error
            ? error.message
            : "Kon de campussen niet ophalen",
        );
      } finally {
        setLoadingCampuses(false);
      }
    };

    loadCampusesForCurrentSchool();
  }, []);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
    setImportResult(null);
    setDuplicateWarnings([]);
    setSelectedDuplicateRows([]);
    setMessage("");
  };

  const handleUploadExcel = async (confirmDuplicates = false) => {
    if (!selectedFile) return;

    if (!API_URL) {
      setMessage("NEXT_PUBLIC_API_URL ontbreekt.");
      return;
    }

    setLoading(true);
    setMessage("");
    setImportResult(null);

    if (!confirmDuplicates) {
      setDuplicateWarnings([]);
      setSelectedDuplicateRows([]);
    }

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      const trimmedCampus = campus.trim();

      if (trimmedCampus) {
        formData.append("campus", trimmedCampus);
      }

      if (confirmDuplicates) {
        selectedDuplicateRows.forEach((rowNumber) => {
          formData.append("confirmedDuplicateRows", String(rowNumber));
        });
      }

      const response = await fetch(`${API_URL}/books/import/no-isbn?confirmDuplicates=${confirmDuplicates}`, {
        method: "POST",
        credentials: "include",
        body: formData,
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setMessage(data?.message || "Er ging iets mis bij het importeren");
        return;
      }

      setImportResult(data);

      if (!confirmDuplicates && data?.duplicateWarnings?.length > 0) {
        setDuplicateWarnings(data.duplicateWarnings);
        setSelectedDuplicateRows(
          data.duplicateWarnings.map((warning: DuplicateWarning) => warning.rowNumber),
        );
        setMessage(
          `Er zijn ${data.duplicateWarnings.length} mogelijke dubbele boeken gevonden. Kies per boek welke aantallen je wilt toevoegen.`,
        );
        return;
      }

      setDuplicateWarnings([]);
      setMessage(`Import klaar. ${data.savedCount ?? 0} rij(en) verwerkt.`);
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken");
    } finally {
      setLoading(false);
    }
  };

  const uploadButtonClass =
    `uploadButton ${loading ? "uploadButtonLoading" : ""}`.trim();

  const messageClass = `message ${
    message.includes("klaar") || message.includes("opgeslagen")
      ? "messageSuccess"
      : "messageError"
  }`.trim();

  return (
    <>
      <h1 className="title">Excelbestand zonder ISBN toevoegen</h1>

      <p className="text">
        Gebruik deze import voor boeken zonder ISBN. Enkel titel is verplicht. Al de rest krijgt een basiswaarde als deze leeg zijn.
      </p>

      <p className="text">
        Hieronder vindt u een link naar een template om boeken toe te voegen
      </p>

      <a
        href="/BoekenlijstTemplateZonderISBN.xlsx"
        download
        className="downloadLink"
      >
        Download Excelbestand
      </a>

      <p className="helperText">
        Deze campus wordt gebruikt als basis voor rijen waar de Campus-kolom
        leeg is
      </p>

      <label className="campusField">
        Campus
        <select
          value={campus}
          onChange={(e) => setCampus(e.target.value)}
          className="campusInput"
          disabled={loadingCampuses}
        >
          <option value="">
            {loadingCampuses ? "Campussen laden..." : "Geen campus"}
          </option>

          {campuses.map((campusOption) => (
            <option key={campusOption.id} value={campusOption.name}>
              {campusOption.name}
            </option>
          ))}
        </select>
      </label>

      {campusLoadError && <p className="fieldError">{campusLoadError}</p>}

      <p className="spacedText">Voeg hieronder de aangevulde Excel-file toe</p>

      <div className="fileInputBox">
        <input type="file" accept=".xlsx,.xls" onChange={handleFileChange} />
      </div>

      {selectedFile && (
        <div className="selectedFile">
          <div className="selectedFileRow">
            <p>Geselecteerd bestand: {selectedFile.name}</p>

            <button
              type="button"
              onClick={() => handleUploadExcel(false)}
              disabled={loading}
              className={uploadButtonClass}
            >
              {loading ? "Bezig met importeren..." : "Importeer Excelbestand"}
            </button>
          </div>
        </div>
      )}

      {importResult && (
        <div className="resultCard">
          <h2>Import resultaat</h2>
          <p>Totaal aantal rijen: {importResult.totalRows}</p>
          <p>Verwerkte rijen: {importResult.savedCount}</p>
          <p>Mismatches / fouten: {importResult.mismatchCount}</p>

          {importResult.mismatches.length > 0 && (
            <div className="mismatchSection">
              <p>Problemen gevonden in deze rijen:</p>

              <ul>
                {importResult.mismatches.map((mismatch, index) => (
                  <li
                    className="mismatchElement"
                    key={`${mismatch.rowNumber}-${index}`}
                  >
                    <p className="mismatchTitle">
                      Rij {mismatch.rowNumber}: {mismatch.excelTitle || "Geen titel"}{" "}
                      | Reden: {mismatch.reason}
                    </p>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {duplicateWarnings.length > 0 && (
        <div className="resultCard duplicateWarningCard">
          <div className="duplicateWarningHeader">
            <div>
              <p className="duplicateEyebrow">Controle vereist</p>
              <h2>Mogelijke dubbele boeken gevonden</h2>
              <p>
                Deze boeken lijken al te bestaan. Kies per boek of je de aantallen
                wilt toevoegen aan het bestaande boek.
              </p>
            </div>

            <span className="duplicateCountBadge">
              {selectedDuplicateRows.length} van {duplicateWarnings.length} geselecteerd
            </span>
          </div>

          <div className="duplicateToolbar">
            <button
              type="button"
              className="secondaryButton"
              onClick={() =>
                setSelectedDuplicateRows(
                  duplicateWarnings.map((warning) => warning.rowNumber),
                )
              }
            >
              Alles selecteren
            </button>

            <button
              type="button"
              className="secondaryButton"
              onClick={() => setSelectedDuplicateRows([])}
            >
              Alles deselecteren
            </button>
          </div>

          <div className="duplicateList">
            {duplicateWarnings.map((warning) => {
              const isSelected = selectedDuplicateRows.includes(warning.rowNumber);

              return (
                <label
                  key={`${warning.rowNumber}-${warning.existingBookId}`}
                  className={`duplicateItem ${isSelected ? "duplicateItemSelected" : ""}`}
                >
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedDuplicateRows((prev) => [
                          ...prev,
                          warning.rowNumber,
                        ]);
                      } else {
                        setSelectedDuplicateRows((prev) =>
                          prev.filter((rowNumber) => rowNumber !== warning.rowNumber),
                        );
                      }
                    }}
                  />

                  <div className="duplicateItemContent">
                    <div className="duplicateItemTop">
                      <div>
                        <p className="duplicateRowLabel">Rij {warning.rowNumber}</p>
                        <h3>{warning.title}</h3>
                      </div>

                      <span className="duplicateStatusBadge">
                        {isSelected ? "Wordt toegevoegd" : "Wordt overgeslagen"}
                      </span>
                    </div>

                    <div className="duplicateMetaGrid">
                      <div>
                        <span>Auteur(s)</span>
                        <strong>{warning.authors?.join(", ") || "Onbekend"}</strong>
                      </div>

                      <div>
                        <span>Uitgever</span>
                        <strong>{warning.publisher || "Onbekend"}</strong>
                      </div>

                      <div>
                        <span>Campus</span>
                        <strong>{warning.campus || "Geen campus"}</strong>
                      </div>

                      <div>
                        <span>Huidige voorraad</span>
                        <strong>
                          {warning.currentTotalCopies} totaal /{" "}
                          {warning.currentAvailableCopies} beschikbaar
                        </strong>
                      </div>

                      <div>
                        <span>Toe te voegen</span>
                        <strong>
                          +{warning.totalCopiesToAdd} totaal / +{warning.availableCopiesToAdd} beschikbaar
                        </strong>
                      </div>
                    </div>

                    <p className="duplicateReason">{warning.reason}</p>
                  </div>
                </label>
              );
            })}
          </div>

          <div className="duplicateActions">
            <button
              type="button"
              className="uploadButton"
              disabled={confirmingDuplicates || loading}
              onClick={async () => {
                setConfirmingDuplicates(true);
                try {
                  await handleUploadExcel(true);
                } finally {
                  setConfirmingDuplicates(false);
                }
              }}
            >
              {confirmingDuplicates
                ? "Geselecteerde aantallen toevoegen..."
                : `Voeg geselecteerde aantallen toe (${selectedDuplicateRows.length})`}
            </button>
          </div>
        </div>
      )}

      {message && <div className={messageClass}>{message}</div>}
    </>
  );
}