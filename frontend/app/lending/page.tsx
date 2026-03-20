"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Book } from "../interfaces/Book";
import { SmartschoolUser } from "../interfaces/SmartschoolUser";
import "./lending.css";

// Interface voor items in het winkelmandje
interface CartItem {
  book: Book;
  quantity: number;
}

export default function UitleenPagina() {
  const router = useRouter();

  // --- States ---
  // Lener
  const [userQuery, setUserQuery] = useState("");
  const [userSearchResults, setUserSearchResults] = useState<SmartschoolUser[]>([]);
  const [selectedUser, setSelectedUser] = useState<SmartschoolUser | null>(null);

  // Boeken (Zoeklijst)
  const [bookQuery, setBookQuery] = useState("");
  const [searchResults, setSearchResults] = useState<Book[]>([]);
  
  // Winkelmandje (Cart)
  const [cart, setCart] = useState<CartItem[]>([]);

  // --- Lener Zoeken (Simulatie) ---
  const handleSearchSmartschoolUser = () => {
    if (!userQuery.trim()) {
      setUserSearchResults([]);
      return;
    }
    setUserSearchResults([
      { smartschoolUserId: "SS-987654", name: "Jan Peeters", classGroup: "5IT", school: "GO! Atheneum", schoolId: "GO-ANT-01" },
      { smartschoolUserId: "SS-112233", name: "Janssen Peter", classGroup: "6B", school: "GO! Atheneum", schoolId: "GO-ANT-01" }
    ]);
  };

  const handleSelectUser = (user: SmartschoolUser) => {
    setSelectedUser(user);
    setUserSearchResults([]); 
    setUserQuery(""); 
  };

  const handleRemoveUser = () => setSelectedUser(null);

  // --- Boeken Zoeken (Live Backend) ---
  useEffect(() => {
    if (!bookQuery || bookQuery.trim() === "") {
      setSearchResults([]);
      return;
    }

    const timer = setTimeout(() => {
      const params = new URLSearchParams();
      params.append("page", "0");
      params.append("size", "10"); 
      params.append("query", bookQuery.trim()); 

      fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/search?${params}`)
        .then((res) => res.json())
        .then((data) => setSearchResults(data.content || []))
        .catch((err) => console.error("Fout bij ophalen:", err));
    }, 300);

    return () => clearTimeout(timer);
  }, [bookQuery]);

  // --- Handlers voor Winkelmandje & Zoeklijst ---
  const handleAddToCart = (book: Book) => {
    const available = book.availableCopies !== undefined ? book.availableCopies : 5; 

    if (available <= 0) {
      alert("Dit boek is momenteel helaas niet beschikbaar.");
      return;
    }

    setCart((prev) => {
      const existingItem = prev.find((item) => item.book.id === book.id);
      if (existingItem) {
        return prev; 
      } else {
        return [...prev, { book, quantity: 1 }];
      }
    });
  };

  const updateQuantity = (bookId: number, delta: number) => {
    setCart((prev) => prev.map((item) => {
      if (item.book.id === bookId) {
        const available = item.book.availableCopies !== undefined ? item.book.availableCopies : 5;
        const newQuantity = item.quantity + delta;
        
        if (newQuantity >= 1 && newQuantity <= available) {
          return { ...item, quantity: newQuantity };
        }
      }
      return item;
    }));
  };

  const handleRemoveFromCart = (bookId: number) => {
    setCart(cart.filter((item) => item.book.id !== bookId));
  };

  // --- Registreren & Annuleren ---
  const handleAnnuleren = () => {
    // Maakt de volledige pagina leeg
    setCart([]);
    setSelectedUser(null);
    setSearchResults([]);
    setUserSearchResults([]);
    setBookQuery("");
    setUserQuery("");
  };

  const handleUitleenRegistreren = async () => {
    if (cart.length === 0 || !selectedUser) return;
    
    const payload = cart.map(item => ({
      bookId: item.book.id,
      quantity: item.quantity,
      user: {
        smartschoolUserId: selectedUser.smartschoolUserId,
        classGroup: selectedUser.classGroup,
        school: selectedUser.school,
        schoolId: selectedUser.schoolId
      }
    }));

    try {
      // Stuur de data naar de backend
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/loans`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error("Er is een fout opgetreden bij het registreren in de database.");
      }

      alert(`Succes! Uitlening correct geregistreerd aan ${selectedUser.name}.`);
      
      // Na succesvolle registratie maken we de pagina leeg (en verversen evt. we de zoekresultaten)
      handleAnnuleren();
      
    } catch (err) {
      console.error(err);
      alert("Er ging iets mis bij het uitlenen van de boeken. Controleer de verbinding en de voorraad.");
    }
  };

  return (
    <main className="pageLayout">
      <div className="pageHeader">
        <button className="backButton" onClick={() => router.back()}>←</button>
        <h1>Uitleen registreren</h1>
      </div>

      <div className="uitleenGrid driekolomsGrid">
        
        {/* --- KOLOM 1: LENER --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader"><h2>Geselecteerde Lener</h2></div>
          
          <div className="userProfileCard">
            {selectedUser ? (
              <>
                <div className="userPhotoPlaceholder">👤</div>
                <div className="userInfo">
                  <p className="userName">{selectedUser.name}</p>
                  <p><strong>ID:</strong> {selectedUser.smartschoolUserId}</p>
                  <p><strong>Klas:</strong> {selectedUser.classGroup}</p>
                </div>
                <button className="actionBtn removeBtn" onClick={handleRemoveUser}>✕</button>
              </>
            ) : (
              <p className="placeholderText">Nog geen lener geselecteerd.</p>
            )}
          </div>

          <div className="sectieHeader" style={{ marginTop: '1.5rem' }}><h2>Lener zoeken</h2></div>
          <div className="searchbar">
            <input type="text" placeholder="Naam, ID of klas..." value={userQuery} onChange={(e) => setUserQuery(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && handleSearchSmartschoolUser()} />
            <button id="searchButton" onClick={handleSearchSmartschoolUser}>🔎︎</button>
          </div>

          <div className="resultsFrame">
            {userSearchResults.length === 0 ? (
               <p className="placeholderText" style={{textAlign: "center", marginTop: "2rem"}}>Typ een naam of ID.</p>
            ) : (
              userSearchResults.map((user, idx) => (
                <div key={idx} className="listItem">
                  <div className="userPhotoSmall">👤</div>
                  <div className="itemDetails">
                    <strong>{user.name}</strong>
                    <span>{user.classGroup} • {user.smartschoolUserId}</span>
                  </div>
                  <button className="actionBtn addBtn" onClick={() => handleSelectUser(user)}>+</button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* --- KOLOM 2: BOEKEN ZOEKEN --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader"><h2>Boek zoeken</h2></div>
          
          <div className="searchbar">
            <input type="text" placeholder="Titel, auteur, ISBN..." value={bookQuery} onChange={(e) => setBookQuery(e.target.value)} />
            <button id="searchButton">🔎︎</button>
          </div>

          <div className="resultsFrame">
            {searchResults.length === 0 && bookQuery.trim() !== "" ? (
              <p className="placeholderText" style={{textAlign: "center", marginTop: "2rem"}}>Geen boeken gevonden.</p>
            ) : searchResults.length === 0 ? (
              <p className="placeholderText" style={{textAlign: "center", marginTop: "2rem"}}>Typ een zoekterm.</p>
            ) : (
              searchResults.map((book) => {
                const available = book.availableCopies !== undefined ? book.availableCopies : 5; 
                const cartItem = cart.find(item => item.book.id === book.id);

                return (
                  <div key={book.id} className="listItem">
                    <img src={book.thumbnail || "/book-closed.png"} alt="cover" className="itemThumbnail" />
                    <div className="itemDetails">
                      <strong>{book.title}</strong>
                      <span>{book.authors?.join(", ")}</span>
                    </div>

                    {cartItem ? (
                      // AANGEPAST: De stock-indicator staat hier nu ook!
                      <div className="addArea">
                        <span className={available > 0 ? "stock-ok" : "stock-empty"}>
                          {available > 0 ? `${available} vrij` : "Op"}
                        </span>
                        <div className="quantityControl syncedControl">
                          <button className="qtyBtn" onClick={() => updateQuantity(book.id, -1)}>-</button>
                          <span className="qtyDisplay">{cartItem.quantity}</span>
                          <button className="qtyBtn" onClick={() => updateQuantity(book.id, 1)}>+</button>
                        </div>
                      </div>
                    ) : (
                      // Hier stond de stock-indicator al
                      <div className="addArea">
                        <span className={available > 0 ? "stock-ok" : "stock-empty"}>
                          {available > 0 ? `${available} vrij` : "Op"}
                        </span>
                        <button 
                          className="actionBtn addBtn" 
                          disabled={available <= 0} 
                          onClick={() => handleAddToCart(book)}
                          title="Voeg eerste exemplaar toe"
                        >
                          +
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* --- KOLOM 3: GESELECTEERDE BOEKEN --- */}
        <div className="gridColumn flexBetween">
          <div style={{ display: "flex", flexDirection: "column", flexGrow: 1 }}>
            <div className="sectieHeader">
              <h2>Geselecteerde boeken ({cart.reduce((total, item) => total + item.quantity, 0)})</h2>
            </div>
            
            <div className="resultsFrame">
              {cart.length === 0 ? (
                <p className="placeholderText" style={{textAlign: "center", marginTop: "2rem"}}>Nog geen boeken.</p>
              ) : (
                cart.map((item) => (
                  <div key={item.book.id} className="listItem selectedItem">
                    <img src={item.book.thumbnail || "/book-closed.png"} alt="cover" className="itemThumbnail" />
                    
                    <div className="itemDetails">
                      <strong>{item.book.title}</strong>
                      
                      <div className="quantityControl">
                        <button className="qtyBtn" onClick={() => updateQuantity(item.book.id, -1)}>-</button>
                        <span className="qtyDisplay">{item.quantity}</span>
                        <button className="qtyBtn" onClick={() => updateQuantity(item.book.id, 1)}>+</button>
                      </div>

                    </div>
                    <button className="actionBtn removeBtn" onClick={() => handleRemoveFromCart(item.book.id)} title="Verwijder uit selectie">✕</button>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* ACTIE FOOTER: Twee knoppen onder elkaar */}
          <div className="actionFooter">
            <button 
              className="primaryBtn" 
              onClick={handleUitleenRegistreren} 
              disabled={cart.length === 0 || !selectedUser}
            >
              Boeken uitlenen
            </button>
            <button 
              className="secondaryBtn" 
              onClick={handleAnnuleren}
              disabled={cart.length === 0 && !selectedUser && bookQuery === "" && userQuery === ""}
            >
              Uitlenen annuleren
            </button>
          </div>
        </div>

      </div>
    </main>
  );
}