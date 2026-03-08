"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";
import BookCard from "./bookCard";
import "./bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  const [activeTab, setActiveTab] = useState("Catalogus");

  //houdt bij welk boeken de gebruiker wil verwijderen -> als dit op null staat is er geen boek geselecteerd en is de extra modal gesloten
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  //toont bevestings modal wanneer true -> vanaf dat er boeken geselcteerd zijn
  const [showConfirm, setShowConfirm] = useState<boolean>(false);
  //houdt bij of er een delete request bezig is -> zo ja dan wordt de delete knop uitgeschakeld
  const [deleting, setDeleting] = useState<boolean>(false);

  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/all`)
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
    //knop uitschakelen omdat er een request bezig is
    setDeleting(true);

    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}`, {
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

  const setSpotlight = async () => {
    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}/spotlight?value=true`, {
            method: "PATCH",
          }),
        ),
      );
      setSelectedIds(new Set());
    } catch (error) {
      console.log(error);
  };
}


  return (
    <main>
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
        {selectedIds.size > 0 && (
          <li onClick={() => setSpotlight()}>
            {selectedIds.size} boek(en) in kijker zetten
          </li>
        )}
      </ul>
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

      {/* modal wordt alleen gerenderd als showConfirm true is*/}
      {showConfirm && (
        <div className="modalOverlay">
          <div className="modalBox">
            <p>Ben je zeker dat je deze wilt verwijderen?</p>
            <p>Deze actie is onterugkeerbaar!</p>

            <div>
              {/* bij klikken sluit de modal */}
              <button onClick={() => setShowConfirm(false)} disabled={deleting}>
                Ga terug
              </button>
              {/* delete-knop  wordt uitgeschakeld tijdens de request*/}
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
