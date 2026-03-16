"use client";

import { useState } from "react";

type ImportMismatch = {
  rowNumber: number;
  isbn: string;
  excelTitle: string;
  fetchedTitle: string | null;
  reason: string;
};

type ImportResult = {
  totalRows: number;
  savedCount: number;
  mismatchCount: number;
  mismatches: ImportMismatch[];
};

export default function BookListImport() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

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

      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/books/import`,
        {
          method: "POST",
          body: formData,
        }
      );

      const data = await response.json();

      if (!response.ok) {
        setMessage(data?.message || "Er ging iets mis bij het importeren.");
        return;
      }

      setImportResult(data);
      setMessage(`Import klaar. ${data.savedCount} boeken opgeslagen.`);
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <h1 style={{ fontSize: "2rem", marginBottom: "1rem" }}>
        Excel file toevoegen
      </h1>

      <p style={{ color: "#555" }}>
        Hieronder vind u een link naar een template om boeken toe te voegen.
      </p>

      <a href="/BoekenlijstTemplate.xlsx" download>
        Download Excelbestand
      </a>

      <p style={{ color: "#555", marginTop: "1rem" }}>
        Voeg hieronder de aangevulde excel file toe.
      </p>

      <div className="fileInputBox">
        <input
          className="fileInput"
          type="file"
          accept=".xlsx,.xls"
          onChange={handleFileChange}
        />
      </div>

      {selectedFile && (
        <div style={{ marginTop: "1rem" }}>
          <p>Geselecteerd bestand: {selectedFile.name}</p>

          <button
            type="button"
            onClick={handleUploadExcel}
            disabled={loading}
            style={{
              padding: "0.75rem 1.5rem",
              backgroundColor: loading ? "#ccc" : "#28a745",
              color: "white",
              border: "none",
              borderRadius: "4px",
              fontWeight: "bold",
              cursor: loading ? "not-allowed" : "pointer",
            }}
          >
            {loading ? "Bezig met importeren..." : "Importeer Excelbestand"}
          </button>
        </div>
      )}

      {importResult && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1rem",
            border: "1px solid #8e2446",
            borderRadius: "8px",
            backgroundColor: "#f9f9f9",
            color: "black",
          }}
        >
          <h2>Import resultaat</h2>
          <p>Totaal aantal rijen: {importResult.totalRows}</p>
          <p>Opgeslagen boeken: {importResult.savedCount}</p>
          <p>Mismatches / fouten: {importResult.mismatchCount}</p>

          {importResult.mismatches.length > 0 && (
            <div style={{ marginTop: "1rem" }}>
              <p>Problemen gevonden in deze rijen:</p>
              <ul>
                {importResult.mismatches.map((mismatch, index) => (
                  <li key={`${mismatch.rowNumber}-${mismatch.isbn}-${index}`}>
                    Rij {mismatch.rowNumber}: {mismatch.isbn} |{" "}
                    {mismatch.excelTitle} | Reden: {mismatch.reason}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}

      {message && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1rem",
            backgroundColor:
              message.includes("klaar") || message.includes("opgeslagen")
                ? "#d4edda"
                : "#f8d7da",
            color:
              message.includes("klaar") || message.includes("opgeslagen")
                ? "#155724"
                : "#721c24",
            borderRadius: "4px",
          }}
        >
          {message}
        </div>
      )}
    </>
  );
}