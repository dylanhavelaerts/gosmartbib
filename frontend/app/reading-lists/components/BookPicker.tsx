"use client";

import { useState, useEffect } from "react";
import { Book } from "../../interfaces/Book";
import Pagination from "../../catalog/pagination";
import "../create/createReadingList.css";

const PAGE_SIZE = 10;

interface BookPickerProps {
  selectedBooks: Book[];
  onAdd: (book: Book) => void;
  onRemove: (bookId: number) => void;
  label?: string;
}

export default function BookPicker({
  selectedBooks,
  onAdd,
  onRemove,
  label = "Zoek boeken",
}: BookPickerProps) {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Book[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const isSearching = query.trim() !== "";
    const params = new URLSearchParams({
      page: String(currentPage - 1),
      size: String(PAGE_SIZE),
    });
    const url = isSearching
      ? `${apiUrl}/books/search?query=${encodeURIComponent(query.trim())}&${params}`
      : `${apiUrl}/books/all?${params}`;

    const delay = isSearching ? 300 : 0;
    const timer = setTimeout(() => {
      setLoading(true);
      setError("");
      fetch(url, { credentials: "include" })
        .then((res) => (res.ok ? res.json() : Promise.reject()))
        .then((data) => {
          setResults(data.content || []);
          setTotalPages(data.totalPages || 0);
        })
        .catch(() => setError("Fout bij het ophalen van boeken."))
        .finally(() => setLoading(false));
    }, delay);

    return () => clearTimeout(timer);
  }, [query, currentPage, apiUrl]);

  return (
    <div>
      <div className="search-container">
        <label className="search-step-label">{label}</label>
        <input
          type="text"
          className="search-input"
          placeholder="Zoek op titel, auteur of ISBN..."
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setCurrentPage(1);
          }}
        />
      </div>

      <div className="list-container">
        {loading && <p className="loading-text">Laden...</p>}
        {error && <p className="loading-text">{error}</p>}
        {!loading && !error && results.length === 0 && query.trim() !== "" && (
          <p className="loading-text">Geen boeken gevonden.</p>
        )}
        {results.length > 0 && (
          <>
            <ul className="book-list">
              {results.map((book) => {
                const isAdded = selectedBooks.some((b) => b.id === book.id);
                return (
                  <li
                    key={book.id}
                    className={`book-list-item ${isAdded ? "added" : ""}`}
                  >
                    <div className="book-list-thumb">
                      {book.thumbnail && book.thumbnail.trim() !== "" ? (
                        <img src={book.thumbnail} alt={book.title} />
                      ) : (
                        <span>Geen cover</span>
                      )}
                    </div>
                    <div className="book-list-info">
                      <h3 className="book-list-title">{book.title}</h3>
                      <p className="book-list-authors">
                        {book.authors ? book.authors.join(", ") : "Onbekend"}
                      </p>
                    </div>
                    <button
                      type="button"
                      className="add-btn"
                      onClick={() => onAdd(book)}
                      disabled={isAdded}
                    >
                      {isAdded ? "Toegevoegd" : "Voeg toe"}
                    </button>
                  </li>
                );
              })}
            </ul>
            {totalPages > 1 && (
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={setCurrentPage}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}
