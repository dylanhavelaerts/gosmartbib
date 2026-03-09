"use client";

import { useState } from "react";
import { Book } from "../interfaces/Book";
import "./addBook.css";
import { read, utils } from "xlsx";


type ImportedBook = {
  isbn: string;
  title: string;
};

type TabId = "Boek" | "Boekenlijst";

export default function AddBookPage() {
  const [isbn, setIsbn] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState<TabId>("Boek");
  const [previewBook, setPreviewBook] = useState<Book | null>(null);
  const [importedBooks, setImportedBooks] = useState<ImportedBook[]>([]);
  const [fetchedBooks, setFetchedBooks] = useState<Book[]>([]);
  const [mismatchedBooks, setMismatchedBooks] = useState<ImportedBook[]>([])
  const [error, setError] = useState("");

  const cls = (id: TabId) => `tabBtn ${selected === id ? "selectedCategory" : ""}`;

    const handleFileChange = async (
  e: React.ChangeEvent<HTMLInputElement>
) => {
  const file = e.target.files?.[0];
  if (!file) return;

  setError("");
  setImportedBooks([]);

  try {
    const buffer = await file.arrayBuffer();
    const workbook = read(buffer, { type: "array" });

    const sheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[sheetName];

    const rows = utils.sheet_to_json<(string | number)[]>(worksheet, {
      header: 1,
      defval: "",
    });

    const dataRows = rows.slice(1);

    const imported: ImportedBook[] = dataRows
      .map((row) => ({
        isbn: String(row[0] ?? "").trim(),
        title: String(row[1] ?? "").trim(),
      }))
      .filter((book) => book.isbn !== "" && book.title !== "");

    setImportedBooks(imported);
  } catch (err) {
    console.error(err);
    setError("Could not read the Excel file.");
  }
};

  const handleSearchBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isbn) return;

    setLoading(true);
    setMessage("");
    setPreviewBook(null);

    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/books/search/${isbn}`,
      );

      if (response.ok) {
        const data = await response.json();
        setPreviewBook(data);
        setMessage("Boek gevonden! Controleer de gegevens hieronder.");
      } else if (response.status === 404) {
        setMessage("Geen boek gevonden met dit ISBN-nummer bij Google Books.");
      } else {
        setMessage("Er is een onverwachte serverfout opgetreden.");
      }
    } catch (error) {
      setMessage(
        "Kan de server niet bereiken. Controleer of de backend draait.",
      );
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmAdd = async () => {
    if (!isbn) return;
    setLoading(true);
    setMessage("");

    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/books/add/${isbn}`,
        {
          method: "POST",
        },
      );

      if (response.ok) {
        const data = await response.json();
        setMessage(
          `Boek succesvol aan de database toegevoegd: "${data.title}"`,
        );
        setIsbn("");
        setPreviewBook(null);
      } else {
        setMessage("Er ging iets mis bij het opslaan van het boek.");
      }
    } catch (error) {
      setMessage("Kan de server niet bereiken.");
    } finally {
      setLoading(false);
    }
  };

  if(selected === "Boek"){
  return (
    <div
      style={{
        padding: "2rem",
        maxWidth: "600px",
        margin: "0 auto",
        fontFamily: "sans-serif",
      }}
    >
      <nav className="lowerNav">
        <button style={{}}className={cls("Boek")} onClick={() => setSelected("Boek")}>in de kijker</button>
        <button className={cls("Boekenlijst")} onClick={() => setSelected("Boekenlijst")}>Nieuw in bibliotheek</button>
      </nav>
      <h1 style={{ fontSize: "2rem", marginBottom: "1rem" }}>
        Nieuw Boek Toevoegen
      </h1>
      <p style={{ marginBottom: "2rem", color: "#555" }}>
        Scan of typ het ISBN-nummer in. We halen eerst een voorbeeld op voordat
        we het opslaan.
      </p>

      {/* Zoekformulier */}
      <form
        onSubmit={handleSearchBook}
        style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
      >
        <div>
          <label
            htmlFor="isbn"
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            ISBN Nummer:
          </label>
          <input
            id="isbn"
            type="text"
            value={isbn}
            onChange={(e) => setIsbn(e.target.value)}
            placeholder="Bijv. 9781473227989"
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #ccc",
              color: "black",
            }}
            disabled={previewBook !== null}
          />
        </div>

        {/* Alleen tonen als we nog NIET aan het previewen zijn */}
        {!previewBook && (
          <button
            type="submit"
            disabled={loading}
            style={{
              padding: "0.75rem 1.5rem",
              fontSize: "1rem",
              cursor: loading ? "not-allowed" : "pointer",
              backgroundColor: loading ? "#ccc" : "#0070f3",
              color: "white",
              border: "none",
              borderRadius: "4px",
              fontWeight: "bold",
            }}
          >
            {loading ? "Bezig met zoeken..." : "Zoek Boek"}
          </button>
        )}
      </form>

      {/* De Preview Kaart! */}
      {previewBook && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1.5rem",
            border: "2px solid #0070f3",
            borderRadius: "8px",
            backgroundColor: "#f9f9f9",
            color: "black",
          }}
        >
          <h2 style={{ marginTop: 0 }}>Preview van het boek:</h2>
          <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
            {previewBook.thumbnail && (
              <img
                src={previewBook.thumbnail}
                alt="Cover"
                style={{
                  width: "100px",
                  height: "150px",
                  objectFit: "cover",
                  borderRadius: "4px",
                }}
              />
            )}
            <div>
              <p>
                <strong>Titel:</strong> {previewBook.title}
              </p>
              <p>
                <strong>Auteur(s):</strong> {previewBook.authors?.join(", ")}
              </p>
              <p>
                <strong>Uitgeverij:</strong> {previewBook.publisher}
              </p>
              <p>
                <strong>Jaar van uitgave:</strong>{" "}
                {previewBook.publishedYear
                  ? previewBook.publishedYear
                  : "Onbekend"}
              </p>
              <p>
                <strong>ISBN:</strong> {previewBook.isbn}
              </p>
              <p>
                <strong>Pagina's:</strong> {previewBook.pageCount}
              </p>
            </div>
          </div>

          {/* Bevestigingsknoppen */}
          <div style={{ display: "flex", gap: "1rem", marginTop: "1.5rem" }}>
            <button
              onClick={handleConfirmAdd}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#28a745",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              {loading ? "Bezig..." : "Ja, Voeg toe aan Catalogus"}
            </button>
            <button
              onClick={() => {
                setPreviewBook(null);
                setMessage("");
                setIsbn("");
              }}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#dc3545",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              Annuleren
            </button>
          </div>
        </div>
      )}

      {/* Meldingen weergeven */}
      {message && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1rem",
            backgroundColor: message.includes("succesvol")
              ? "#d4edda"
              : message.includes("gevonden!")
                ? "#cce5ff"
                : "#f8d7da",
            color: message.includes("succesvol")
              ? "#155724"
              : message.includes("gevonden!")
                ? "#004085"
                : "#721c24",
            borderRadius: "4px",
          }}
        >
          {message}
        </div>
      )}
    </div>
  );
}


