"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../../context/AuthContext";
import { Book } from "../../interfaces/Book";
import ProtectedRoute from "../../components/ProtectedRoute";
import "./createReadingList.css";

export default function CreateReadingListPage() {
  const { user } = useAuth();
  const router = useRouter();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const [title, setTitle] = useState("");
  const [deadline, setDeadline] = useState("");
  const [taskDescription, setTaskDescription] = useState("");
  
  const [selectedBooks, setSelectedBooks] = useState<Book[]>([]);
  const [allBooks, setAllBooks] = useState<Book[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Book[]) => setAllBooks(data))
      .catch((err) => console.error("Fout bij ophalen boeken:", err));
  }, [apiUrl]);

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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // @ts-ignore
    const finalCreatorUid = user?.smartschoolUid || user?.userID || user?.uid || user?.id;

    if (!user || !finalCreatorUid) {
      setMessage({ type: "error", text: "Fout: Je bent niet correct ingelogd (Geen ID gevonden)." });
      return; 
    }
    
    if (!title.trim() || !deadline) {
      setMessage({ type: "error", text: "Titel en deadline zijn verplicht." });
      return;
    }

    if (selectedBooks.length === 0) {
      setMessage({ type: "error", text: "Voeg minstens één boek toe aan de leeslijst." });
      return;
    }

    setLoading(true);
    setMessage(null);

    const payload = {
      title,
      taskDescription,
      deadline: deadline.length === 16 ? `${deadline}:00` : deadline, 
      creatorId: finalCreatorUid, 
      bookIds: selectedBooks.map(b => b.id),
    };

    try {
      const res = await fetch(`${apiUrl}/reading-lists`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) throw new Error("Fout bij opslaan");

      // Toon succesbericht (zonder redirect tekst)
      setMessage({ type: "success", text: "Leeslijst succesvol aangemaakt!" });
      
      // Maak alle velden weer leeg
      setTitle("");
      setDeadline("");
      setTaskDescription("");
      setSelectedBooks([]);
      setSearchQuery("");

      // Optioneel: Haal het succesbericht na 5 seconden weer weg
      setTimeout(() => {
        setMessage(null);
      }, 5000);

    } catch (err) {
      setMessage({ type: "error", text: "Kon de leeslijst niet aanmaken. Probeer opnieuw." });
    } finally {
      setLoading(false);
    }
  };  

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"]}>
      <div className="readingListContainer">
        
        <h1 className="pageTitle">Nieuwe leeslijst aanmaken</h1>

        {message && (
          <div className={message.type === "success" ? "msgSuccess" : "msgError"}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', flex: 1 }}>
          
          <div className="info-island">
            <div className="info-row-top">
              <div className="inputGroup">
                <label htmlFor="title">1. Titel van de leeslijst *</label>
                <input
                  id="title"
                  type="text"
                  className="textInput"
                  placeholder="Bijv. Verplichte literatuur 5IT - Q2"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  required
                />
              </div>
              <div className="inputGroup">
                <label htmlFor="deadline">2. Deadline *</label>
                <input
                  id="deadline"
                  type="datetime-local"
                  className="textInput"
                  value={deadline}
                  onChange={(e) => setDeadline(e.target.value)}
                  required
                />
              </div>
            </div>
            
            <div className="inputGroup">
              <label htmlFor="taskDescription">3. Algemene opdrachtomschrijving</label>
              <textarea
                id="taskDescription"
                className="textAreaInput"
                placeholder="Wat moeten de leerlingen doen met deze boeken?"
                value={taskDescription}
                onChange={(e) => setTaskDescription(e.target.value)}
              />
            </div>
          </div>

          <div className="manage-wrapper">
            
            {/* LINKER EILAND */}
            <div className="eiland-common book-selector-island">
              <div className="search-container">
                <label style={{ fontWeight: "bold", color: "#8e2446", display: "block", marginBottom: "0.5rem", fontSize: "0.9rem", textTransform: "uppercase" }}>
                  4. Zoek boeken
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
                {allBooks.length === 0 ? (
                  <p style={{textAlign: 'center', color: '#888', marginTop: '2rem'}}>Catalogus laden...</p>
                ) : filteredBooks.length === 0 ? (
                   <p style={{textAlign: 'center', color: '#888', marginTop: '2rem'}}>Geen boeken gevonden.</p>
                ) : (
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
                            <p className="book-list-authors">
                              {book.authors ? book.authors.join(", ") : "Onbekend"}
                            </p>
                          </div>

                          <button
                            type="button"
                            className="add-btn"
                            onClick={() => addBookToList(book)}
                            disabled={isAdded}
                          >
                            {isAdded ? "Toegevoegd" : "+ Voeg toe"}
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>

            {/* RECHTER EILAND */}
            <div className="eiland-common form-details-island">
              
              <div className="form-details-content">
                <div className="inputGroup" style={{marginBottom: '1.5rem'}}>
                    <label>5. Boeken op deze lijst ({selectedBooks.length})</label>
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
                          
                          {/* NIEUW: Boek info met thumbnail en auteur onder elkaar */}
                          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flex: 1 }}>
                            <div className="book-list-thumb" style={{ width: '40px', height: '60px' }}>
                              {book.thumbnail && book.thumbnail.trim() !== "" ? (
                                <img src={book.thumbnail} alt={book.title} />
                              ) : (
                                <span>Geen cover</span>
                              )}
                            </div>
                            <div className="book-list-info">
                              <h3 className="book-list-title" style={{ fontSize: '1rem', marginBottom: '0.2rem' }}>
                                {book.title}
                              </h3>
                              <p className="book-list-authors" style={{ margin: 0 }}>
                                door {book.authors?.join(", ") || "Onbekend"}
                              </p>
                            </div>
                          </div>

                          <button
                            type="button"
                            className="remove-btn"
                            onClick={() => removeBookFromList(book.id!)}
                          >
                            ✕ Verwijder
                          </button>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="submit-container">
                  <button type="submit" className="titleSubmitBtn" style={{width: '100%', alignSelf: 'center'}} disabled={loading}>
                    {loading ? "Lijst opslaan..." : "Leeslijst aanmaken"}
                  </button>
              </div>

            </div>

          </div>
        </form>
      </div>
    </ProtectedRoute>
  );
}