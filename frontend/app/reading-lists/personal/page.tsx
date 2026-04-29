"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import { Book } from "../../interfaces/Book";
import ProtectedRoute from "../../components/ProtectedRoute";
import "./myReadingList.css";

interface PersonalList {
  id: number;
  title: string;
  taskDescription?: string | null;
  bookIds: number[];
  ownList: boolean;
  listType: "PERSONAL" | "CLASS";
}

type ViewState = "overview" | "create";

export default function MyReadingListPage() {
  const { user } = useAuth();
  const router = useRouter();
  const searchParams = useSearchParams();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const [view, setView] = useState<ViewState>("overview");
  const [activeList, setActiveList] = useState<PersonalList | null>(null);

  const [myLists, setMyLists] = useState<PersonalList[]>([]);
  const [listsLoading, setListsLoading] = useState(true);

  const [formTitle, setFormTitle] = useState("");
  const [formDescription, setFormDescription] = useState("");
  const [formBooks, setFormBooks] = useState<Book[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [allBooks, setAllBooks] = useState<Book[]>([]);
  const [formLoading, setFormLoading] = useState(false);
  const [formMsg, setFormMsg] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const [deleteConfirm, setDeleteConfirm] = useState<number | null>(null);

  const booksById = useCallback(() => {
    const map = new Map<number, Book>();
    allBooks.forEach((b) => map.set(b.id, b));
    return map;
  }, [allBooks]);

  const listBooks = (list: PersonalList | null): Book[] => {
    if (!list) return [];
    const map = booksById();
    return list.bookIds
      .map((id) => map.get(id))
      .filter((b): b is Book => Boolean(b));
  };

  const fetchMyLists = useCallback(() => {
    if (!user) return;
    setListsLoading(true);

    fetch(`${apiUrl}/reading-lists`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : []))
      .then((data: PersonalList[]) => {
        const personalOwn = (Array.isArray(data) ? data : []).filter(
          (l) => l.listType === "PERSONAL" && l.ownList,
        );
        setMyLists(personalOwn);
      })
      .catch(console.error)
      .finally(() => setListsLoading(false));
  }, [user, apiUrl]);

  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`, { credentials: "include" })
      .then((r) => (r.ok ? r.json() : []))
      .then((data: Book[]) => setAllBooks(Array.isArray(data) ? data : []))
      .catch(console.error);
  }, [apiUrl]);

  useEffect(() => {
    fetchMyLists();
  }, [fetchMyLists]);

  useEffect(() => {
    const editId = Number(searchParams.get("edit"));
    if (!editId || myLists.length === 0 || allBooks.length === 0) return;
    const list = myLists.find((l) => l.id === editId);
    if (list) openEdit(list);
  }, [searchParams, myLists, allBooks]);

  const openEdit = (list: PersonalList) => {
    setFormTitle(list.title);
    setFormDescription(list.taskDescription || "");
    setFormBooks(listBooks(list));
    setActiveList(list);
    setView("create");
    setFormMsg(null);
  };

  const openCreate = () => {
    setFormTitle("");
    setFormDescription("");
    setFormBooks([]);
    setSearchQuery("");
    setActiveList(null);
    setView("create");
    setFormMsg(null);
  };

  const filteredBooks = allBooks.filter((book) => {
    const q = searchQuery.toLowerCase();
    return (
      book.title?.toLowerCase().includes(q) ||
      book.authors?.some((a) => a.toLowerCase().includes(q)) ||
      book.isbn?.toLowerCase().includes(q)
    );
  });

  const addBook = (book: Book) => {
    if (formBooks.some((b) => b.id === book.id)) return;
    setFormBooks((prev) => [...prev, book]);
  };

  const removeBook = (id: number) => {
    setFormBooks((prev) => prev.filter((b) => b.id !== id));
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!formTitle.trim()) {
      setFormMsg({ type: "error", text: "Een titel is verplicht." });
      return;
    }

    setFormLoading(true);
    setFormMsg(null);

    const payload = {
      title: formTitle.trim(),
      taskDescription: formDescription.trim() || null,
      bookIds: formBooks.map((b) => b.id),
    };

    const isEdit = Boolean(activeList?.id);
    const url = isEdit
      ? `${apiUrl}/reading-lists/personal/${activeList?.id}`
      : `${apiUrl}/reading-lists/personal`;

    try {
      const res = await fetch(url, {
        method: isEdit ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error();

      setFormMsg({
        type: "success",
        text: isEdit ? "Leeslijst bijgewerkt." : "Leeslijst aangemaakt.",
      });

      fetchMyLists();

      if (!isEdit) {
        setFormTitle("");
        setFormDescription("");
        setFormBooks([]);
        setTimeout(() => {
          setView("overview");
          setFormMsg(null);
        }, 1200);
      }
    } catch {
      setFormMsg({ type: "error", text: "Opslaan mislukt. Probeer opnieuw." });
    } finally {
      setFormLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      const res = await fetch(`${apiUrl}/reading-lists/personal/${id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!res.ok) throw new Error();

      setMyLists((prev) => prev.filter((l) => l.id !== id));
    } catch {
      alert("Verwijderen mislukt.");
    } finally {
      setDeleteConfirm(null);
    }
  };

  return (
    <ProtectedRoute
      allowedRoles={["STUDENT", "TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"]}
    >
      <div className="mrl-page">
        {view === "overview" && (
          <>
            <div className="mrl-header">
              <div>
                <h1>Mijn leeslijsten</h1>
                <p>Maak persoonlijke lijsten met boeken die jij wilt lezen.</p>
              </div>
              <div className="mrl-header-actions">
                <button
                  className="mrl-btn-outline"
                  onClick={() => router.push("/reading-lists")}
                >
                  Terug naar alle leeslijsten
                </button>
                <button className="mrl-btn-primary" onClick={openCreate}>
                  Nieuwe lijst
                </button>
              </div>
            </div>

            {listsLoading && (
              <div className="mrl-state">
                <div className="mrl-spinner" />
                <p>Laden...</p>
              </div>
            )}

            {!listsLoading && myLists.length === 0 && (
              <div className="mrl-state mrl-state--empty">
                <h2>Je hebt nog geen leeslijsten</h2>
                <p>Begin met jouw eerste persoonlijke leeslijst.</p>
                <button className="mrl-btn-primary" onClick={openCreate}>
                  Maak mijn eerste lijst
                </button>
              </div>
            )}

            {!listsLoading && myLists.length > 0 && (
              <div className="mrl-list-grid">
                {myLists.map((list) => {
                  const count = list.bookIds?.length ?? 0;
                  return (
                    <div key={list.id} className="mrl-list-card">
                      <div
                        className="mrl-list-card-body"
                        onClick={() => router.push(`/reading-lists/${list.id}`)}
                      >
                        <div>
                          <h3>{list.title}</h3>
                          {list.taskDescription && (
                            <p>{list.taskDescription}</p>
                          )}
                          <span className="mrl-book-count">
                            {count} {count === 1 ? "boek" : "boeken"}
                          </span>
                        </div>
                      </div>

                      <div className="mrl-list-card-actions">
                        <button
                          className="mrl-btn-outline"
                          onClick={() =>
                            router.push(`/reading-lists/${list.id}`)
                          }
                        >
                          Bekijken
                        </button>
                        <button
                          className="mrl-btn-outline"
                          onClick={() => openEdit(list)}
                        >
                          Bewerken
                        </button>

                        {deleteConfirm === list.id ? (
                          <span className="mrl-delete-row">
                            Verwijderen?
                            <button
                              className="mrl-btn-sm-danger"
                              onClick={() => handleDelete(list.id)}
                            >
                              Ja
                            </button>
                            <button
                              className="mrl-btn-sm-ghost"
                              onClick={() => setDeleteConfirm(null)}
                            >
                              Nee
                            </button>
                          </span>
                        ) : (
                          <button
                            className="mrl-btn-trash"
                            onClick={() => setDeleteConfirm(list.id)}
                            title="Verwijderen"
                            aria-label="Verwijderen"
                          ></button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </>
        )}

        {view === "create" && (
          <>
            <div className="mrl-subheader">
              <button
                className="mrl-back-btn"
                onClick={() => setView("overview")}
              >
                Terug naar overzicht
              </button>
              <h1>{activeList ? "Leeslijst bewerken" : "Nieuwe leeslijst"}</h1>
            </div>

            {formMsg && (
              <div
                className={
                  formMsg.type === "success"
                    ? "mrl-msg mrl-msg--success"
                    : "mrl-msg mrl-msg--error"
                }
              >
                {formMsg.text}
              </div>
            )}

            <form onSubmit={handleSave} className="mrl-create-form">
              <div className="mrl-panel mrl-panel--picker">
                <label className="mrl-panel-label">
                  Boeken zoeken en toevoegen
                </label>
                <input
                  type="text"
                  className="mrl-search-input"
                  placeholder="Zoek op titel, auteur of ISBN..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
                <div className="mrl-book-list">
                  {allBooks.length === 0 && (
                    <p className="mrl-hint">Catalogus laden...</p>
                  )}
                  {allBooks.length > 0 && filteredBooks.length === 0 && (
                    <p className="mrl-hint">Geen boeken gevonden.</p>
                  )}

                  {filteredBooks.map((book) => {
                    const isAdded = formBooks.some((b) => b.id === book.id);
                    return (
                      <div
                        key={book.id}
                        className={`mrl-book-row ${isAdded ? "mrl-book-row--added" : ""}`}
                      >
                        <div className="mrl-thumb">
                          {book.thumbnail ? (
                            <img src={book.thumbnail} alt={book.title} />
                          ) : (
                            <span>Geen cover</span>
                          )}
                        </div>
                        <div className="mrl-book-info">
                          <strong>{book.title}</strong>
                          <span>{book.authors?.join(", ") || "Onbekend"}</span>
                        </div>
                        <button
                          type="button"
                          className="mrl-add-btn"
                          onClick={() => addBook(book)}
                          disabled={isAdded}
                        >
                          {isAdded ? "✓" : "+"}
                        </button>
                      </div>
                    );
                  })}
                </div>
              </div>

              <div className="mrl-panel mrl-panel--details">
                <div className="mrl-form-fields">
                  <div className="mrl-field">
                    <label htmlFor="mrl-title">Naam van de lijst *</label>
                    <input
                      id="mrl-title"
                      type="text"
                      className="mrl-input"
                      placeholder="Bijv. Zomervakantie lezen"
                      value={formTitle}
                      onChange={(e) => setFormTitle(e.target.value)}
                      required
                    />
                  </div>
                  <div className="mrl-field">
                    <label htmlFor="mrl-desc">Beschrijving (optioneel)</label>
                    <textarea
                      id="mrl-desc"
                      className="mrl-textarea"
                      placeholder="Waarom maak je deze lijst?"
                      value={formDescription}
                      onChange={(e) => setFormDescription(e.target.value)}
                      rows={3}
                    />
                  </div>
                </div>

                <div className="mrl-selected-header">
                  <label>Geselecteerde boeken ({formBooks.length})</label>
                </div>

                <div className="mrl-selected-list">
                  {formBooks.length === 0 ? (
                    <div className="mrl-selected-empty">
                      <p>Gebruik de zoekbalk links om boeken toe te voegen.</p>
                    </div>
                  ) : (
                    formBooks.map((book) => (
                      <div key={book.id} className="mrl-selected-row">
                        <div className="mrl-thumb mrl-thumb--sm">
                          {book.thumbnail ? (
                            <img src={book.thumbnail} alt={book.title} />
                          ) : (
                            <span>Geen cover</span>
                          )}
                        </div>
                        <div className="mrl-book-info">
                          <strong>{book.title}</strong>
                          <span>{book.authors?.join(", ") || "Onbekend"}</span>
                        </div>
                        <button
                          type="button"
                          className="mrl-remove-btn"
                          onClick={() => removeBook(book.id)}
                          title="Verwijderen uit lijst"
                        >
                          Verwijder
                        </button>
                      </div>
                    ))
                  )}
                </div>

                <div className="mrl-form-actions">
                  <button
                    type="button"
                    className="mrl-btn-cancel"
                    onClick={() => setView("overview")}
                    disabled={formLoading}
                  >
                    Annuleer
                  </button>
                  <button
                    type="submit"
                    className="mrl-btn-primary mrl-submit-btn"
                    disabled={formLoading}
                  >
                    {formLoading
                      ? "Opslaan..."
                      : activeList
                        ? "Wijzigingen opslaan"
                        : "Lijst aanmaken"}
                  </button>
                </div>
              </div>
            </form>
          </>
        )}
      </div>
    </ProtectedRoute>
  );
}
