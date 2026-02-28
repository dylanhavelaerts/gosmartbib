"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  //houdt bij welk boek de gebruiker wil verwijderen -> als dit op null staat is er geen boek geselecteerd en is de extra modal gesloten
  const [bookToDelete, setBookToDelete] = useState<Book | null>(null);
  //houdt bij of er een delete request bezig is -> zo ja dan wordt de delete knop uitgeschakeld
  const [deleting, setDeleting] = useState<boolean>(false);

  useEffect(() => {
    fetch("http://localhost:8080/catalog/all")
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, []);

  const tryDelete = async () => {
    if (!bookToDelete) return;

    //knop uitschakelen omdat er een request bezig is
    setDeleting(true);

    try {
      const res = await fetch(
        `http://localhost:8080/catalog/delete/${bookToDelete.id}`,
        { method: "DELETE" },
      );

      if (res.ok) {
        //haalt boek weg zonder full page refresh
        setBooks((prev) => prev.filter((b) => b.id !== bookToDelete.id));
        setBookToDelete(null);
      } else {
        console.log("failed to delete book: " + bookToDelete.title);
      }
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
            {book.title}
            <button
              onClick={() => setBookToDelete(book)}
              style={{ marginLeft: "1rem", color: "red" }}
            >
              Delete book
            </button>
          </li>
        ))}
      </ul>

      {/* modal wordt alleen gerenderd als bookToDelete niet null is */}
      {bookToDelete && (
        <div
          style={{
            position: "fixed",
            // top: 0, left:0,
            width: "100%",
            height: "100%",
            backgroundColor: "rgba(0,0,0,0.5)",
          }}
        >
          <p>Are you sure you want me to delete </p>
          <strong>{bookToDelete.title}</strong>?
          <p>This action cannot be undone</p>
          <div>
            {/* bij het klikken wordt bookToDelete terug op null gezet -> modal sluit */}
            <button onClick={() => setBookToDelete(null)} disabled={deleting}>
              Cancel
            </button>
            {/* delete-knop  wordt uitgeschakeld tijdens de request*/}
            <button
              onClick={tryDelete}
              disabled={deleting}
              style={{ background: "red", color: "white", borderRadius: "4px" }}
            >
              {deleting ? "deleting..." : "Confirm Delete"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
