"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Book } from "../interfaces/Book";
import { SmartschoolUser } from "../interfaces/SmartschoolUser";
import "./returns.css";

// Interface voor de boeken die de lener momenteel heeft (gebundeld per boek)
interface BorrowedItem {
  book: Book;
  quantityBorrowed: number;
}

// Interface voor het retourmandje
interface ReturnCartItem {
  book: Book;
  quantityToReturn: number;
  maxQuantity: number;
}

export default function ReturnsPage() {
  const router = useRouter();

  // --- Kolom 1: Lener States ---
  const [userQuery, setUserQuery] = useState("");
  const [userSearchResults, setUserSearchResults] = useState<SmartschoolUser[]>([]);
  const [selectedUser, setSelectedUser] = useState<SmartschoolUser | null>(null);

  // --- Kolom 2: Uitgeleende boeken States ---
  const [borrowedBooks, setBorrowedBooks] = useState<BorrowedItem[]>([]);
  const [bookQuery, setBookQuery] = useState("");
  
  // --- Kolom 3: Retour Mandje States ---
  const [returnCart, setReturnCart] = useState<ReturnCartItem[]>([]);

  // --- 1. Lener Zoeken & Selecteren ---
  const handleSearchSmartschoolUser = async () => {
    if (!userQuery.trim()) {
      setUserSearchResults([]);
      return;
    }

    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/smartschool/users?query=${encodeURIComponent(userQuery.trim())}`,
        { credentials: "include" }
      );

      if (!response.ok) throw new Error("Failed to fetch Smartschool users");
      const data = await response.json();
      setUserSearchResults(data);
    } catch (err) {
      console.error("Error searching users:", err);
      alert("Kan momenteel geen verbinding maken met de Smartschool API.");
    }
  };

  const handleSelectUser = async (user: SmartschoolUser) => {
    setSelectedUser(user);
    setUserSearchResults([]);
    setUserQuery("");
    setReturnCart([]); // Leegmandje bij nieuwe gebruiker
    fetchUserLoans(user.smartschoolUserId); // Haal direct de leningen op
  };

  const handleRemoveUser = () => {
    setSelectedUser(null);
    setBorrowedBooks([]);
    setReturnCart([]);
  };

  // --- 2. Ophalen van actieve leningen via Backend ---
  const fetchUserLoans = async (smartschoolUserId: string) => {
    try {
      // LET OP: Pas deze URL aan naar jouw backend endpoint voor actieve leningen per user!
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/loans/active?smartschoolUserId=${smartschoolUserId}`,
        { credentials: "include" }
      );
      if (!response.ok) throw new Error("Kan leningen niet ophalen");
      
      const data = await response.json();
      
      // We groeperen de losse leningen per boek, zodat we het aantal (quantity) weten.
      const groupedBooks: Record<number, BorrowedItem> = {};
      
      data.forEach((loan: any) => {
        const bookId = loan.book.id;
        if (!groupedBooks[bookId]) {
          groupedBooks[bookId] = { book: loan.book, quantityBorrowed: 0 };
        }
        groupedBooks[bookId].quantityBorrowed += loan.quantity;
      });
      
      setBorrowedBooks(Object.values(groupedBooks));
    } catch (err) {
      console.error(err);
      alert("Fout bij het ophalen van de uitgeleende boeken voor deze gebruiker.");
    }
  };

  // Lokale zoekfilter voor de uitgeleende boeken (want we hebben ze al opgehaald)
  const filteredBorrowedBooks = borrowedBooks.filter(item => 
    item.book.title.toLowerCase().includes(bookQuery.toLowerCase()) || 
    item.book.authors?.some(a => a.toLowerCase().includes(bookQuery.toLowerCase()))
  );

  // --- 3. Return Cart Handlers ---
  const handleAddToReturnCart = (borrowedItem: BorrowedItem) => {
    setReturnCart((prev) => {
      const existing = prev.find((i) => i.book.id === borrowedItem.book.id);
      if (existing) return prev;
      
      return [
        ...prev,
        {
          book: borrowedItem.book,
          quantityToReturn: 1, // Startwaarde
          maxQuantity: borrowedItem.quantityBorrowed, // Max = wat ze in bezit hebben
        },
      ];
    });
  };

  const updateReturnQuantity = (bookId: number, delta: number) => {
    setReturnCart((prev) =>
      prev.map((item) => {
        if (item.book.id === bookId) {
          const newQty = item.quantityToReturn + delta;
          // Zorg dat we niet minder dan 1 of meer dan het geleende aantal terugbrengen
          if (newQty >= 1 && newQty <= item.maxQuantity) {
            return { ...item, quantityToReturn: newQty };
          }
        }
        return item;
      })
    );
  };

  const handleRemoveFromReturnCart = (bookId: number) => {
    setReturnCart(returnCart.filter((item) => item.book.id !== bookId));
  };

  // --- 4. Acties: Inleveren of Annuleren ---
  const handleCancel = () => {
    setReturnCart([]);
    setSelectedUser(null);
    setBorrowedBooks([]);
    setUserSearchResults([]);
    setBookQuery("");
    setUserQuery("");
  };

  const handleRegisterReturn = async () => {
    if (returnCart.length === 0 || !selectedUser) return;

    // We sturen dit naar de backend. De backend regelt de datums en de uitleengeschiedenis-tabel!
    const payload = returnCart.map((item) => ({
      bookId: item.book.id,
      quantity: item.quantityToReturn,
      smartschoolUserId: selectedUser.smartschoolUserId
    }));

    try {
      // LET OP: Pas deze URL aan naar jouw return endpoint
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/loans/return`, {
        method: "POST", // of PUT afhankelijk van jullie backend design
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error("Fout bij het registreren van de retour.");

      alert(`Succes! De boeken van ${selectedUser.name} zijn succesvol ingeleverd.`);
      
      // Mandje leegmaken en de lijst met uitgeleende boeken opnieuw inladen
      setReturnCart([]);
      fetchUserLoans(selectedUser.smartschoolUserId);
      setBookQuery("");
      
    } catch (err) {
      console.error(err);
      alert("Er ging iets mis bij het inleveren van de boeken. Controleer de verbinding.");
    }
  };

  return (
    <main className="pageLayout">
      <div className="pageHeader">
        <button className="backButton" onClick={() => router.back()}>←</button>
        <h1>Boeken Inleveren</h1>
      </div>

      <div className="uitleenGrid driekolomsGrid">
        
        {/* --- KOLOM 1: LENER SELECTEREN --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader">
            <h2>Lener (Retour)</h2>
          </div>

          <div className="userProfileCard">
            {selectedUser ? (
              <>
                {selectedUser.photoUrl ? (
                  <img src={selectedUser.photoUrl} alt="Profile" className="userPhotoPlaceholder cover" />
                ) : (
                  <div className="userPhotoPlaceholder">👤</div>
                )}
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

          <div className="sectieHeader margined">
            <h2>Lener zoeken</h2>
          </div>
          <div className="searchbar">
            <input
              type="text"
              placeholder="Naam of ID..."
              value={userQuery}
              onChange={(e) => setUserQuery(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && handleSearchSmartschoolUser()}
            />
            <button id="searchButton" onClick={handleSearchSmartschoolUser}>🔎︎</button>
          </div>

          <div className="resultsFrame">
            {userSearchResults.length === 0 ? (
              <p className="placeholderText centered">Typ een deel van de naam in.</p>
            ) : (
              userSearchResults.map((user, idx) => (
                <div key={idx} className="listItem">
                  {user.photoUrl ? (
                    <img src={user.photoUrl} alt="Profile" className="userPhotoSmall cover" />
                  ) : (
                    <div className="userPhotoSmall">👤</div>
                  )}
                  <div className="itemDetails">
                    <strong>{user.name}</strong>
                    <span>{user.classGroup}</span>
                  </div>
                  <button className="actionBtn addBtn" onClick={() => handleSelectUser(user)}>
                    {selectedUser?.smartschoolUserId === user.smartschoolUserId ? "✓" : "+"}
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* --- KOLOM 2: UITGELEENDE BOEKEN LIJST --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader">
            <h2>Uitgeleende Boeken</h2>
          </div>

          <div className="searchbar">
            <input
              type="text"
              placeholder="Filter uitgeleende boeken..."
              value={bookQuery}
              onChange={(e) => setBookQuery(e.target.value)}
              disabled={!selectedUser} // Alleen zoeken als er een user is
            />
          </div>

          <div className="resultsFrame">
            {!selectedUser ? (
              <p className="placeholderText centered">Selecteer eerst een lener.</p>
            ) : borrowedBooks.length === 0 ? (
              <p className="placeholderText centered">Deze persoon heeft geen boeken in bezit.</p>
            ) : filteredBorrowedBooks.length === 0 ? (
              <p className="placeholderText centered">Geen boeken gevonden met deze filter.</p>
            ) : (
              filteredBorrowedBooks.map((item) => {
                const isSelectedForReturn = returnCart.find((c) => c.book.id === item.book.id);

                return (
                  <div key={item.book.id} className="listItem">
                    <img src={item.book.thumbnail || "/book-closed.png"} alt="cover" className="itemThumbnail"/>
                    <div className="itemDetails">
                      <strong>{item.book.title}</strong>
                      <span>{item.book.authors?.join(", ")}</span>
                    </div>

                    {isSelectedForReturn ? (
                      <div className="addArea">
                        <span className="stock-ok">{item.quantityBorrowed} in bezit</span>
                        <div className="quantityControl syncedControl">
                          <button className="qtyBtn" onClick={() => updateReturnQuantity(item.book.id, -1)}>-</button>
                          <span className="qtyDisplay">{isSelectedForReturn.quantityToReturn}</span>
                          <button className="qtyBtn" onClick={() => updateReturnQuantity(item.book.id, 1)}>+</button>
                        </div>
                      </div>
                    ) : (
                      <div className="addArea">
                        <span className="stock-ok">{item.quantityBorrowed} in bezit</span>
                        <button className="actionBtn addBtn" onClick={() => handleAddToReturnCart(item)} title="Voeg toe aan retour">
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

        {/* --- KOLOM 3: RETOUR MANDJE --- */}
        <div className="gridColumn flexBetween">
          <div className="flexColumnGrow">
            <div className="sectieHeader">
              <h2>
                Terug te brengen ({returnCart.reduce((total, item) => total + item.quantityToReturn, 0)})
              </h2>
            </div>

            <div className="resultsFrame">
              {returnCart.length === 0 ? (
                <p className="placeholderText centered">Geen boeken geselecteerd voor inlevering.</p>
              ) : (
                returnCart.map((item) => (
                  <div key={item.book.id} className="listItem selectedItem">
                    <img src={item.book.thumbnail || "/book-closed.png"} alt="cover" className="itemThumbnail"/>
                    <div className="itemDetails">
                      <strong>{item.book.title}</strong>
                      <div className="quantityControl">
                        <button className="qtyBtn" onClick={() => updateReturnQuantity(item.book.id, -1)}>-</button>
                        <span className="qtyDisplay">{item.quantityToReturn}</span>
                        <button className="qtyBtn" onClick={() => updateReturnQuantity(item.book.id, 1)}>+</button>
                      </div>
                    </div>
                    <button className="actionBtn removeBtn" onClick={() => handleRemoveFromReturnCart(item.book.id)} title="Verwijder uit selectie">
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="actionFooter">
            <button className="primaryBtn" onClick={handleRegisterReturn} disabled={returnCart.length === 0 || !selectedUser}>
              Geselecteerde boeken inleveren
            </button>
            <button className="secondaryBtn" onClick={handleCancel} disabled={returnCart.length === 0 && !selectedUser && bookQuery === "" && userQuery === ""}>
              Annuleren / Schoonmaken
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}