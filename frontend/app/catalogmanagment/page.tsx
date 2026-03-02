"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  //houdt bij welk boeken de gebruiker wil verwijderen -> als dit op null staat is er geen boek geselecteerd en is de extra modal gesloten
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showConfirm, setShowConfirm] = useState<boolean>(false);
  //houdt bij of er een delete request bezig is -> zo ja dan wordt de delete knop uitgeschakeld
  const [deleting, setDeleting] = useState<boolean>(false);

  useEffect(() => {
    fetch("http://localhost:8080/catalog/all")
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, []);

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
    //knop uitschakelen omdat er een request bezig is
    setDeleting(true);

    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(`http://localhost:8080/catalog/delete/${id}`, {
            method: "DELETE",
          }),
        ),
      );

      //haalt boek weg zonder full page refresh
      setBooks((prev) => prev.filter((b) => !selectedIds.has(b.id)));
      setSelectedIds(new Set());
      setShowConfirm(false);
    } catch (error) {
      console.log(error);
    } finally {
      //knop altijd terug inschakelen
      setDeleting(false);
    }
  };

  return (
    <div style={{ position: "relative", zIndex: 1, marginTop: "-100vh" }}>
      <ul>
        {books.map((book) => (
          <li key={book.id}>
            <input
              type="checkbox"
              checked={selectedIds.has(book.id)}
              onChange={() => toggleSelect(book.id)}
              style={{ marginRight: "0.5rem" }}
            />
            {book.title}
          </li>
        ))}
      </ul>
      {selectedIds.size > 0 && (
        <button
          onClick={() => setShowConfirm(true)}
          disabled={selectedIds.size === 0}
          style={{ background: "red", color: "white" }}
        >
          wil je deze {selectedIds.size} verwijderen?
        </button>
      )}

      {/* modal wordt alleen gerenderd als bookToDelete niet null is */}
      {showConfirm && (
        <div
          style={{
            position: "fixed",
            // top: 0, left:0,
            width: "100%",
            height: "100%",
            backgroundColor: "rgba(0,0,0,0.5)",
          }}
        >
          <p>Ben je zeker dat je ze wilt verwijderen </p>
          <strong>
            {selectedIds.size} book{selectedIds.size > 1 ? "s" : ""}
          </strong>
          ?<p>Deze actie is onterugkeerbaar!</p>
          <div>
            {/* bij het klikken wordt bookToDelete terug op null gezet -> modal sluit */}
            <button onClick={() => setShowConfirm(false)} disabled={deleting}>
              Ga terug
            </button>
            {/* delete-knop  wordt uitgeschakeld tijdens de request*/}
            <button
              onClick={tryDelete}
              disabled={deleting}
              style={{
                background: "grey",
                color: "white",
                borderRadius: "4px",
              }}
            >
              {deleting ? "deleting..." : "Confirm Delete"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
