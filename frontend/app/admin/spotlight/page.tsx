"use client";

import { useEffect, useState } from "react";
import { Book } from "../../interfaces/Book";
import BookCard from "../../catalog/bookCard";
import "../../catalog/bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);

  //houdt bij welk boeken de gebruiker wil verwijderen -> als dit op null staat is er geen boek geselecteerd en is de extra modal gesloten
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [selectAllBox, setSelectAllBox] = useState(false);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/spotlight/all`, {
      credentials: "include",
    })
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, []);

  //voegt een boek toe aan de selectie die verwijderd moet worden (selectedIds) als deze er al in zit wordt het boek verwijdert uit de selectie
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

      //haalt boek weg zonder full page refresh
      setBooks((prev) => prev.filter((b) => !selectedIds.has(b.id)));
      setSelectedIds(new Set());
    } catch (error) {
      console.error(error);
    } finally {
      setSelectAllBox(false);
    }
  };

  const selectAll = () => {
    if (!selectAllBox) {
      setSelectedIds(new Set(books.map((book) => book.id)));
      setSelectAllBox(true);
    } else {
      setSelectedIds(new Set());
      setSelectAllBox(false);
    }
  };

  return (
    <main id="spotlightPage">
      <h1>In de kijker</h1>
      <ul>
        {selectedIds.size > 0 && (
          <li onClick={() => tryDelete()}>
            Verwijder {selectedIds.size} boek(en) uit de kijker
          </li>
        )}
      </ul>
      <div className="selectAllCheckbox">
        <input type="checkbox" checked={selectAllBox} onChange={selectAll} />{" "}
        <p>Select all</p>
      </div>
      <div id="bookList">
        {books.map((book) => (
          <BookCard
            key={book.id}
            book={book}
            isSelected={selectedIds.has(book.id)}
            onToggle={() => toggleSelect(book.id)}
          />
        ))}
      </div>
    </main>
  );
}
