"use client";

import { useSearchParams, useRouter, usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";
import BookCard from "./bookCard";
import Pagination from "./pagination";
import "./bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  const [activeTab, setActiveTab] = useState("Catalogus");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<Book[]>([]);

  const [language, setLanguage] = useState("");
  const [categories, setCategories] = useState<Set<string>>(new Set());
  const [minPages, setMinPages] = useState("");
  const [maxPages, setMaxPages] = useState("");
  const [minYear, setMinYear] = useState("");
  const [maxYear, setMaxYear] = useState("");
  const [categoryOpen, setCategoryOpen] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const PAGE_SIZE = pageSize;
  const totalPages = Math.ceil(results.length / PAGE_SIZE);
  const paginatedResults = results.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();

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
    const isFiltered = filterQuery.length > 0;
    const url = isFiltered
      ? `${process.env.NEXT_PUBLIC_API_URL}/books/filter?${filterQuery}`
      : `${process.env.NEXT_PUBLIC_API_URL}/books/all?page=0&size=10000`;

    fetch(url)
      .then((res) => res.json())
      .then((data) => {
        const bookList: Book[] = Array.isArray(data) ? data : data.content;
        setBooks(bookList);
        if (!query) {
          setResults(bookList);
          setCurrentPage(1);
        }
      });
  }, [language, categories, minPages, maxPages, minYear, maxYear]);

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
      setCurrentPage(1);
    }, 300);
    return () => clearTimeout(delay);
  }, [query, books]);

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

        {/* Tab bar — no page size selector here */}
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
        </ul>

        {/* Toolbar row */}
        <div className="bookListToolbar">
          <span className="resultCount">{results.length} boeken</span>
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

        <div id="bookList">
          {paginatedResults.map((book) => (
            <BookCard
              withCheckbox={false}
              key={book.id}
              book={book}
              isSelected={false}
              onToggle={() => {}}
            />
          ))}
        </div>

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
