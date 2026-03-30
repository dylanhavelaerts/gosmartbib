"use client";

import { useEffect, useState } from "react";
import { Book, BOOK_CATEGORIES, BOOK_LABELS } from "../interfaces/Book";
import { useRouter, useSearchParams } from "next/navigation";
import "../catalog/bookList.css";
import "./editbook.css";
import ProtectedRoute from "../components/ProtectedRoute";

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
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [query, setQuery] = useState("");
  const router = useRouter();

  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`, { credentials: "include" })
      .then((res) => {
        if (!res.ok) throw new Error("Netwerk response was niet ok");
        return res.json();
      })
      .then((data: Book[]) => {
        setBooks(data);
        const selectedId = searchParams.get("selectedId");
        if (selectedId) {
          const match = data.find((b) => b.id === Number(selectedId));
          if (match) {
            setSelectedBook(match);
          }
        }
      })
      .catch((err) => console.error("Fout bij ophalen boeken:", err));
  }, [apiUrl, searchParams]);

  function openModal() {
    if (!selectedBook) return;
    setFormData({ ...selectedBook });
    setModalOpen(true);
    setError(null);
  }

  function closeModal() {
    setModalOpen(false);
    setError(null);
  }
function handleChange(
  e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
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
    const values = e.target.value.split(",").map((v) => v.trim());
    setFormData((prev) => ({ ...prev, [field]: values }));
  }

  const filteredBooks = books.filter((book) => {
    const q = query.toLowerCase();
    return (
      book.title?.toLowerCase().includes(q) ||
      book.authors?.some((a) => a.toLowerCase().includes(q)) ||
      book.isbn?.toLowerCase().includes(q)
    );
  });

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

    try {
      const res = await fetch(`${apiUrl}/books/${selectedBook.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(formData),
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

  async function tryDelete() {
    if (!selectedBook) return;
    setDeleting(true);
    try {
      const res = await fetch(`${apiUrl}/books/${selectedBook.id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!res.ok) throw new Error("Delete failed");
      setBooks((prev) => prev.filter((b) => b.id !== selectedBook.id));
      setSelectedBook(null);
      setShowDeleteConfirm(false);
    } catch {
      setError(
        "Er is iets misgegaan tijdens het verwijderen, probeer opnieuw.",
      );
    } finally {
      setDeleting(false);
    }
  }

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "ADMIN"]}>
      <div>
        <main className="manage-main-layout">
          <h1 className="manage-title">Beheer catalogus</h1>
          
          <div className="manage-wrapper">
            {/* EILAND LIJST */}
            <div className="eiland-lijst">
              <div className="headerdiv">
                <button
                  onClick={() => router.push("/add-book")}
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
                    {filteredBooks.map((book) => {
                      const isSelected = selectedBook?.id === book.id;
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
                              {book.authors ? book.authors.join(", ") : "Onbekend"}
                            </p>
                            <p className="book-list-isbn">
                              ISBN: {book.isbn || "-"}
                            </p>
                            <div className="copies-container">
                              <span
                                className={`copies-pill ${book.availableCopies === 0 ? "copies-pill--empty" : "copies-pill--available"}`}
                              >
                                {book.availableCopies ?? "-"} beschikbaar
                              </span>
                              <span className="copies-pill copies-pill--total">
                                {book.totalCopies ?? "-"} totaal
                              </span>
                            </div>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                )}
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
                      <button
                        onClick={() => setShowDeleteConfirm(true)}
                        className="btn-danger"
                        type="button"
                      >
                        Verwijderen
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
                              <td>{selectedBook.categories?.join(", ") || "-"}</td>
                            </tr>
                          </tbody>
                        </table>
                      </div>

                      <div className="details-summary">
                        <h3>Samenvatting</h3>
                        <p>
                          {selectedBook.description || "Geen samenvatting beschikbaar voor dit boek."}
                        </p>
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
                      value={formData.authors?.join(", ") || ""}
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
                        onClick={() => setCategoryDropdownOpen(!categoryDropdownOpen)}
                      >
                        Categorie(ën){" "}
                        {formData.categories?.length ? `(${formData.categories.length})` : ""}{" "}
                        ▼
                      </button>
                      {categoryDropdownOpen && (
                        <div className="filterDropdownPanel">
                          {BOOK_CATEGORIES.map((cat) => (
                            <label key={cat} className="filterCheckboxLabel">
                              <input
                                type="checkbox"
                                checked={formData.categories?.includes(cat) || false}
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
                                  categories: prev.categories?.filter((c) => c !== cat),
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
                        onClick={() =>
                          setLabelDropdownOpen(!labelDropdownOpen)
                        }
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
                    ><option value="">Leesniveau</option>
                    <option value="A">A</option>
                    <option value="B">B</option>
                    <option value="C">C</option>
                    <option value="D">D</option>
                      </select>
                  </div>
                  <div className="modal-row">
                    <label className="modal-label" htmlFor="description">
                      Leeftijd
                    </label>
                    <select
                      className="modal-input"
                      name="ageRange"
                      value={formData.ageRange ?? ""}
                      onChange={handleChange}
                    ><option value="">Leeftijd</option>
                    <option value="Eerste graad">Eerste graad</option>
                    <option value="Tweede graad">Tweede graad</option>
                    <option value="Derde graad">Derde graad</option>
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
                    ><option value="true">Ja</option>
                    <option value="false">Nee</option>
                      </select>
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

          {/* VERWIJDER MODAL */}
          {showDeleteConfirm && selectedBook && (
            <div className="modalOverlay">
              <div className="modalBox">
                <p>
                  Ben je zeker dat je <strong>{selectedBook.title}</strong> wilt verwijderen?
                </p>
                <p>Deze actie is onterugkeerbaar!</p>
                <div>
                  <button
                    className="gobackButton"
                    onClick={() => setShowDeleteConfirm(false)}
                    disabled={deleting}
                  >
                    Ga terug
                  </button>
                  <button
                    className="confirmButton"
                    onClick={tryDelete}
                    disabled={deleting}
                  >
                    {deleting ? "Verwijderen..." : "Bevestig"}
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