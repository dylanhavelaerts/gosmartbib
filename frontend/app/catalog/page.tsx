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
  const [language, setLanguage] = useState("");
  const [categories, setCategories] = useState<Set<string>>(new Set());
  const [minPages, setMinPages] = useState("");
  const [maxPages, setMaxPages] = useState("");
  const [minYear, setMinYear] = useState("");
  const [maxYear, setMaxYear] = useState("");
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams();
    if (language) params.append("language", language);
    if (categories.size > 0) {
      categories.forEach((categories) =>
        params.append("categories", categories),
      );
    }
    if (minPages) params.append("minPageCount", minPages);
    if (maxPages) params.append("maxPageCount", maxPages);
    if (minYear) params.append("minPubYear", minYear);
    if (maxYear) params.append("maxPubYear", maxYear);

    const query = params.toString();
    const url = query
      ? `${process.env.NEXT_PUBLIC_API_URL}/books/filter?${query}`
      : `${process.env.NEXT_PUBLIC_API_URL}/books/all`;

    fetch(url)
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, [language, categories, minPages, maxPages, minYear, maxYear]);

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
      console.log(error); //later beter error handling
    } finally {
      //knop altijd terug inschakelen
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
        <h1>Catalogus</h1>
        <ul>
          <li onClick={() => setSidebarOpen(true)} className="filterToggleBtn">
            ☰
          </li>
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
