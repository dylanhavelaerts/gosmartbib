"use client";

import { useEffect, useState } from "react";
import "./bookListImport.css";
import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";
import type { MeResponse } from "@/app/interfaces/user";
import type {
  BulkImportResult,
  DuplicateWarning,
  ImportMismatch
} from "@/app/interfaces/Book";
import { fetchSchoolCampuses } from "@/app/utils/schoolCampuses";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export default function BookListImport() {
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
        if (campusData.length === 1) {
          setCampus(campusData[0].name);
        }
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
    setMessage("");
    setDuplicateWarnings([]);
    setSelectedDuplicateRows([]);
  };

    const handleUploadExcel = async (confirmDuplicates = false) => {
    if (!selectedFile) return;

    if (!API_URL) {
      setMessage("NEXT_PUBLIC_API_URL ontbreekt.");
      return;
    }

    const selectedCampus = campuses.length === 1 ? campuses[0].name : campus.trim();
    if (campuses.length > 1 && !selectedCampus) {
      setMessage("Kies eerst een campus voor deze import.");
      return;
    }

    setLoading(true);
    setMessage("");

    if (!confirmDuplicates) {
      setImportResult(null);
      setDuplicateWarnings([]);
      setSelectedDuplicateRows([]);
    }

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      if (selectedCampus) {
        formData.append("campus", selectedCampus);
      }

      if (confirmDuplicates) {
        selectedDuplicateRows.forEach((rowNumber) => {
          formData.append("confirmedDuplicateRows", String(rowNumber));
        });
      }

      const response = await fetch(
        `${API_URL}/books/import?confirmDuplicates=${confirmDuplicates}`,
        {
          method: "POST",
          credentials: "include",
          body: formData,
        },
      );

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setMessage(data?.message || data || "Er ging iets mis bij het importeren");
        return;
      }

      setImportResult(data);

      if (!confirmDuplicates && data?.duplicateWarnings?.length > 0) {
        setDuplicateWarnings(data.duplicateWarnings);
        setSelectedDuplicateRows(
          data.duplicateWarnings.map((warning: DuplicateWarning) => warning.rowNumber),
        );
        setMessage(
          `Er zijn ${data.duplicateWarnings.length} bestaande ISBN's gevonden. Kies per boek welke aantallen je wilt toevoegen.`,
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

   const addSingleBook = async (mismatch: ImportMismatch) => {
      if (!API_URL) {
        setMessage("NEXT_PUBLIC_API_URL ontbreekt.");
        return;
      }

      const selectedCampus = campuses.length === 1 ? campuses[0].name : campus.trim();
      if (campuses.length > 1 && !selectedCampus) {
        setMessage("Kies eerst een campus voor deze import.");
        return;
      }

      if (!mismatch.isbn) {
        setMessage("Geen ISBN gevonden voor deze rij.");
        return;
      }

      setLoading(true);

      try {
        const params = new URLSearchParams();

        if (selectedCampus) {
          params.set("campus", selectedCampus);
        }

        if (mismatch.didacticBook === true) {
          params.set("didacticBook", "true");
        }

        if (mismatch.amount !== null && mismatch.amount !== undefined) {
          params.set("amount", String(mismatch.amount));
        }

        const queryString = params.toString();

        const response = await fetch(
          `${API_URL}/books/add/${mismatch.isbn}${
            queryString ? `?${queryString}` : ""
          }`,
          {
            method: "POST",
            credentials: "include",
          },
        );

        if (response.ok) {
          setImportResult((prev) => {
            if (!prev) return prev;

            setMessage(`Import klaar. ${prev.savedCount + 1} boek(en) opgeslagen.`);

            return {
              ...prev,
              savedCount: prev.savedCount + 1,
              mismatchCount: prev.mismatchCount - 1,
              mismatches: prev.mismatches.filter(
                (item) =>
                  !(
                    item.rowNumber === mismatch.rowNumber &&
                    item.isbn === mismatch.isbn
                  ),
              ),
            };
          });

          return;
        }

        const errorText = await response.text();
        setMessage(errorText || "Er ging iets mis bij het opslaan van het boek");
      } catch (error) {
        console.error(error);
        setMessage("Kan de server niet bereiken");
      } finally {
        setLoading(false);
      }
    };

  const shouldShowCampusSelect = campuses.length > 1;
  const importButtonDisabled =
    loading ||
    loadingCampuses ||
    Boolean(campusLoadError) ||
    (shouldShowCampusSelect && !campus.trim());

  const uploadButtonClass =
    `uploadButton ${loading ? "uploadButtonLoading" : ""}`.trim();
  const messageClass = `message ${
    message.includes("klaar") || message.includes("opgeslagen")
      ? "messageSuccess"
      : "messageError"
  }`.trim();

  return (
    <>
      <h1 className="title">Excel file toevoegen</h1>

      <p className="text">
        Hieronder vind u een link naar een template om boeken toe te voegen
      </p>

      <a href="/BoekenlijstTemplate.xlsx" download className="downloadLink">
        Download Excelbestand
      </a>

      {shouldShowCampusSelect && (
        <>
          <label className="campusField">
            Campus
            <select
              value={campus}
              onChange={(e) => setCampus(e.target.value)}
              className="campusInput"
              disabled={loadingCampuses}
            >
              <option value="">
                {loadingCampuses ? "Campussen laden..." : "Kies een campus"}
              </option>

              {campuses.map((campusOption) => (
                <option key={campusOption.id} value={campusOption.name}>
                  {campusOption.name}
                </option>
              ))}
            </select>
          </label>
          
          <p className="helperText">
              Deze campus wordt toegepast op alle boeken in dit Excelbestand. Nieuwe campussen maak je enkel aan op de schoolbeheerpagina.
          </p>
        </>
      )}
      {campusLoadError && <p className="fieldError">{campusLoadError}</p>}
        <p className="spacedText">
          Voeg hieronder de aangevulde excel file toe
        </p>

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
                disabled={importButtonDisabled}
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
          <p>Opgeslagen boeken: {importResult.savedCount}</p>
          <p>Mismatches / fouten: {importResult.mismatchCount}</p>

          {importResult.mismatches.length > 0 && (
            <div className="mismatchSection">
              <p>Problemen gevonden in deze rijen:</p>
              <ul>
                {importResult.mismatches.map((mismatch, index) => (
                  <li className="mismatchElement" key={`${mismatch.rowNumber}-${mismatch.isbn}-${index}`}>
                    <p className="mismatchTitle">Rij {mismatch.rowNumber}: {mismatch.isbn} | {mismatch.excelTitle} |
                    {" "}Reden: {mismatch.reason}</p>{mismatch.reason.includes("De titel komt niet overeen") && (
                      <button
                        className="mismatchButton"
                        onClick={() => addSingleBook(mismatch)}
                        disabled={loading}
                      >
                        Toch opslaan
                      </button>
                    )}
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
              <h2>Bestaande ISBN&apos;s gevonden</h2>
              <p>
                Deze ISBN&apos;s bestaan al in de database. Kies per boek of je de
                aantallen wilt toevoegen aan het bestaande boek.
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
                          +{warning.totalCopiesToAdd} totaal / +{warning.availableCopiesToAdd}{" "}
                          beschikbaar
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
