"use client";

import { useEffect, useState } from "react";
import LabelPrintModal from "./LabelPrintModal";
import type { BookCopyLabel } from "../../interfaces/BookCopyLabel";
import {
  BOOK_CATEGORIES,
  BOOK_LABELS,
  BOOK_LANGUAGE_PRESETS,
} from "../../interfaces/Book";
import type { Book, BookInventory } from "../../interfaces/Book";
import type { MeResponse } from "../../interfaces/user";
import type { SchoolCampusDTO } from "../../interfaces/schoolIntegration";
import {
  fetchSchoolCampuses,
  getCampusSelectOptions,
} from "../../utils/schoolCampuses";
import { formatSchoolLabel } from "../reviews/components/ReviewCard";
import { useRouter } from "next/navigation";
import "../../catalog/bookList.css";
import "./editbook.css";
import ProtectedRoute from "../../components/ProtectedRoute";
import Pagination from "../../catalog/pagination";

export default function ManageCatalogPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [formData, setFormData] = useState<Partial<Book>>({});
  const [languageInputMode, setLanguageInputMode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const CUSTOM_LANGUAGE_VALUE = "__custom_language__";

  const [categoryDropdownOpen, setCategoryDropdownOpen] = useState(false);
  const [labelDropdownOpen, setLabelDropdownOpen] = useState(false);
  const [availableCategories, setAvailableCategories] =
    useState<string[]>(BOOK_CATEGORIES);
  const [availableLabels, setAvailableLabels] = useState<string[]>(BOOK_LABELS);
  const [newCategory, setNewCategory] = useState("");
  const [newLabel, setNewLabel] = useState("");
  const [query, setQuery] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(25);
  const [totalPages, setTotalPages] = useState(0);
  const [me, setMe] = useState<MeResponse | null>(null);
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [campusLoadError, setCampusLoadError] = useState("");

  const [barcodesEnabled, setBarcodesEnabled] = useState(false);
  const [labelModalOpen, setLabelModalOpen] = useState(false);
  const [printLabels, setPrintLabels] = useState<BookCopyLabel[]>([]);
  const [loadingLabels, setLoadingLabels] = useState(false);

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

  // Handle ?selectedId param — fetch the specific book by ID.
  // If ?edit=true is present, immediately open the edit modal for that book.
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const selectedId = params.get("selectedId");
    const shouldOpenEdit = params.get("edit") === "true";

    if (!selectedId) return;

    fetch(`${apiUrl}/books/${selectedId}`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : null))
      .then((book: Book | null) => {
        if (!book) return;

        setSelectedBook(book);

        if (shouldOpenEdit) {
          setFormData({
            ...book,
            inventories: book.inventories ?? [],
          });

          setLanguageInputMode(getLanguageInputMode(book.language));

          setModalOpen(true);
          setError(null);
        }
      })
      .catch(() => {});
  }, [apiUrl]);

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
          const libRes = await fetch(
            `${apiUrl}/admin/schools/${data.school.id}/library-settings`,
            { credentials: "include" },
          );
          if (libRes.ok) {
            const libData = await libRes.json();
            setBarcodesEnabled(libData.barcodesEnabled ?? false);
          }
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

  useEffect(() => {
    const fetchOptions = async (endpoint: string) => {
      const response = await fetch(`${apiUrl}/books/${endpoint}`, {
        credentials: "include",
      });

      if (!response.ok) {
        return [];
      }

      const data = await response.json();
      return Array.isArray(data) ? data : [];
    };

    Promise.all([fetchOptions("categories"), fetchOptions("labels")])
      .then(([categories, labels]) => {
        setAvailableCategories(mergeOptions(BOOK_CATEGORIES, categories));
        setAvailableLabels(mergeOptions(BOOK_LABELS, labels));
      })
      .catch(() => {
        setAvailableCategories(BOOK_CATEGORIES);
        setAvailableLabels(BOOK_LABELS);
      });
  }, [apiUrl]);

  function openModal() {
    if (!selectedBook) return;
    setFormData({
      ...selectedBook,
      inventories: buildFallbackInventories(selectedBook),
    });

    setLanguageInputMode(getLanguageInputMode(selectedBook.language));

    setModalOpen(true);
    setError(null);
  }

  function mergeOptions(baseOptions: string[], databaseOptions: string[]) {
    const mergedOptions: string[] = [];
    const seenOptions = new Set<string>();

    [...baseOptions, ...databaseOptions].forEach((option) => {
      const trimmedOption = option.trim();

      if (!trimmedOption) {
        return;
      }

      const normalizedOption = trimmedOption.toLowerCase();

      if (!seenOptions.has(normalizedOption)) {
        seenOptions.add(normalizedOption);
        mergedOptions.push(trimmedOption);
      }
    });

    return mergedOptions.sort((a, b) => a.localeCompare(b));
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

  function getLanguageInputMode(language?: string | null) {
    if (!language || language.trim() === "") {
      return "";
    }

    const normalizedLanguage = language.trim().toLowerCase();

    if (BOOK_LANGUAGE_PRESETS.includes(normalizedLanguage)) {
      return normalizedLanguage;
    }

    return CUSTOM_LANGUAGE_VALUE;
  }

  function handleLanguageSelectChange(value: string) {
    setLanguageInputMode(value);

    if (value === CUSTOM_LANGUAGE_VALUE) {
      setFormData((prev) => ({
        ...prev,
        language: "",
      }));
      return;
    }

    setFormData((prev) => ({
      ...prev,
      language: value,
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

  async function handlePrintAllLabels() {
    setLoadingLabels(true);
    try {
      const res = await fetch(`${apiUrl}/books/copies/labels/school`, {
        credentials: "include",
      });
      if (!res.ok) throw new Error();
      const data: BookCopyLabel[] = await res.json();
      setPrintLabels(data);
      setLabelModalOpen(true);
    } catch {
      setError("Kon labels niet ophalen.");
    } finally {
      setLoadingLabels(false);
    }
  }

  async function handleOpenLabels() {
    if (!selectedBook) return;
    setLoadingLabels(true);
    try {
      const res = await fetch(
        `${apiUrl}/books/${selectedBook.id}/copies/labels/school`,
        { credentials: "include" },
      );
      if (!res.ok) throw new Error();
      const data: BookCopyLabel[] = await res.json();
      setPrintLabels(data);
      setLabelModalOpen(true);
    } catch {
      setError("Kon labels niet ophalen.");
    } finally {
      setLoadingLabels(false);
    }
  }

  const editableInventoryRows = (formData.inventories ?? [])
    .map((inventory, index) => ({ inventory, index }))
    .filter(({ inventory }) => inventory.schoolId === me?.school?.id);

  return (
    <ProtectedRoute allowedRoles="LIBRARIAN">
      <main className="manage-main-layout">
        <div className="manage-page-header">
          <div>
            <h1>Catalogusbeheer</h1>
            <p>
              Beheer boeken, inventaris en exemplaren voor jouw bibliotheek.
            </p>
          </div>
          {barcodesEnabled && (
            <button
              className="btn-labels"
              disabled={loadingLabels}
              onClick={handlePrintAllLabels}
              type="button"
            >
              {loadingLabels ? "Bezig…" : "Alle labels afdrukken"}
            </button>
          )}
        </div>
        <div className="manage-wrapper">
          {/* BOOK LIST */}
          <div className="book-list-panel">
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

          {/* BOOK DETAILS */}
          <div className="book-details-panel">
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
                      className="modal-btn-cancel"
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
                            <td className="bold">{selectedBook.isbn || "-"}</td>
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
                  </div>
                </div>

                <div className="details-inventory">
                  <div className="details-inventory-header">
                    <h3>Inventaris</h3>
                    {barcodesEnabled &&
                      (() => {
                        const myInventory = selectedBook.inventories?.find(
                          (inv) =>
                            inv.schoolId === me?.school?.id && inv.id != null,
                        );
                        return myInventory ? (
                          <button
                            type="button"
                            className="btn-labels"
                            disabled={loadingLabels}
                            onClick={() => handleOpenLabels()}
                          >
                            {loadingLabels ? "Bezig…" : "Labels afdrukken"}
                          </button>
                        ) : null;
                      })()}
                  </div>

                  {selectedBook.inventories &&
                  selectedBook.inventories.length > 0 ? (
                    <div className="details-table-wrapper">
                      <table className="details-table">
                        <thead>
                          <tr>
                            <th>School</th>
                            <th>Campus</th>
                            <th>Tot.</th>
                            <th>Beschikb.</th>
                            <th>Beschad.</th>
                            <th>Kapot</th>
                            <th>Verloren</th>
                          </tr>
                        </thead>
                        <tbody>
                          {selectedBook.inventories.map((inventory, index) => (
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
                              <td>{inventory.damagedCopies ?? 0}</td>
                              <td>{inventory.brokenCopies ?? 0}</td>
                              <td>{inventory.lostCopies ?? 0}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <p>Geen inventarisgegevens beschikbaar.</p>
                  )}
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
                        {availableCategories.map((cat) => (
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
                        <div className="customOptionRow">
                          <input
                            type="text"
                            className="customOptionInput"
                            value={newCategory}
                            onChange={(e) => setNewCategory(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") {
                                e.preventDefault();

                                const trimmed = newCategory.trim();
                                if (!trimmed) return;

                                setAvailableCategories((prev) =>
                                  prev.some(
                                    (cat) =>
                                      cat.toLowerCase() ===
                                      trimmed.toLowerCase(),
                                  )
                                    ? prev
                                    : [...prev, trimmed].sort((a, b) =>
                                        a.localeCompare(b),
                                      ),
                                );

                                setFormData((prev) => ({
                                  ...prev,
                                  categories: prev.categories?.some(
                                    (cat) =>
                                      cat.toLowerCase() ===
                                      trimmed.toLowerCase(),
                                  )
                                    ? prev.categories
                                    : [...(prev.categories ?? []), trimmed],
                                }));

                                setNewCategory("");
                              }
                            }}
                            placeholder="Nieuwe categorie"
                          />

                          <button
                            type="button"
                            className="customOptionButton"
                            onClick={() => {
                              const trimmed = newCategory.trim();
                              if (!trimmed) return;

                              setAvailableCategories((prev) =>
                                prev.some(
                                  (cat) =>
                                    cat.toLowerCase() === trimmed.toLowerCase(),
                                )
                                  ? prev
                                  : [...prev, trimmed].sort((a, b) =>
                                      a.localeCompare(b),
                                    ),
                              );

                              setFormData((prev) => ({
                                ...prev,
                                categories: prev.categories?.some(
                                  (cat) =>
                                    cat.toLowerCase() === trimmed.toLowerCase(),
                                )
                                  ? prev.categories
                                  : [...(prev.categories ?? []), trimmed],
                              }));

                              setNewCategory("");
                            }}
                          >
                            Toevoegen
                          </button>
                        </div>
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
                        {availableLabels.map((label) => (
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
                        <div className="customOptionRow">
                          <input
                            type="text"
                            className="customOptionInput"
                            value={newLabel}
                            onChange={(e) => setNewLabel(e.target.value)}
                            onKeyDown={(e) => {
                              if (e.key === "Enter") {
                                e.preventDefault();

                                const trimmed = newLabel.trim();
                                if (!trimmed) return;

                                setAvailableLabels((prev) =>
                                  prev.some(
                                    (label) =>
                                      label.toLowerCase() ===
                                      trimmed.toLowerCase(),
                                  )
                                    ? prev
                                    : [...prev, trimmed].sort((a, b) =>
                                        a.localeCompare(b),
                                      ),
                                );

                                setFormData((prev) => ({
                                  ...prev,
                                  labels: prev.labels?.some(
                                    (label) =>
                                      label.toLowerCase() ===
                                      trimmed.toLowerCase(),
                                  )
                                    ? prev.labels
                                    : [...(prev.labels ?? []), trimmed],
                                }));

                                setNewLabel("");
                              }
                            }}
                            placeholder="Nieuw leefwereldlabel"
                          />

                          <button
                            type="button"
                            className="customOptionButton"
                            onClick={() => {
                              const trimmed = newLabel.trim();
                              if (!trimmed) return;

                              setAvailableLabels((prev) =>
                                prev.some(
                                  (label) =>
                                    label.toLowerCase() ===
                                    trimmed.toLowerCase(),
                                )
                                  ? prev
                                  : [...prev, trimmed].sort((a, b) =>
                                      a.localeCompare(b),
                                    ),
                              );

                              setFormData((prev) => ({
                                ...prev,
                                labels: prev.labels?.some(
                                  (label) =>
                                    label.toLowerCase() ===
                                    trimmed.toLowerCase(),
                                )
                                  ? prev.labels
                                  : [...(prev.labels ?? []), trimmed],
                              }));

                              setNewLabel("");
                            }}
                          >
                            Toevoegen
                          </button>
                        </div>
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
                                labels: prev.labels?.filter((c) => c !== label),
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
                  <label className="modal-label" htmlFor="languageSelect">
                    Taal
                  </label>

                  <select
                    id="languageSelect"
                    className="modal-input"
                    value={languageInputMode}
                    onChange={(e) => handleLanguageSelectChange(e.target.value)}
                  >
                    <option value="">Kies een taal</option>
                    {BOOK_LANGUAGE_PRESETS.map((languagePreset) => (
                      <option key={languagePreset} value={languagePreset}>
                        {languagePreset.toUpperCase()}
                      </option>
                    ))}
                    <option value={CUSTOM_LANGUAGE_VALUE}>
                      Andere taal...
                    </option>
                  </select>

                  {languageInputMode === CUSTOM_LANGUAGE_VALUE && (
                    <input
                      id="language"
                      name="language"
                      className="modal-input"
                      type="text"
                      value={formData.language ?? ""}
                      onChange={handleChange}
                      placeholder="Geef een afkorting van een taal in"
                    />
                  )}
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
                      <p className="inventory-editor-school">
                        {formatSchoolLabel(
                          inventory.schoolName || me?.school?.name || "",
                        )}
                      </p>
                      <div className="inventory-editor-grid">
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
                    style={{ width: "fit-content", marginTop: "0.25rem" }}
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
        {labelModalOpen && (
          <LabelPrintModal
            labels={printLabels}
            onClose={() => setLabelModalOpen(false)}
          />
        )}
      </main>
    </ProtectedRoute>
  );
}
