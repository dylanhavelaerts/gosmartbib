"use client";

import { useEffect, useMemo, useState } from "react";
import type { Book } from "../../interfaces/Book";
import BookCard from "../../catalog/bookCard";
import {
  formatReadingLevelTitle,
  groupBooksByReadingLevel,
} from "@/app/utils/bookReadingLevels";
import "./spotlight.css";
import ProtectedRoute from "@/app/components/ProtectedRoute";

export default function SpotlightPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());

  const booksByReadingLevel = useMemo(
    () => groupBooksByReadingLevel(books),
    [books],
  );

  const allBooksSelected = books.length > 0 && selectedIds.size === books.length;

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/spotlight/all`, {
      credentials: "include",
    })
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data))
      .catch((error) => console.error(error));
  }, []);

  const toggleSelect = (id: number) => {
    setSelectedIds((previousSelectedIds) => {
      const nextSelectedIds = new Set(previousSelectedIds);

      if (nextSelectedIds.has(id)) {
        nextSelectedIds.delete(id);
      } else {
        nextSelectedIds.add(id);
      }

      return nextSelectedIds;
    });
  };

  const selectAll = () => {
    if (allBooksSelected) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(books.map((book) => book.id)));
    }
  };

  const tryDelete = async () => {
    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(
            `${process.env.NEXT_PUBLIC_API_URL}/books/${id}/spotlight?value=false`,
            {
              credentials: "include",
              method: "PATCH",
            },
          ),
        ),
      );

      setBooks((previousBooks) =>
        previousBooks.filter((book) => !selectedIds.has(book.id)),
      );

      setSelectedIds(new Set());
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER"]}>
      <main className="spotlightAdminPage">
        <header className="spotlightAdminHeader">
          <div>
            <p className="spotlightAdminEyebrow">Beheer</p>
            <h1>In de kijker</h1>
          </div>
        </header>

        <section className="spotlightToolbar">
          <div className="spotlightToolbarLeft">
            <label className="spotlightSelectAll">
              <input
                type="checkbox"
                checked={allBooksSelected}
                onChange={selectAll}
              />
              <span>Alles selecteren</span>
            </label>
          </div>

          {selectedIds.size > 0 && (
            <button
              type="button"
              className="spotlightDeleteButton"
              onClick={tryDelete}
            >
              Verwijder {selectedIds.size} boek
              {selectedIds.size === 1 ? "" : "en"}
            </button>
          )}
        </section>

        {booksByReadingLevel.length > 0 ? (
          <div className="spotlightReadingLevelSections">
            {booksByReadingLevel.map((section) => (
              <section
                key={section.readingLevel}
                className="spotlightReadingLevelSection"
              >
                <div className="spotlightReadingLevelHeader">
                  <h2>{formatReadingLevelTitle(section.readingLevel)}</h2>

                  <span>
                    {section.books.length} boek
                    {section.books.length === 1 ? "" : "en"}
                  </span>
                </div>

                <div className="spotlightReadingLevelBookGrid">
                  {section.books.map((book) => (
                    <BookCard
                      key={book.id}
                      book={book}
                      isSelected={selectedIds.has(book.id)}
                      onToggle={() => toggleSelect(book.id)}
                    />
                  ))}
                </div>
              </section>
            ))}
          </div>
        ) : (
          <section className="spotlightEmptyState">
            <h2>Geen boeken in de kijker</h2>
            <p>Er zijn momenteel geen boeken toegevoegd aan deze selectie.</p>
          </section>
        )}
      </main>
    </ProtectedRoute>
  );
}