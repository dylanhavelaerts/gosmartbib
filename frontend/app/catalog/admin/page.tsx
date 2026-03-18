"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Book } from "@/app/interfaces/Book";
import BookCard from "../bookCard";
import "../bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  const [activeTab, setActiveTab] = useState("Catalogus");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Book[]>([]);

  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [showConfirm, setShowConfirm] = useState<boolean>(false);
  const [deleting, setDeleting] = useState<boolean>(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  /**
   * Fetcht alle boeken bij het laden van de pagina.
   * Als er nog geen zoekquery is, zet deze boeken dan ook als resultaten (om de volledige catalogus te tonen).
   */
  useEffect(() => {
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/all/unpaged`)
      .then((res) => res.json())
      .then((data) => {
        const safeData = Array.isArray(data) ? data : [];

        setBooks(safeData);

        if (!query) {
          setResults(safeData);
        }
      });
  }, []);

  /**
   * Als er een 'search' query param is,
   * gebruik deze dan als zoekquery en sla deze op in sessionStorage (zodat deze behouden blijft bij page-refresh).
   */
  useEffect(() => {
    const incoming = searchParams.get("search");
    if (incoming) {
      setQuery(incoming);
      sessionStorage.setItem("catalogSearch", incoming);
      router.replace(pathname);
    } else {
      /**
       * Als er geen 'search' query param is, maar er is wel een opgeslagen zoekquery in sessionStorage,
       * gebruik deze dan. (search blijft bij page-refresh)
       */
      const saved = sessionStorage.getItem("catalogSearch");
      if (saved) {
        setQuery(saved);
      }
    }
  }, [searchParams]);

  /**
   * Wanneer de zoekquery verandert, of wanneer de boekenlijst verandert (bijvoorbeeld na het verwijderen van boeken),
   * voer dan een nieuwe zoekopdracht uit. Als de zoekquery leeg is, toon dan alle boeken.
   */
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

  /**
   * Toggle of een boek geselecteerd is of niet, op basis van zijn ID. (voor het verwijderen van meerdere boeken tegelijk)
   */
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

  /**
   * Probeer de geselecteerde boeken te verwijderen. Stuur voor elk geselecteerd boek een DELETE request naar de API.
   */
  const tryDelete = async () => {
    setDeleting(true);

    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/${id}`, {
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

  const setSpotlight = async () => {
    try {
      await Promise.all(
        Array.from(selectedIds).map((id) =>
          fetch(
            `${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}/spotlight?value=true`,
            {
              method: "PATCH",
            },
          ),
        ),
      );
      setSelectedIds(new Set());
    } catch (error) {
      console.log(error);
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
            // Saved de query in state en sessionStorage (zodat deze behouden blijft bij page-refresh)
            onChange={(e) => {
              setQuery(e.target.value);
              if (e.target.value) {
                sessionStorage.setItem("catalogSearch", e.target.value);
              } else {
                sessionStorage.removeItem("catalogSearch");
              }
            }}
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
          onClick={() => {
            setActiveTab("Boek toevoegen");
            router.push("../../add-book");
          }}
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
            Voeg {selectedIds.size} boek(en) toe aan kijker
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
