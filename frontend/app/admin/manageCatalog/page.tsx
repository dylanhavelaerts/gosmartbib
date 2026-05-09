"use client";

export const dynamic = "force-dynamic";

import { useEffect, useState } from "react";
import { BOOK_CATEGORIES, BOOK_LABELS } from "../../interfaces/Book";
import type { Book, BookInventory } from "../../interfaces/Book";
import type { MeResponse } from "../../interfaces/user";
import type { SchoolCampusDTO } from "../../interfaces/schoolIntegration";
import {
  fetchSchoolCampuses,
  getCampusSelectOptions,
} from "../../utils/schoolCampuses";
import { useRouter, useSearchParams } from "next/navigation";
import "../../catalog/bookList.css";
import "./editbook.css";
import ProtectedRoute from "../../components/ProtectedRoute";
import Pagination from "../../catalog/pagination";

export default function ManageCatalogPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [formData, setFormData] = useState<Partial<Book>>({});
  const [error, setError] = useState<string | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const searchParams = useSearchParams();
  const [categoryDropdownOpen, setCategoryDropdownOpen] = useState(false);
  const [labelDropdownOpen, setLabelDropdownOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(25);
  const [totalPages, setTotalPages] = useState(0);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [campusLoadError, setCampusLoadError] = useState("");
  const router = useRouter();

  // Fetch paged books from backend (with search debounce)
  useEffect(() => {
    const params = new URLSearchParams();
    params.append("page", String(currentPage - 1));
    params.append("size", String(pageSize));

    const isSearching = query.trim() !== "";
    const url = isSearching
      ? `${apiUrl}/books/search?query=${encodeURIComponent(query.trim())}&${params}`
      : `${apiUrl}/books/all?${params}`;

    const delay = isSearching ? 300 : 0;
    const timer = setTimeout(() => {
      fetch(url, { credentials: "include" })
        .then((res) => res.json())
        .then((data) => {
          setBooks(data.content);
          setTotalPages(data.totalPages);
        })
        .catch((err) => console.error("Fout bij ophalen boeken:", err));
    }, delay);

    return () => clearTimeout(timer);
  }, [query, currentPage, pageSize, apiUrl]);

  // Handle ?selectedId param — fetch the specific book by ID
  useEffect(() => {
    const selectedId = searchParams.get("selectedId");
    if (!selectedId) return;
    fetch(`${apiUrl}/books/${selectedId}`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : null))
      .then((book) => {
        if (book) setSelectedBook(book);
      })
      .catch(() => {});
  }, [apiUrl, searchParams]);

  useEffect(() => {
    async function loadMeAndCampuses() {
      try {
        setLoadingCampuses(true);
        setCampusLoadError("");

        const response = await fetch(`${apiUrl}/auth/me`, {
          credentials: "include",
        });

        if (!response.ok) {
          throw new Error("Kon auth/me niet ophalen");
        }

        const data: MeResponse = await response.json();
        setMe(data);

        if (data.school?.id) {
          const campusData = await fetchSchoolCampuses(apiUrl, data.school.id);
          setCampuses(campusData);
        }
      } catch (err) {
        console.error("Fout bij ophalen gebruiker of campussen:", err);
        setCampusLoadError(
          err instanceof Error ? err.message : "Kon de campussen niet ophalen",
        );
      } finally {
        setLoadingCampuses(false);
      }
    }
    loadMeAndCampuses();
  }, [apiUrl]);

  function openModal() {
    if (!selectedBook) return;
    setFormData({
      ...selectedBook,
      inventories: buildFallbackInventories(selectedBook),
    });
    setModalOpen(true);
    setError(null);
  }

  function closeModal() {
    setModalOpen(false);
    setError(null);
  }
  function handleChange(
    e: React.ChangeEvent<
      HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement
    >,
  ) {
    const { name, value } = e.target;

    setFormData((prev) => ({
      ...prev,
      [name]:
        name === "pageCount" || name === "publishedYear"
          ? value === ""
            ? undefined
            : Number(value)
          : name === "didacticTag"
            ? value === "true"
            : value,
    }));
  }
  function handleArrayChange(
    e: React.ChangeEvent<HTMLInputElement>,
    field: keyof Book,
  ) {
    const values = e.target.value.split(",");

    setFormData((prev) => ({ ...prev, [field]: values }));
  }

  useEffect(() => {
    setCurrentPage(1);
  }, [query]);

  async function handleSave() {
    if (!selectedBook) return;
    setError(null);
    if (!formData.title || formData.title.trim() === "") {
      setError("Titel mag niet leeg zijn.");
      return;
    }

    if (formData.pageCount !== undefined && formData.pageCount <= 0) {
      setError("Aantal pagina's mag niet negatief of nul zijn.");
      return;
    }

    if (
      formData.publishedYear !== undefined &&
      formData.publishedYear > new Date().getFullYear()
    ) {
      setError("Uitgavejaar mag niet in de toekomst liggen.");
      return;
    }
    if (
      formData.publishedYear !== undefined &&
      formData.publishedYear !== null
    ) {
      if (formData.publishedYear <= 0) {
        setError("Uitgavejaar moet groter zijn dan 0.");
        return;
      }
    }

    const inventories = formData.inventories ?? [];

    for (const inventory of inventories) {
      if (!inventory.schoolId) {
        setError("Elke inventarisregel moet een school hebben.");
        return;
      }

      if (inventory.totalCopies < 0 || inventory.availableCopies < 0) {
        setError("Aantallen mogen niet negatief zijn.");
        return;
      }

      if (inventory.availableCopies > inventory.totalCopies) {
        setError("Beschikbare exemplaren mogen niet groter zijn dan totaal.");
        return;
      }
    }

    const computedTotalCopies = inventories.reduce(
      (sum, inventory) => sum + (inventory.totalCopies || 0),
      0,
    );

    const computedAvailableCopies = inventories.reduce(
      (sum, inventory) => sum + (inventory.availableCopies || 0),
      0,
    );

    const payload = {
      ...formData,
      authors: (formData.authors ?? [])
        .map((author) => author.trim())
        .filter((author) => author !== ""),
      totalCopies: computedTotalCopies,
      availableCopies: computedAvailableCopies,
      inventories: inventories.map((inventory) => ({
        id: inventory.id ?? null,
        schoolId: inventory.schoolId,
        schoolName: inventory.schoolName ?? "",
        campus: inventory.campus?.trim() ?? "",
        totalCopies: inventory.totalCopies,
        availableCopies: inventory.availableCopies,
      })),
    };

    try {
      const res = await fetch(`${apiUrl}/books/${selectedBook.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error("Save failed");

      const updated: Book = await res.json();

      setBooks((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
      setSelectedBook(updated);
      closeModal();
    } catch {
      setError("Er is iets misgegaan tijdens het opslaan, probeer opnieuw.");
    }
  }

  const buildFallbackInventories = (book: Book): BookInventory[] => {
    if (book.inventories && book.inventories.length > 0) {
      return book.inventories;
    }

    if (!me?.school) {
      return [];
    }

    return [
      {
        id: null,
        schoolId: me.school.id,
        schoolName: me.school.name,
        campus: "",
        totalCopies: book.totalCopies ?? 0,
        availableCopies: book.availableCopies ?? 0,
      },
    ];
  };

  function handleInventoryChange(
    index: number,
    field: keyof BookInventory,
    value: string | number | null,
  ) {
    setFormData((prev) => ({
      ...prev,
      inventories: (prev.inventories ?? []).map((inventory, i) =>
        i === index ? { ...inventory, [field]: value } : inventory,
      ),
    }));
  }

  function addInventoryRow() {
    setFormData((prev) => ({
      ...prev,
      inventories: [
        ...(prev.inventories ?? []),
        {
          id: null,
          schoolId: me?.school?.id ?? null,
          schoolName: me?.school?.name ?? "",
          campus: "",
          totalCopies: 1,
          availableCopies: 1,
        },
      ],
    }));
  }

  function removeInventoryRow(index: number) {
    setFormData((prev) => ({
      ...prev,
      inventories: (prev.inventories ?? []).filter((_, i) => i !== index),
    }));
  }

  const editableInventoryRows = (formData.inventories ?? [])
    .map((inventory, index) => ({ inventory, index }))
    .filter(({ inventory }) => inventory.schoolId === me?.school?.id);

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "ADMIN"]}>
      <div>
        <main className="manage-main-layout">
          <div className="manage-wrapper">
            {/* EILAND LIJST */}
            <div className="eiland-lijst">
              <div className="headerdiv">
                <button
                  onClick={() => router.push("/admin/add-book")}
                  className="modal-btn-save"
                  type="button"
                >
                  + Boek(en) toevoegen
                </button>
              </div>

              <div className="search-container">
                <input
                  type="text"
                  placeholder="Zoek op titel, auteur of ISBN..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="search-input"
                />
              </div>

              <div className="list-container">
                {books.length === 0 ? (
                  <p className="empty-message">Geen boeken gevonden.</p>
                ) : (
                  <ul className="book-list">
                    {books.map((book) => {
                      const isSelected = selectedBook?.id === book.id;
                      const schoolInventory = book.inventories?.find(
                        (inv) => inv.schoolId === me?.school?.id,
                      );
                      const displayAvailable =
                        schoolInventory !== undefined
                          ? schoolInventory.availableCopies
                          : book.availableCopies;
                      const displayTotal =
                        schoolInventory !== undefined
                          ? schoolInventory.totalCopies
                          : book.totalCopies;
                      return (
                        <li
                          key={book.id}
                          onClick={() => setSelectedBook(book)}
                          className={`book-list-item ${isSelected ? "selected" : ""}`}
                        >
                          <div className="book-list-thumb">
                            {book.thumbnail && book.thumbnail.trim() !== "" ? (
                              <img src={book.thumbnail} alt={book.title} />
                            ) : (
                              <span>Geen cover</span>
                            )}
                          </div>

                          <div className="book-list-info">
                            <h3 className="book-list-title">{book.title}</h3>
                            <p className="book-list-authors">
                              {book.authors
                                ? book.authors.join(", ")
                                : "Onbekend"}
                            </p>
                            <p className="book-list-isbn">
                              ISBN: {book.isbn || "-"}
                            </p>
                            <div className="copies-container">
                              <span
                                className={`copies-pill ${displayAvailable === 0 ? "copies-pill--empty" : "copies-pill--available"}`}
                              >
                                {displayAvailable ?? "-"} beschikbaar
                              </span>
                              <span className="copies-pill copies-pill--total">
                                {displayTotal ?? "-"} totaal
                              </span>
                            </div>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>

              <div className="list-footer">
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={setCurrentPage}
                />
              </div>
            </div>

            {/* EILAND DETAILS */}
            <div className="eiland-details">
              {!selectedBook ? (
                <div className="details-empty">
                  <p>Klik op een boek in de lijst om de details te bekijken.</p>
                </div>
              ) : (
                <div>
                  <div className="details-header">
                    <div className="details-title-container">
                      <h1 className="details-title">{selectedBook.title}</h1>
                      <p className="details-author">
                        door {selectedBook.authors?.join(", ") || "Onbekend"}
                      </p>
                    </div>

                    <div className="details-actions">
                      <button
                        onClick={openModal}
                        className="btn-secondary"
                        type="button"
                      >
                        Bewerken
                      </button>
                    </div>
                  </div>

                  <div className="details-content">
                    <div className="details-cover-container">
                      {selectedBook.thumbnail ? (
                        <img
                          src={selectedBook.thumbnail}
                          alt={`Cover van ${selectedBook.title}`}
                          className="details-cover"
                        />
                      ) : (
                        <div className="details-cover-placeholder">
                          <span>Geen cover</span>
                        </div>
                      )}
                    </div>

                    <div className="details-info-container">
                      <div className="details-table-wrapper">
                        <table className="details-table">
                          <tbody>
                            <tr>
                              <th>ISBN</th>
                              <td className="bold">
                                {selectedBook.isbn || "-"}
                              </td>
                            </tr>
                            <tr>
                              <th>Uitgeverij</th>
                              <td>{selectedBook.publisher || "-"}</td>
                            </tr>
                            <tr>
                              <th>Uitgavejaar</th>
                              <td>{selectedBook.publishedYear || "-"}</td>
                            </tr>
                            <tr>
                              <th>Pagina's</th>
                              <td>{selectedBook.pageCount || "-"}</td>
                            </tr>
                            <tr>
                              <th>Categorie</th>
                              <td>
                                {selectedBook.categories?.join(", ") || "-"}
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>

                      <div className="details-summary">
                        <h3>Samenvatting</h3>
                        <p>
                          {selectedBook.description ||
                            "Geen samenvatting beschikbaar voor dit boek."}
                        </p>
                      </div>
                      <div className="details-summary">
                        <h3>Inventaris</h3>

                        {selectedBook.inventories &&
                        selectedBook.inventories.length > 0 ? (
                          <table className="details-table">
                            <thead>
                              <tr>
                                <th>School</th>
                                <th>Campus</th>
                                <th>Totaal</th>
                                <th>Beschikbaar</th>
                              </tr>
                            </thead>
                            <tbody>
                              {selectedBook.inventories.map(
                                (inventory, index) => (
                                  <tr
                                    key={
                                      inventory.id ??
                                      `${inventory.schoolId}-${inventory.campus}-${index}`
                                    }
                                  >
                                    <td>
                                      {inventory.schoolName ||
                                        inventory.schoolId ||
                                        "-"}
                                    </td>
                                    <td>{inventory.campus || ""}</td>
                                    <td>{inventory.totalCopies}</td>
                                    <td>{inventory.availableCopies}</td>
                                  </tr>
                                ),
                              )}
                            </tbody>
                          </table>
                        ) : (
                          <p>Geen inventarisgegevens beschikbaar.</p>
                        )}
                      </div>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* BEWERKEN MODAL */}
          {modalOpen && selectedBook && (
            <div className="modal-overlay">
              <div className="modal-box">
                <div className="modal-header">
                  <h2>{selectedBook.title} bewerken</h2>
                </div>

                <div className="modal-body">
                  {error && <p className="modal-error">⚠ {error}</p>}

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="title">
                      Titel
                    </label>
                    <input
                      id="title"
                      name="title"
                      className="modal-input"
                      type="text"
                      value={formData.title || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="authors">
                      Auteur(s) <span>(komma-gescheiden)</span>
                    </label>
                    <input
                      id="authors"
                      name="authors"
                      className="modal-input"
                      type="text"
                      value={formData.authors?.join(",") || ""}
                      onChange={(e) => handleArrayChange(e, "authors")}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="isbn">
                      ISBN
                    </label>
                    <input
                      id="isbn"
                      name="isbn"
                      className="modal-input"
                      type="text"
                      value={formData.isbn || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="publisher">
                      Uitgeverij
                    </label>
                    <input
                      id="publisher"
                      name="publisher"
                      className="modal-input"
                      type="text"
                      value={formData.publisher || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="publishedYear">
                      Uitgavejaar
                    </label>
                    <input
                      id="publishedYear"
                      name="publishedYear"
                      className="modal-input"
                      type="number"
                      value={formData.publishedYear || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="pageCount">
                      Pagina's
                    </label>
                    <input
                      id="pageCount"
                      name="pageCount"
                      className="modal-input"
                      type="number"
                      value={formData.pageCount || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label"> Categorie(ën)</label>
                    <div className="filterDropdown">
                      <button
                        type="button"
                        className="filterDropdownToggle"
                        onClick={() =>
                          setCategoryDropdownOpen(!categoryDropdownOpen)
                        }
                      >
                        Categorie(ën){" "}
                        {formData.categories?.length
                          ? `(${formData.categories.length})`
                          : ""}{" "}
                        ▼
                      </button>
                      {categoryDropdownOpen && (
                        <div className="filterDropdownPanel">
                          {BOOK_CATEGORIES.map((cat) => (
                            <label key={cat} className="filterCheckboxLabel">
                              <input
                                type="checkbox"
                                checked={
                                  formData.categories?.includes(cat) || false
                                }
                                onChange={() => {
                                  const current = formData.categories || [];
                                  const updated = current.includes(cat)
                                    ? current.filter((c) => c !== cat)
                                    : [...current, cat];
                                  setFormData((prev) => ({
                                    ...prev,
                                    categories: updated,
                                  }));
                                }}
                              />
                              {cat}
                            </label>
                          ))}
                        </div>
                      )}
                    </div>
                    {formData.categories && formData.categories.length > 0 && (
                      <div className="modal-selected-categories">
                        {formData.categories.map((cat) => (
                          <span key={cat} className="modal-category-pill">
                            {cat}
                            <button
                              type="button"
                              onClick={() =>
                                setFormData((prev) => ({
                                  ...prev,
                                  categories: prev.categories?.filter(
                                    (c) => c !== cat,
                                  ),
                                }))
                              }
                            >
                              ✕
                            </button>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="modal-row">
                    <label className="modal-label"> Leefwereldlabel(s)</label>
                    <div className="filterDropdown">
                      <button
                        type="button"
                        className="filterDropdownToggle"
                        onClick={() => setLabelDropdownOpen(!labelDropdownOpen)}
                      >
                        Label(s){" "}
                        {formData.labels?.length
                          ? `(${formData.labels?.length})`
                          : ""}{" "}
                        ▼
                      </button>
                      {labelDropdownOpen && (
                        <div className="filterDropdownPanel">
                          {BOOK_LABELS.map((label) => (
                            <label key={label} className="filterCheckboxLabel">
                              <input
                                type="checkbox"
                                checked={
                                  formData.labels?.includes(label) || false
                                }
                                onChange={() => {
                                  const current = formData.labels || [];
                                  const updated = current.includes(label)
                                    ? current.filter((c) => c !== label)
                                    : [...current, label];
                                  setFormData((prev) => ({
                                    ...prev,
                                    labels: updated,
                                  }));
                                }}
                              />
                              {label}
                            </label>
                          ))}
                        </div>
                      )}
                    </div>
                    {/* Filter Pills */}
                    {formData.labels && formData.labels.length > 0 && (
                      <div className="modal-selected-categories">
                        {formData.labels.map((label) => (
                          <span key={label} className="modal-category-pill">
                            {label}
                            <button
                              type="button"
                              onClick={() =>
                                setFormData((prev) => ({
                                  ...prev,
                                  labels: prev.labels?.filter(
                                    (c) => c !== label,
                                  ),
                                }))
                              }
                            >
                              ✕
                            </button>
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="modal-row">
                    <label className="modal-label" htmlFor="thumbnail">
                      Voorpagina
                    </label>
                    <input
                      id="thumbnail"
                      name="thumbnail"
                      className="modal-input"
                      type="text"
                      value={formData.thumbnail || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="description">
                      Samenvatting
                    </label>
                    <textarea
                      id="description"
                      name="description"
                      className="modal-textarea"
                      value={formData.description || ""}
                      onChange={handleChange}
                    />
                  </div>

                  <div className="modal-row">
                    <label className="modal-label" htmlFor="description">
                      Taal
                    </label>
                    <select
                      name="language"
                      className="modal-input"
                      value={formData.language ?? ""}
                      onChange={handleChange}
                    >
                      <option value="">Alle talen</option>
                      <option value="NE">NE</option>
                      <option value="EN">EN</option>
                      <option value="FR">FR</option>
                    </select>
                  </div>
                  <div className="modal-row">
                    <label className="modal-label" htmlFor="description">
                      Leesniveau
                    </label>
                    <select
                      className="modal-input"
                      name="readingLevel"
                      value={formData.readingLevel ?? ""}
                      onChange={handleChange}
                    >
                      <option value="">Leesniveau</option>
                      <option value="A">A</option>
                      <option value="B">B</option>
                      <option value="C">C</option>
                      <option value="D">D</option>
                    </select>
                  </div>
                  <div className="modal-row">
                    <label className="modal-label" htmlFor="description">
                      Didactisch boek
                    </label>
                    <select
                      className="modal-input"
                      name="didacticTag"
                      value={
                        formData.didacticTag === undefined
                          ? "true"
                          : String(formData.didacticTag)
                      }
                      onChange={handleChange}
                    >
                      <option value="true">Ja</option>
                      <option value="false">Nee</option>
                    </select>
                  </div>
                  <div className="modal-row">
                    <label className="modal-label">
                      Inventaris per school/campus
                    </label>

                    {campusLoadError && (
                      <p className="modal-error">⚠ {campusLoadError}</p>
                    )}

                    {editableInventoryRows.length === 0 && (
                      <p className="modal-error">
                        Er is nog geen inventarisregel voor jouw school.
                      </p>
                    )}

                    {editableInventoryRows.map(({ inventory, index }) => (
                      <div
                        key={inventory.id ?? index}
                        className="inventory-editor-card"
                      >
                        <div className="inventory-editor-grid">
                          <div>
                            <label className="modal-label">School</label>
                            <input
                              className="modal-input"
                              type="text"
                              value={
                                inventory.schoolName || me?.school?.name || ""
                              }
                              disabled
                            />
                          </div>

                          <div>
                            <label className="modal-label">Campus</label>
                            <select
                              className="modal-input"
                              value={inventory.campus || ""}
                              onChange={(e) =>
                                handleInventoryChange(
                                  index,
                                  "campus",
                                  e.target.value,
                                )
                              }
                              disabled={loadingCampuses}
                            >
                              <option value="">
                                {loadingCampuses
                                  ? "Campussen laden..."
                                  : "Geen campus"}
                              </option>

                              {getCampusSelectOptions(
                                campuses,
                                inventory.campus,
                              ).map((campusOption) => (
                                <option
                                  key={`${campusOption.id}-${campusOption.name}`}
                                  value={campusOption.name}
                                >
                                  {campusOption.name}
                                </option>
                              ))}
                            </select>
                          </div>

                          <div>
                            <label className="modal-label">Totaal</label>
                            <input
                              className="modal-input"
                              type="number"
                              min="0"
                              value={inventory.totalCopies}
                              onChange={(e) =>
                                handleInventoryChange(
                                  index,
                                  "totalCopies",
                                  Number(e.target.value) || 0,
                                )
                              }
                            />
                          </div>

                          <div>
                            <label className="modal-label">Beschikbaar</label>
                            <input
                              className="modal-input"
                              type="number"
                              min="0"
                              value={inventory.availableCopies}
                              onChange={(e) =>
                                handleInventoryChange(
                                  index,
                                  "availableCopies",
                                  Number(e.target.value) || 0,
                                )
                              }
                            />
                          </div>
                        </div>

                        {editableInventoryRows.length > 1 && (
                          <button
                            type="button"
                            className="modal-btn-cancel"
                            onClick={() => removeInventoryRow(index)}
                          >
                            Regel verwijderen
                          </button>
                        )}
                      </div>
                    ))}

                    <button
                      type="button"
                      className="modal-btn-save"
                      onClick={addInventoryRow}
                    >
                      + Campus toevoegen
                    </button>
                  </div>
                </div>
                <div className="modal-footer">
                  <button
                    className="modal-btn-cancel"
                    type="button"
                    onClick={closeModal}
                  >
                    Annuleren
                  </button>
                  <button
                    className="modal-btn-save"
                    type="button"
                    onClick={handleSave}
                  >
                    Opslaan
                  </button>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </ProtectedRoute>
  );
}
