"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";
import BookCard from "./bookCard";
import "./bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  const [activeTab, setActiveTab] = useState("Catalogus");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Book[]>([]);

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showConfirm, setShowConfirm] = useState<boolean>(false);
  const [deleting, setDeleting] = useState<boolean>(false);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/all`)
      .then((res) => res.json())
      .then((data: Book[]) => {
        setBooks(data);
        setResults(data);
      });
  }, []);

  useEffect(() => {
    if (!query || query.trim() === "") {
      setResults(books);
      return;
    }

    const delay = setTimeout(async () => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/books/search?query=${query}`,
      );
      const data = await response.json();
      setResults(data);
    }, 300);

    return () => clearTimeout(delay);
  }, [query, books]);

  const toggleSelect = (id: number) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const tryDelete = async () => {
    setDeleting(true);

    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}`, {
            method: "DELETE",
          }),
        ),
      );

      setBooks((prev) => prev.filter((b) => !selectedIds.has(b.id)));
      setSelectedIds(new Set());
      setShowConfirm(false);
    } catch (error) {
      console.log(error);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <main>
      <div className="filterSection">
        <div className="catalogSearchbar">
          <input
            type="text"
            placeholder="Titel, auteur, genre, onderwerp"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <button id="searchButton" aria-label="Zoeken">
            🔎︎
          </button>
        </div>
      </div>

      <h1>Catalogus</h1>
      <ul>
        <li
          className={activeTab === "Catalogus" ? "active" : ""}
          onClick={() => setActiveTab("Catalogus")}
        >
          Catalogus
        </li>
        <li
          className={activeTab === "Boek toevoegen" ? "active" : ""}
          onClick={() => setActiveTab("Boek toevoegen")}
        >
          Boek toevoegen
        </li>

        {selectedIds.size > 0 && (
          <li onClick={() => setShowConfirm(true)}>
            Verwijder {selectedIds.size} boek(en)
          </li>
        )}
      </ul>
      <div id="bookList">
        {results.map((book) => (
          <BookCard
            key={book.id}
            book={book}
            isSelected={selectedIds.has(book.id)}
            onToggle={() => toggleSelect(book.id)}
          />
        ))}
      </div>

      {showConfirm && (
        <div className="modalOverlay">
          <div className="modalBox">
            <p>Ben je zeker dat je deze wilt verwijderen?</p>
            <p>Deze actie is onterugkeerbaar!</p>

            <div>
              <button onClick={() => setShowConfirm(false)} disabled={deleting}>
                Ga terug
              </button>
              <button
                className="confirmButton"
                onClick={tryDelete}
                disabled={deleting}
              >
                {deleting ? "deleting..." : "Bevestig"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}
