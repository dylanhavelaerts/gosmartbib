"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import { Book } from "../../interfaces/Book";
import ProtectedRoute from "../../components/ProtectedRoute";
import "../create/createReadingList.css"; 

export default function EditReadingListPage() {
  const { user } = useAuth();
  const router = useRouter();
  const params = useParams(); // Pakt de ID uit de URL
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const [listDetails, setListDetails] = useState<any>(null);
  const [selectedBooks, setSelectedBooks] = useState<Book[]>([]);
  const [allBooks, setAllBooks] = useState<Book[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  // 1. Haal alle boeken én de specifieke leeslijst op
  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Book[]) => setAllBooks(data));

    if (params.id) {
      fetch(`${apiUrl}/reading-lists/${params.id}`, { credentials: "include" })
        .then((res) => (res.ok ? res.json() : null))
        .then((data) => {
          if (data) {
            setListDetails(data);
            setSelectedBooks(data.books || []);
          }
        });
    }
  }, [apiUrl, params.id]);

  const filteredBooks = allBooks.filter((book) => {
    const q = searchQuery.toLowerCase();
    return (
      book.title?.toLowerCase().includes(q) ||
      book.authors?.some((a) => a.toLowerCase().includes(q)) ||
      book.isbn?.toLowerCase().includes(q)
    );
  });

  const addBookToList = (book: Book) => {
    if (selectedBooks.some((b) => b.id === book.id)) return;
    setSelectedBooks([...selectedBooks, book]);
  };

  const removeBookFromList = (bookId: number) => {
    setSelectedBooks(selectedBooks.filter((b) => b.id !== bookId));
  };

  const handleSaveChanges = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setMessage(null);

    const payload = selectedBooks.map(b => b.id);

    try {
      const res = await fetch(`${apiUrl}/reading-lists/${params.id}/books`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error("Fout bij updaten");

      setMessage({ type: "success", text: "Boekenlijst succesvol bijgewerkt!" });
      setTimeout(() => setMessage(null), 3000);
    } catch (err) {
      setMessage({ type: "error", text: "Kon de lijst niet bijwerken." });
    } finally {
      setLoading(false);
    }
  };

  if (!listDetails) return <div className="readingListContainer">Laden...</div>;

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"]}>
      <div className="readingListContainer">
        
        <div style={{ display: "flex", gap: "1rem", alignItems: "center", marginBottom: "1.5rem" }}>
          <button onClick={() => router.push("/reading-lists")} className="back-btn">
            <span>←</span> Terug
          </button>
          <h1 className="pageTitle" style={{ margin: 0 }}>Beheer: {listDetails.title}</h1>
        </div>

        {message && (
          <div className={message.type === "success" ? "msgSuccess" : "msgError"}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSaveChanges} style={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0 }}>
          <div className="manage-wrapper">
            
            {/* LINKER EILAND */}
            <div className="eiland-common book-selector-island">
              <div className="search-container">
                <label style={{ fontWeight: "bold", color: "#8e2446", display: "block", marginBottom: "0.5rem", fontSize: "0.9rem", textTransform: "uppercase" }}>
                  Zoek boeken om toe te voegen
                </label>
                <input
                  type="text"
                  className="search-input"
                  placeholder="Titel, auteur of ISBN..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>

              <div className="list-container">
                <ul className="book-list">
                  {filteredBooks.map((book) => {
                    const isAdded = selectedBooks.some((b) => b.id === book.id);
                    return (
                      <li key={book.id} className={`book-list-item ${isAdded ? 'added' : ''}`}>
                        
                        <div className="book-list-thumb">
                            {book.thumbnail && book.thumbnail.trim() !== "" ? (
                              <img src={book.thumbnail} alt={book.title} />
                            ) : (
                              <span>Geen cover</span>
                            )}
                        </div>

                        <div className="book-list-info">
                          <h3 className="book-list-title">{book.title}</h3>
                          <p className="book-list-authors">{book.authors?.join(", ") || "Onbekend"}</p>
                        </div>
                        <button type="button" className="add-btn" onClick={() => addBookToList(book)} disabled={isAdded}>
                          {isAdded ? "Toegevoegd" : "+ Voeg toe"}
                        </button>
                      </li>
                    );
                  })}
                </ul>
              </div>
            </div>

            {/* RECHTER EILAND */}
            <div className="eiland-common form-details-island">
              <div className="form-details-content">
                <div className="inputGroup" style={{marginBottom: '1.5rem'}}>
                    <label>Boeken op deze lijst ({selectedBooks.length})</label>
                </div>

                <div className="selectedBooksContainer">
                  {selectedBooks.length === 0 ? (
                    <div style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", color: "#888", border: '2px dashed #ddd', borderRadius: '7px', padding: '2rem' }}>
                      <p style={{ fontSize: "1.1rem", textAlign: "center", margin: 0 }}>
                        Gebruik de linkerlijst om boeken aan deze leeslijst toe te voegen.
                      </p>
                    </div>
                  ) : (
                    selectedBooks.map((book) => (
                      <div key={book.id} className="selectedBookCard" style={{ padding: '0.8rem 1.2rem' }}>
                        <div className="selectedBookHeader" style={{ marginBottom: 0, alignItems: 'center' }}>
                          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flex: 1, minWidth: 0 }}>
                            
                            <div className="book-list-thumb" style={{ width: '40px', height: '60px' }}>
                              {book.thumbnail && book.thumbnail.trim() !== "" ? (
                                <img src={book.thumbnail} alt={book.title} />
                              ) : (
                                <span>Geen cover</span>
                              )}
                            </div>

                            <div className="book-list-info" style={{ minWidth: 0 }}>
                              <h3 className="book-list-title" style={{ fontSize: '1rem', marginBottom: '0.2rem' }}>{book.title}</h3>
                              <p className="book-list-authors" style={{ margin: 0 }}>door {book.authors?.join(", ") || "Onbekend"}</p>
                            </div>
                          </div>
                          
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="submit-container">
                  <button type="submit" className="titleSubmitBtn" style={{width: '100%', alignSelf: 'center'}} disabled={loading}>
                    {loading ? "Bezig met opslaan..." : "Wijzigingen opslaan"}
                  </button>
              </div>
            </div>

          </div>
        </form>
      </div>
    </ProtectedRoute>
  );
}