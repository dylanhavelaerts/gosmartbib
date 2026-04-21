"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Book, BOOK_CATEGORIES, BOOK_LABELS } from "../interfaces/Book";
import BookCard from "./bookCard";
import Pagination from "./pagination";
import "./bookList.css";
import { useAuth } from "../context/AuthContext";
import StarRating from "../components/reviewsection/StarRating";

export default function Home() {
  // -- States ------------------------------------------------------------------------------------------------------------------------------

  // Boek data
  const [books, setBooks] = useState<Book[]>([]);
  const [query, setQuery] = useState("");

  // Paging
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // Filters
  const [language, setLanguage] = useState("");
  const [categories, setCategories] = useState<Set<string>>(new Set());
  const [labels, setLabels] = useState<Set<string>>(new Set());
  const [minPages, setMinPages] = useState("");
  const [maxPages, setMaxPages] = useState("");
  const [minYear, setMinYear] = useState("");
  const [maxYear, setMaxYear] = useState("");
  const [minRating, setMinRating] = useState<number | null>(null);
  const [maxRating, setMaxRating] = useState<number | null>(null);

  // UI state
  const [activeTab, setActiveTab] = useState("Catalogus");
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [labelOpen, setLabelOpen] = useState(false);

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const { user } = useAuth();

  // -- URL zoek aspect --------------------------------------------------------------------------------------------------------------------
  // Leest de ?search query param bij het laden van de pagina en zet deze als zoekquery.
  // Slaat de zoekquery ook op in sessionStorage zodat deze behouden blijft bij page-refresh.

  useEffect(() => {
    const incoming = searchParams.get("search");
    if (incoming) {
      setQuery(incoming);
      sessionStorage.setItem("catalogSearch", incoming);
      router.replace(pathname);
    } else {
      const saved = sessionStorage.getItem("catalogSearch");
      if (saved) setQuery(saved);
    }
  }, [searchParams]);

  // -- Fetch boeken ------------------------------------------------------------------------------------------------------------------------------
  // Rent filters en zoekquery uit als dependencies zodat er automatisch een nieuwe fetch wordt gedaan bij verandering.
  // Bij het fetchen wordt ook rekening gehouden met de huidige pagina en aantal items per pagina (pageSize).
  // Bij een zoekopdracht is er een kleine debounce (300ms) om onnodige fetches te voorkomen tijdens het typen.

  useEffect(() => {
    const params = new URLSearchParams();
    params.append("page", String(currentPage - 1)); // backend is 0-based
    params.append("size", String(pageSize));

    const isSearching = query && query.trim() !== "";
    const hasFilters =
      language ||
      categories.size > 0 ||
      labels.size > 0 ||
      minPages ||
      maxPages ||
      minYear ||
      maxYear ||
      minRating !== null ||
      maxRating !== null;

    let url: string;

    if (isSearching || hasFilters) {
      if (isSearching) params.append("query", query.trim());
      if (language) params.append("language", language);
      categories.forEach((cat) => params.append("categories", cat));
      labels.forEach((label) => params.append("labels", label));
      if (minPages) params.append("minPageCount", minPages);
      if (maxPages) params.append("maxPageCount", maxPages);
      if (minYear) params.append("minPubYear", minYear);
      if (maxYear) params.append("maxPubYear", maxYear);
      if (minRating !== null) params.append("minRating", minRating.toString());
      if (maxRating !== null) params.append("maxRating", maxRating.toString());
      url = `${process.env.NEXT_PUBLIC_API_URL}/books/filter?${params}`;
    } else {
      url = `${process.env.NEXT_PUBLIC_API_URL}/books/all?${params}`;
    }

    const delay = isSearching ? 300 : 0;
    const timer = setTimeout(() => {
      fetch(url, { credentials: "include" })
        .then((res) => res.json())
        .then((data) => {
          setBooks(data.content);
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements);
        });
    }, delay);

    return () => clearTimeout(timer);
  }, [
    language,
    categories,
    labels,
    minPages,
    maxPages,
    minYear,
    maxYear,
    query,
    currentPage,
    pageSize,
    minRating,
    maxRating,
  ]);

  // -- Helper methods --------------------------------------------------------------------------------------------------------------

  const toggleCategory = (cat: string) => {
    setCategories((prev) => {
      const next = new Set(prev);
      next.has(cat) ? next.delete(cat) : next.add(cat);
      return next;
    });
    setCurrentPage(1);
  };

  const toggleLabel = (label: string) => {
    setLabels((prev) => {
      const next = new Set(prev);
      next.has(label) ? next.delete(label) : next.add(label);
      return next;
    });
    setCurrentPage(1);
  };

  const handleQueryChange = (value: string) => {
    setQuery(value);
    setCurrentPage(1);
    if (value) {
      sessionStorage.setItem("catalogSearch", value);
    } else {
      sessionStorage.removeItem("catalogSearch");
    }
  };

  // -- Visueel aspect ----------------------------------------------------------------------------------------------------------------------------------

  return (
    <main className="pageLayout">
      {/* Filter bar */}
      <aside className="filterSidebar">
        <div className="sidebarHeader">
          <span>Filters</span>
        </div>

        <div className="filterBar">
          <select
            value={language}
            onChange={(e) => {
              setLanguage(e.target.value);
              setCurrentPage(1);
            }}
            className="filterSelect"
          >
            <option value="">Alle talen</option>
            <option value="en">EN</option>
          </select>

          <div className="filterDropdown">
            <button
              className="filterDropdownToggle"
              onClick={() => {
                setCategoryOpen(!categoryOpen);
                if (labelOpen) {
                  setLabelOpen(false);
                }
              }}
            >
              Genre {categories.size > 0 ? `(${categories.size})` : ""} ▼
            </button>
            {categoryOpen && (
              <div className="filterDropdownPanel">
                {BOOK_CATEGORIES.map((cat) => (
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

          <div className="filterDropdown">
            <button
              className="filterDropdownToggle"
              onClick={() => setLabelOpen(!labelOpen)}
            >
              label {labels.size > 0 ? `(${labels.size})` : ""} ▼
            </button>
            {labelOpen && (
              <div className="filterDropdownPanel">
                {BOOK_LABELS.map((label) => (
                  <label key={label} className="filterCheckboxLabel">
                    <input
                      type="checkbox"
                      checked={labels.has(label)}
                      onChange={() => toggleLabel(label)}
                    />
                    {label}
                  </label>
                ))}
              </div>
            )}
          </div>

          <input
            type="number"
            placeholder="Min pagina's"
            value={minPages}
            onChange={(e) => {
              setMinPages(e.target.value);
              setCurrentPage(1);
            }}
            className="filterInput"
          />
          <input
            type="number"
            placeholder="Max pagina's"
            value={maxPages}
            onChange={(e) => {
              setMaxPages(e.target.value);
              setCurrentPage(1);
            }}
            className="filterInput"
          />

          <input
            type="number"
            placeholder="Min jaar"
            value={minYear}
            onChange={(e) => {
              setMinYear(e.target.value);
              setCurrentPage(1);
            }}
            className="filterInput"
          />
          <input
            type="number"
            placeholder="Max jaar"
            value={maxYear}
            onChange={(e) => {
              setMaxYear(e.target.value);
              setCurrentPage(1);
            }}
            className="filterInput"
          />
          <div className="filterRatingGroup">
            <p className="filterRatingTitle">Minimum score</p>
            <div className="filterRatingRow">
              <StarRating
                value={minRating ?? 0}
                onChange={(v) => {
                  setMinRating(v);
                  setCurrentPage(1);
                }}
              />
              <button
                type="button"
                className="filterRatingClear"
                onClick={() => {
                  setMinRating(null);
                  setCurrentPage(1);
                }}
              >
                Reset
              </button>
            </div>
            <span className="filterRatingValue">
              {minRating !== null ? `${minRating}/5` : "Geen minimum"}
            </span>
          </div>

          <div className="filterRatingGroup">
            <p className="filterRatingTitle">Maximum score</p>
            <div className="filterRatingRow">
              <StarRating
                value={maxRating ?? 0}
                onChange={(v) => {
                  setMaxRating(v);
                  setCurrentPage(1);
                }}
              />
              <button
                type="button"
                className="filterRatingClear"
                onClick={() => {
                  setMaxRating(null);
                  setCurrentPage(1);
                }}
              >
                Reset
              </button>
            </div>
            <span className="filterRatingValue">
              {maxRating !== null ? `${maxRating}/5` : "Geen maximum"}
            </span>
          </div>
        </div>
      </aside>

      <div className="mainContent">
        {/* Search bar */}
        <div className="filterSection">
          <div className="catalogSearchbar">
            <input
              type="text"
              placeholder="Titel, auteur, genre, onderwerp"
              value={query}
              onChange={(e) => handleQueryChange(e.target.value)}
            />
            <button id="searchButton" aria-label="Zoeken">
              🔎︎
            </button>
          </div>
        </div>

        <h1>Catalogus</h1>

        {/* Tab bar */}
        <ul>
          {(user?.role === "TEACHER" ||
            user?.role === "BIBLIOTHEEKBEHEERDER" ||
            user?.role === "ADMIN") && (
            <li
              className={activeTab === "Boek toevoegen" ? "active" : ""}
              onClick={() => {
                setActiveTab("Boek toevoegen");
                router.push("/catalog/admin");
              }}
            >
              Boeken selecteren
            </li>
          )}
          {(user?.role === "BIBLIOTHEEKBEHEERDER" ||
            user?.role === "ADMIN") && (
            <li
              className={activeTab === "Beheer catalogus" ? "active" : ""}
              onClick={() => {
                setActiveTab("Beheer catalogus");
                router.push("/manageCatalog");
              }}
            >
              Beheer catalogus
            </li>
          )}
        </ul>

        {/* Toolbar - totaal paginas en aantal boeken kiezen*/}
        <div className="bookListToolbar">
          <span className="resultCount">{totalElements} boeken</span>
          <div className="pageSizeSelector">
            <span className="pageSizeLabel">Per pagina:</span>
            <select
              value={pageSize}
              onChange={(e) => {
                setPageSize(Number(e.target.value));
                setCurrentPage(1);
              }}
              className="pageSizeSelect"
            >
              <option value={20}>20</option>
              <option value={30}>30</option>
              <option value={50}>50</option>
            </select>
          </div>
        </div>

        {/* Boeken lijst */}
        <div id="bookList">
          {books.map((book) => (
            <BookCard
              withCheckbox={false}
              key={book.id}
              book={book}
              isSelected={false}
              onToggle={() => {}}
            />
          ))}
        </div>

        {/* Paging sectie */}
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={(page) => {
            setCurrentPage(page);
            window.scrollTo({ top: 0, behavior: "smooth" });
          }}
        />
      </div>
    </main>
  );
}
