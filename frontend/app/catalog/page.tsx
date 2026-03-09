"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
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
  const [language, setLanguage] = useState("");
  const [categories, setCategories] = useState<Set<string>>(new Set());
  const [minPages, setMinPages] = useState("");
  const [maxPages, setMaxPages] = useState("");
  const [minYear, setMinYear] = useState("");
  const [maxYear, setMaxYear] = useState("");
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

  /**
   * Fetcht alle boeken bij het laden van de pagina.
   * Als er nog geen zoekquery is, zet deze boeken dan ook als resultaten (om de volledige catalogus te tonen).
   */
  useEffect(() => {
    const params = new URLSearchParams();
    if (language) params.append("language", language);
    if (categories.size > 0) {
      categories.forEach((cat) => params.append("categories", cat));
    }
    if (minPages) params.append("minPageCount", minPages);
    if (maxPages) params.append("maxPageCount", maxPages);
    if (minYear) params.append("minPubYear", minYear);
    if (maxYear) params.append("maxPubYear", maxYear);

    const filterQuery = params.toString();
    const url = filterQuery
      ? `${process.env.NEXT_PUBLIC_API_URL}/books/filter?${filterQuery}`
      : `${process.env.NEXT_PUBLIC_API_URL}/books/all`;

    fetch(url)
      .then((res) => res.json())
      .then((data: Book[]) => {
        setBooks(data);
        if (!query) setResults(data);
      });
  }, [language, categories, minPages, maxPages, minYear, maxYear]);

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
          fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}`, {
            method: "DELETE",
          }),
        ),
      );

      setBooks((prev) => prev.filter((b) => !selectedIds.has(b.id)));
      setSelectedIds(new Set());
      setShowConfirm(false);
    } catch (error) {
      console.log(error); //later beter error handling
    } finally {
      setDeleting(false);
    }
  };

  const toggleCategory = (cat: string) => {
    setCategories((prev) => {
      const next = new Set(prev);
      next.has(cat) ? next.delete(cat) : next.add(cat);
      return next;
    });
  };

  return (
    <main className="pageLayout">
      {sidebarOpen && (
        <div className="sidebarOverlay" onClick={() => setSidebarOpen(false)} />
      )}

      <aside className={`filterSidebar ${sidebarOpen ? "open" : ""}`}>
        <div className="sidebarHeader">
          <span>Filters</span>
          <button onClick={() => setSidebarOpen(false)}>✕</button>
        </div>
        <div className="filterBar">
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="filterSelect"
          >
            <option value="">Alle talen</option>
            <option value="en">EN</option>
          </select>

          <div className="filterDropdown">
            <button
              className="filterDropdownToggle"
              onClick={() => setCategoryOpen(!categoryOpen)}
            >
              Genre {categories.size > 0 ? `(${categories.size})` : ""} ▼
            </button>
            {categoryOpen && (
              <div className="filterDropdownPanel">
                {[
                  "Programming",
                  "Software Engineering",
                  "Best Practices",
                  "Architecture",
                  "Code Quality",
                  "Java",
                  "Spring",
                  "Algorithms",
                  "Computer Science",
                  "JavaScript",
                  "Web Development",
                  "Career",
                  "Design Patterns",
                  "Software Design",
                ].map((cat) => (
                  <label key={cat} className="filterCheckboxLabel">
                    <input
                      type="checkbox"
                      checked={categories.has(cat)}
                      onChange={() => toggleCategory(cat)}
                    />
                    {cat}
                  </label>
                ))}
              </div>
            )}
          </div>

          <input
            type="number"
            placeholder="Min pagina's"
            value={minPages}
            onChange={(e) => setMinPages(e.target.value)}
            className="filterInput"
          />
          <input
            type="number"
            placeholder="Max pagina's"
            value={maxPages}
            onChange={(e) => setMaxPages(e.target.value)}
            className="filterInput"
          />
          <input
            type="number"
            placeholder="Min jaar"
            value={minYear}
            onChange={(e) => setMinYear(e.target.value)}
            className="filterInput"
          />
          <input
            type="number"
            placeholder="Max jaar"
            value={maxYear}
            onChange={(e) => setMaxYear(e.target.value)}
            className="filterInput"
          />
        </div>
      </aside>

      <div className="mainContent">
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
          <li onClick={() => setSidebarOpen(true)}>☰</li>
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
              router.push("/catalog/admin");
            }}
          >
            Naar admin pagina
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
              withCheckbox={false}
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
                <button
                  onClick={() => setShowConfirm(false)}
                  disabled={deleting}
                >
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
      </div>
    </main>
  );
}
