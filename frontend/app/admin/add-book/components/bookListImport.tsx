"use client";

import { useEffect, useState } from "react";
import "./bookListImport.css";
import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";
import type { MeResponse } from "@/app/interfaces/user";
import { fetchSchoolCampuses } from "@/app/utils/schoolCampuses";

type ImportMismatch = {
  rowNumber: number;
  isbn: string;
  excelTitle: string;
  fetchedTitle: string | null;
  reason: string;
  amount: number | null;
};

type ImportResult = {
  totalRows: number;
  savedCount: number;
  mismatchCount: number;
  mismatches: ImportMismatch[];
};

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export default function BookListImport() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [message, setMessage] = useState("");
  const [campusLoadError, setCampusLoadError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [campus, setCampus] = useState("");
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);

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
    setMessage("");
  };

  const handleUploadExcel = async () => {
    if (!selectedFile) return;

    setLoading(true);
    setMessage("");
    setImportResult(null);

    try {
      const formData = new FormData();
      formData.append("file", selectedFile);

      const trimmedCampus = campus.trim();
      if(trimmedCampus) {
        formData.append("campus", trimmedCampus);
      }

      const response = await fetch(`${API_URL}/books/import`, {
        method: "POST",
        credentials:"include",
        body: formData,
      });

      const data = await response.json();

      if (!response.ok) {
        setMessage(data?.message || "Er ging iets mis bij het importeren.");
        return;
      }

      setImportResult(data);
      setMessage(`Import klaar. ${data.savedCount} boek(en) opgeslagen.`);
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken.");
    } finally {
      setLoading(false);
    }
  };

   const addSingleBook = async (mismatch: ImportMismatch) => {
      setLoading(true);

      try {
        const params = new URLSearchParams();

        const trimmedCampus = campus.trim();

        if (trimmedCampus) {
          params.set("campus", trimmedCampus);
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
        setMessage(errorText || "Er ging iets mis bij het opslaan van het boek.");
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
      <h1 className="title">Excel file toevoegen</h1>

      <p className="text">
        Hieronder vind u een link naar een template om boeken toe te voegen.
      </p>

      <a href="/BoekenlijstTemplate.xlsx" download className="downloadLink">
        Download Excelbestand
      </a>

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

      <p className="helperText">
        Deze campus wordt toegepast op alle boeken in dit Excelbestand.
      </p>

        <p className="spacedText">
          Voeg hieronder de aangevulde excel file toe.
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
                onClick={handleUploadExcel}
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

      {message && <div className={messageClass}>{message}</div>}
    </>
  );
}