else {
    return (
    <div
      style={{
        padding: "2rem",
        maxWidth: "600px",
        margin: "0 auto",
        fontFamily: "sans-serif",
      }}
    >
      <nav className="lowerNav">
        <button style={{}}className={cls("Boek")} onClick={() => setSelected("Boek")}>in de kijker</button>
        <button className={cls("Boekenlijst")} onClick={() => setSelected("Boekenlijst")}>Nieuw in bibliotheek</button>
      </nav>
      <h1 style={{ fontSize: "2rem", marginBottom: "1rem" }}>
        Excel file toevoegen
      </h1>
      <p style={{ color: "#555" }}>
        Hieronder vind u een link naar een template om boeken toe te voegen.
      </p>
      <a href="\BoekenlijstTemplate.xlsx" download>
        Download Excelbestand
      </a>
      <p style={{ color: "#555" }}>
        Voeg hieronder de aangevulde excel file toe.
      </p>
    <div className="fileInputBox">
      <input
        className="fileInput"
        type="file"
        accept=".xlsx,.xls"
        onChange={handleFileChange}
      />

      {error && <p>{error}</p>}
    </div>
    { importedBooks.length !== 0 &&(
      <div>
        <p>Deze ISBN nummers komen niet overeen met hun titel:</p>
        <ul>
          {importedBooks.map((book, index) => (
            <li key={book.isbn || index}>
              {book.isbn} | {book.title}
            </li>
          ))}
        </ul>
      </div>)
      }
      <div>
          {/* Bevestigingsknoppen */}
          { importedBooks.length !== 0 &&(
          <div style={{ display: "flex", gap: "1rem", marginTop: "1.5rem" }}>
            <button
              onClick={handleConfirmAdd}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#28a745",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              {loading ? "Bezig..." : "Ja, Voeg toe aan Catalogus"}
            </button>
            <button
              onClick={() => {
                setPreviewBook(null);
                setMessage("");
                setIsbn("");
              }}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#dc3545",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              Annuleren
            </button>
          </div>
 )} </div>

      {/* Meldingen weergeven */}
      {message && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1rem",
            backgroundColor: message.includes("succesvol")
              ? "#d4edda"
              : message.includes("gevonden!")
                ? "#cce5ff"
                : "#f8d7da",
            color: message.includes("succesvol")
              ? "#155724"
              : message.includes("gevonden!")
                ? "#004085"
                : "#721c24",
            borderRadius: "4px",
          }}
        >
          {message}
        </div>
      )}
    </div>
    );
}
}


