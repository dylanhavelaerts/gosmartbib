"use client";

import { useEffect, useState } from "react";
import { Book, BOOK_CATEGORIES } from "../interfaces/Book";
import { useRouter, useSearchParams } from "next/navigation";
import "../catalog/bookList.css";
import "../manageCatalog/editbook.css";

export default function ManageCatalogPage() {
  const [books, setBooks] = useState<Book[]>([]);
  const [selectedBook, setSelectedBook] = useState<Book | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [formData, setFormData] = useState<Partial<Book>>({});
  const [error, setError] = useState<string | null>(null);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
  const searchParams = useSearchParams();
  const [categoryDropdownOpen, setCategoryDropdownOpen] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [query, setQuery] = useState("");
  const router = useRouter();

  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`)
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
  }, []);
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
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>,
  ) {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
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
        body: JSON.stringify(formData),
      });

      if (!res.ok) throw new Error("Save failed");

      const updated: Book = await res.json();

      setBooks((prev) => prev.map((b) => (b.id === updated.id ? updated : b)));
      setSelectedBook(updated);
      closeModal();
    } catch {
      setError("Er is iets misgegaan tijdens het opslagen, probeer opnieuw.");
    }
  }
  async function tryDelete() {
    if (!selectedBook) return;
    setDeleting(true);
    try {
      const res = await fetch(`${apiUrl}/books/${selectedBook.id}`, {
        method: "DELETE",
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
    <main style={{ display: "flex", flexDirection: "column" }}>
      <style>{`
        .manage-wrapper {
          display: flex;
          flex-direction: column-reverse;
          gap: 2rem;
          width: 100%;
          max-width: 1400px;
          flex: 1;
          margin: 0 auto; 
          padding-bottom: 0; 
        }
        .eiland-lijst {
          width: 100%;
        }
        .eiland-details {
          width: 100%;
        }

        @media (min-width: 900px) {
          .manage-wrapper {
            flex-direction: row; 
          }
          .eiland-lijst {
            width: 350px;
            flex-shrink: 0;
          }
          .eiland-details {
            flex: 1;
            min-width: 0;
          }
        }
      `}</style>
      <h1 style={{ margin: "0 0 1.5rem 0", padding: 0, lineHeight: "1" }}>
        Beheer catalogus
      </h1>
      <div className="manage-wrapper">
        <div
          className="eiland-lijst"
          style={{
            height: "calc(80vh - 4rem)",
            boxSizing: "border-box",
            backgroundColor: "white",
            borderRadius: "7px",
            border: "1.2px solid #8a1d40",
            boxShadow: "0 2px 12px rgba(0, 0, 0, 0.12)",
            display: "flex",
            flexDirection: "column",
            overflow: "hidden",
          }}
        >
          <div className="headerdiv">
            <button
              onClick={() => router.push("/add-book")}
              className="modal-btn-save"
              type="button"
            >
              + Boek(en) toevoegen
            </button>
          </div>
          <div
            style={{
              padding: "1.5rem",
              borderBottom: "1px solid #ddd",
              backgroundColor: "#fdfdfd",
            }}
          >
            <input
              type="text"
              placeholder="Zoek op titel, auteur of ISBN..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{
                width: "100%",
                padding: "0.8rem",
                borderRadius: "6px",
                border: "1px solid #ccc",
                boxSizing: "border-box",
                backgroundColor: "#ece6f0",
                outline: "none",
              }}
            />
          </div>

          <div style={{ flex: 1, overflowY: "auto", padding: "1rem" }}>
            {books.length === 0 ? (
              <p style={{ textAlign: "center", color: "#888" }}>
                Geen boeken gevonden.
              </p>
            ) : (
              <ul
                style={{
                  listStyle: "none",
                  margin: 0,
                  padding: 0,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "stretch",
                  gap: "0.5rem",
                }}
              >
                {filteredBooks.map((book) => {
                  const isSelected = selectedBook?.id === book.id;
                  return (
                    <li
                      key={book.id}
                      onClick={() => setSelectedBook(book)}
                      style={{
                        padding: "0.8rem",
                        borderRadius: "6px",
                        cursor: "pointer",
                        backgroundColor: isSelected ? "#8e2446" : "#fff",
                        color: isSelected ? "white" : "#333",
                        border: isSelected
                          ? "1px solid #8e2446"
                          : "1px solid #eee",
                        transition: "all 0.2s ease",
                        display: "flex",
                        alignItems: "flex-start",
                        gap: "1rem",
                      }}
                    >
                      <div
                        style={{
                          flexShrink: 0,
                          width: "45px",
                          height: "65px",
                          backgroundColor: "#eee",
                          borderRadius: "4px",
                          overflow: "hidden",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          marginTop: "0.2rem",
                        }}
                      >
                        {book.thumbnail && book.thumbnail.trim() !== "" ? (
                          <img
                            src={book.thumbnail}
                            alt={book.title}
                            style={{
                              width: "100%",
                              height: "100%",
                              objectFit: "cover",
                            }}
                          />
                        ) : (
                          <span style={{ fontSize: "0.6rem", color: "#aaa" }}>
                            Geen cover
                          </span>
                        )}
                      </div>

                      <div style={{ flex: 1, overflow: "hidden" }}>
                        <h3
                          style={{
                            margin: "0 0 0.25rem 0",
                            fontSize: "1rem",
                            whiteSpace: "normal",
                            wordBreak: "break-word",
                          }}
                        >
                          {book.title}
                        </h3>
                        <p
                          style={{
                            margin: 0,
                            fontSize: "0.85rem",
                            opacity: isSelected ? 0.9 : 0.6,
                            whiteSpace: "normal",
                            wordBreak: "break-word",
                          }}
                        >
                          {book.authors ? book.authors.join(", ") : "Onbekend"}
                        </p>
                        <p
                          style={{
                            margin: "0.25rem 0 0 0",
                            fontSize: "0.75rem",
                            opacity: isSelected ? 0.7 : 0.5,
                            wordBreak: "break-word",
                          }}
                        >
                          ISBN: {book.isbn || "-"}
                        </p>
                        <div
                          style={{
                            display: "flex",
                            gap: "0.4rem",
                            marginTop: "0.4rem",
                          }}
                        >
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

        <div
          className="eiland-details"
          style={{
            height: "calc(80vh - 4rem)",
            boxSizing: "border-box",
            backgroundColor: "white",
            borderRadius: "7px",
            border: "1.2px solid #8a1d40",
            boxShadow: "0 2px 12px rgba(0, 0, 0, 0.12)",
            display: "flex",
            flexDirection: "column",
            overflowY: "auto",
            padding: "clamp(1.5rem, 3vw, 3rem)",
          }}
        >
          {!selectedBook ? (
            <div
              style={{
                flex: 1,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#888",
              }}
            >
              <p style={{ fontSize: "1.2rem", textAlign: "center" }}>
                Klik op een boek in de lijst om de details te bekijken.
              </p>
            </div>
          ) : (
            <div>
              <div
                style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "1rem",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "2rem",
                }}
              >
                <div style={{ flex: "1 1 200px" }}>
                  <h1
                    style={{
                      margin: "0 0 0.5rem 0",
                      fontSize: "clamp(1.5rem, 4vw, 2rem)",
                      color: "#c0392b",
                      padding: 0,
                    }}
                  >
                    {selectedBook.title}
                  </h1>
                  <p style={{ margin: 0, fontSize: "1.1rem", color: "#666" }}>
                    door {selectedBook.authors?.join(", ") || "Onbekend"}
                  </p>
                </div>

                <div style={{ display: "flex", gap: "1rem" }}>
                  <button
                    onClick={openModal}
                    className="confirmButton"
                    type="button"
                    style={{
                      padding: "0.6rem 1.2rem",
                      backgroundColor: "#ece6f0",
                      color: "#333",
                      border: "none",
                      cursor: "pointer",
                      fontWeight: "bold",
                    }}
                  >
                    Bewerken
                  </button>
                  <button
                    onClick={() => setShowDeleteConfirm(true)}
                    className="confirmButton"
                    type="button"
                    style={{
                      padding: "0.6rem 1.2rem",
                      backgroundColor: "#c0392b",
                      color: "white",
                      border: "none",
                      cursor: "pointer",
                      fontWeight: "bold",
                    }}
                  >
                    Verwijderen
                  </button>
                </div>
              </div>

              <div style={{ display: "flex", flexWrap: "wrap", gap: "2rem" }}>
                <div style={{ flexShrink: 0, margin: "0 auto" }}>
                  {selectedBook.thumbnail ? (
                    <img
                      src={selectedBook.thumbnail}
                      alt={`Cover van ${selectedBook.title}`}
                      style={{
                        width: "200px",
                        maxWidth: "100%",
                        height: "auto",
                        borderRadius: "6px",
                        boxShadow: "0 4px 8px rgba(0,0,0,0.1)",
                      }}
                    />
                  ) : (
                    <div
                      style={{
                        width: "200px",
                        height: "300px",
                        backgroundColor: "#eee",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        borderRadius: "6px",
                      }}
                    >
                      <span style={{ color: "#aaa" }}>Geen cover</span>
                    </div>
                  )}
                </div>

                <div style={{ flex: "1 1 300px", minWidth: "0" }}>
                  <div style={{ overflowX: "auto" }}>
                    <table
                      style={{
                        width: "100%",
                        borderCollapse: "collapse",
                        textAlign: "left",
                        minWidth: "250px",
                      }}
                    >
                      <tbody>
                        <tr style={{ borderBottom: "1px solid #ddd" }}>
                          <th
                            style={{
                              padding: "1rem 0",
                              color: "#8e2446",
                              width: "130px",
                            }}
                          >
                            ISBN
                          </th>
                          <td
                            style={{
                              padding: "1rem 0",
                              fontWeight: "bold",
                              color: "#333",
                            }}
                          >
                            {selectedBook.isbn || "-"}
                          </td>
                        </tr>
                        <tr style={{ borderBottom: "1px solid #ddd" }}>
                          <th style={{ padding: "1rem 0", color: "#8e2446" }}>
                            Uitgeverij
                          </th>
                          <td style={{ padding: "1rem 0", color: "#333" }}>
                            {selectedBook.publisher || "-"}
                          </td>
                        </tr>
                        <tr style={{ borderBottom: "1px solid #ddd" }}>
                          <th style={{ padding: "1rem 0", color: "#8e2446" }}>
                            Uitgavejaar
                          </th>
                          <td style={{ padding: "1rem 0", color: "#333" }}>
                            {selectedBook.publishedYear || "-"}
                          </td>
                        </tr>
                        <tr style={{ borderBottom: "1px solid #ddd" }}>
                          <th style={{ padding: "1rem 0", color: "#8e2446" }}>
                            Pagina's
                          </th>
                          <td style={{ padding: "1rem 0", color: "#333" }}>
                            {selectedBook.pageCount || "-"}
                          </td>
                        </tr>
                        <tr style={{ borderBottom: "1px solid #ddd" }}>
                          <th style={{ padding: "1rem 0", color: "#8e2446" }}>
                            Categorie
                          </th>
                          <td style={{ padding: "1rem 0", color: "#333" }}>
                            {selectedBook.categories?.join(", ") || "-"}
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>

                  <div style={{ marginTop: "2rem" }}>
                    <h3
                      style={{
                        fontSize: "1.2rem",
                        color: "#8e2446",
                        marginBottom: "1rem",
                      }}
                    >
                      Samenvatting
                    </h3>
                    <p
                      style={{
                        lineHeight: "1.6",
                        color: "#444",
                        whiteSpace: "pre-wrap",
                      }}
                    >
                      {selectedBook.description ||
                        "Geen samenvatting beschikbaar voor dit boek."}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
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
                {/* Filter Pills */}
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
      {showDeleteConfirm && selectedBook && (
        <div className="modalOverlay">
          <div className="modalBox">
            <p>
              Ben je zeker dat je <strong>{selectedBook.title}</strong> wilt
              verwijderen?
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
  );
}
