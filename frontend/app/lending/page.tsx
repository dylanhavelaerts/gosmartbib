"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Book } from "../interfaces/Book";
import { SmartschoolUser } from "../interfaces/SmartschoolUser";
import "./lending.css";

interface CartItem {
  book: Book;
  quantity: number;
}

export default function LendingPage() {
  const router = useRouter();

  // --- States ---
  const [userQuery, setUserQuery] = useState("");
  const [userSearchResults, setUserSearchResults] = useState<SmartschoolUser[]>(
    [],
  );
  const [selectedUser, setSelectedUser] = useState<SmartschoolUser | null>(
    null,
  );

  const [bookQuery, setBookQuery] = useState("");
  const [searchResults, setSearchResults] = useState<Book[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);

  // --- Search Smartschool User (API Call) ---
  const handleSearchSmartschoolUser = async () => {
    if (!userQuery.trim()) {
      setUserSearchResults([]);
      return;
    }

    try {
      // Call our backend proxy which communicates with the Smartschool API
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/smartschool/users?query=${encodeURIComponent(
          userQuery.trim(),
        )}`,
        {
          credentials: "include",
        },
      );

      if (!response.ok) {
        throw new Error("Failed to fetch Smartschool users");
      }

      const data = await response.json();
      setUserSearchResults(data);
    } catch (err) {
      console.error("Error searching users:", err);
      alert("Kan momenteel geen verbinding maken met de Smartschool API.");
    }
  };

  const handleSelectUser = (user: SmartschoolUser) => {
    // This replaces the currently selected user with the new one
    setSelectedUser(user);
    setUserSearchResults([]);
    setUserQuery("");
  };

  const handleRemoveUser = () => setSelectedUser(null);

  // --- Search Books ---
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

      fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/search?${params}`, {
        credentials: "include",
      })
        .then((res) => res.json())
        .then((data) => setSearchResults(data.content || []))
        .catch((err) => console.error("Error fetching books:", err));
    }, 300);

    return () => clearTimeout(timer);
  }, [bookQuery]);

  // --- Cart Handlers ---
  const handleAddToCart = (book: Book) => {
    const available =
      book.availableCopies !== undefined ? book.availableCopies : 5;

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
    setCart((prev) =>
      prev.map((item) => {
        if (item.book.id === bookId) {
          const available =
            item.book.availableCopies !== undefined
              ? item.book.availableCopies
              : 5;
          const newQuantity = item.quantity + delta;

          if (newQuantity >= 1 && newQuantity <= available) {
            return { ...item, quantity: newQuantity };
          }
        }
        return item;
      }),
    );
  };

  const handleRemoveFromCart = (bookId: number) => {
    setCart(cart.filter((item) => item.book.id !== bookId));
  };

  // --- Registration & Cancel ---
  const handleCancel = () => {
    setCart([]);
    setSelectedUser(null);
    setSearchResults([]);
    setUserSearchResults([]);
    setBookQuery("");
    setUserQuery("");
  };

  const handleRegisterLoan = async () => {
    if (cart.length === 0 || !selectedUser) return;

    // The payload sends the required user info.
    // The backend LoanService extracts ONLY the smartschoolUserId to save into the DB.
    const payload = cart.map((item) => ({
      bookId: item.book.id,
      quantity: item.quantity,
      user: {
        smartschoolUserId: selectedUser.smartschoolUserId,
        classGroup: selectedUser.classGroup,
        school: selectedUser.school || "",
        schoolId: selectedUser.schoolId || "",
      },
    }));

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/loans`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error("Database error during registration.");
      }

      alert(
        `Succes! Uitlening correct geregistreerd aan ${selectedUser.name}.`,
      );
      handleCancel();
    } catch (err) {
      console.error(err);
      alert(
        "Er ging iets mis bij het uitlenen van de boeken. Controleer de verbinding en de voorraad.",
      );
    }
  };

  return (
    <main className="pageLayout">
      <div className="pageHeader">
        <button className="backButton" onClick={() => router.back()}>
          ←
        </button>
        <h1>Uitleen registreren</h1>
      </div>

      <div className="uitleenGrid driekolomsGrid">
        {/* --- COLUMN 1: BORROWER --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader">
            <h2>Geselecteerde lener</h2>
          </div>

          <div className="userProfileCard">
            {selectedUser ? (
              <>
                {selectedUser.photoUrl ? (
                  <img
                    src={selectedUser.photoUrl}
                    alt="Profile"
                    className="userPhotoPlaceholder cover"
                  />
                ) : (
                  <div className="userPhotoPlaceholder">👤</div>
                )}
                <div className="userInfo">
                  <p className="userName">{selectedUser.name}</p>
                  <p>
                    <strong>ID:</strong> {selectedUser.smartschoolUserId}
                  </p>
                  <p>
                    <strong>Klas:</strong> {selectedUser.classGroup}
                  </p>
                </div>
                <button
                  className="actionBtn removeBtn"
                  onClick={handleRemoveUser}
                >
                  ✕
                </button>
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
              onKeyDown={(e) =>
                e.key === "Enter" && handleSearchSmartschoolUser()
              }
            />
            <button id="searchButton" onClick={handleSearchSmartschoolUser}>
              🔎︎
            </button>
          </div>

          <div className="resultsFrame">
            {userSearchResults.length === 0 ? (
              <p className="placeholderText centered">
                Typ een deel van de naam in.
              </p>
            ) : (
              userSearchResults.map((user, idx) => (
                <div key={idx} className="listItem">
                  {user.photoUrl ? (
                    <img
                      src={user.photoUrl}
                      alt="Profile"
                      className="userPhotoSmall cover"
                    />
                  ) : (
                    <div className="userPhotoSmall">👤</div>
                  )}
                  <div className="itemDetails">
                    <strong>{user.name}</strong>
                    <span>{user.classGroup}</span>
                  </div>
                  {/* Button to Select/Replace the user */}
                  <button
                    className="actionBtn addBtn"
                    onClick={() => handleSelectUser(user)}
                  >
                    {selectedUser?.smartschoolUserId === user.smartschoolUserId
                      ? "✓"
                      : "+"}
                  </button>
                </div>
              ))
            )}
          </div>
        </div>

        {/* --- COLUMN 2: SEARCH BOOKS --- */}
        <div className="gridColumn borderRight">
          <div className="sectieHeader">
            <h2>Boek zoeken</h2>
          </div>

          <div className="searchbar">
            <input
              type="text"
              placeholder="Titel, auteur, ISBN..."
              value={bookQuery}
              onChange={(e) => setBookQuery(e.target.value)}
            />
            <button id="searchButton">🔎︎</button>
          </div>

          <div className="resultsFrame">
            {searchResults.length === 0 && bookQuery.trim() !== "" ? (
              <p className="placeholderText centered">Geen boeken gevonden.</p>
            ) : searchResults.length === 0 ? (
              <p className="placeholderText centered">Typ een zoekterm.</p>
            ) : (
              searchResults.map((book) => {
                const available =
                  book.availableCopies !== undefined ? book.availableCopies : 5;
                const cartItem = cart.find((item) => item.book.id === book.id);

                return (
                  <div key={book.id} className="listItem">
                    <img
                      src={book.thumbnail || "/book-closed.png"}
                      alt="cover"
                      className="itemThumbnail"
                    />
                    <div className="itemDetails">
                      <strong>{book.title}</strong>
                      <span>{book.authors?.join(", ")}</span>
                    </div>

                    {cartItem ? (
                      <div className="addArea">
                        <span
                          className={available > 0 ? "stock-ok" : "stock-empty"}
                        >
                          {available > 0 ? `${available} vrij` : "Op"}
                        </span>
                        <div className="quantityControl syncedControl">
                          <button
                            className="qtyBtn"
                            onClick={() => updateQuantity(book.id, -1)}
                          >
                            -
                          </button>
                          <span className="qtyDisplay">
                            {cartItem.quantity}
                          </span>
                          <button
                            className="qtyBtn"
                            onClick={() => updateQuantity(book.id, 1)}
                          >
                            +
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="addArea">
                        <span
                          className={available > 0 ? "stock-ok" : "stock-empty"}
                        >
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

        {/* --- COLUMN 3: SELECTED BOOKS --- */}
        <div className="gridColumn flexBetween">
          <div className="flexColumnGrow">
            <div className="sectieHeader">
              <h2>
                Geselecteerde boeken (
                {cart.reduce((total, item) => total + item.quantity, 0)})
              </h2>
            </div>

            <div className="resultsFrame extraMargin">
              {cart.length === 0 ? (
                <p className="placeholderText centered">Nog geen boeken.</p>
              ) : (
                cart.map((item) => (
                  <div key={item.book.id} className="listItem selectedItem">
                    <img
                      src={item.book.thumbnail || "/book-closed.png"}
                      alt="cover"
                      className="itemThumbnail"
                    />

                    <div className="itemDetails">
                      <strong>{item.book.title}</strong>

                      <div className="quantityControl">
                        <button
                          className="qtyBtn"
                          onClick={() => updateQuantity(item.book.id, -1)}
                        >
                          -
                        </button>
                        <span className="qtyDisplay">{item.quantity}</span>
                        <button
                          className="qtyBtn"
                          onClick={() => updateQuantity(item.book.id, 1)}
                        >
                          +
                        </button>
                      </div>
                    </div>
                    <button
                      className="actionBtn removeBtn"
                      onClick={() => handleRemoveFromCart(item.book.id)}
                      title="Verwijder uit selectie"
                    >
                      ✕
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="actionFooter">
            <button
              className="primaryBtn"
              onClick={handleRegisterLoan}
              disabled={cart.length === 0 || !selectedUser}
            >
              Boeken uitlenen
            </button>
            <button
              className="secondaryBtn"
              onClick={handleCancel}
              disabled={
                cart.length === 0 &&
                !selectedUser &&
                bookQuery === "" &&
                userQuery === ""
              }
            >
              Uitlenen annuleren
            </button>
          </div>
        </div>
      </div>
    </main>
  );
}
