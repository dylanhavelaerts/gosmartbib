"use client";

import { useState, useEffect, useRef } from "react";
import { Book } from "../../../interfaces/Book";
import { SmartschoolUser } from "../../../interfaces/SmartschoolUser";
import "./returns.css";
import ProtectedRoute from "@/app/components/ProtectedRoute";

type CopyCondition = "GOOD" | "DAMAGED" | "BROKEN" | "LOST";

interface BorrowedItem {
  book: Book;
  quantityBorrowed: number;
}

interface ReturnCartItem {
  book: Book;
  quantityToReturn: number;
  maxQuantity: number;
}

interface ScannedCopy {
  barcode: string;
  condition: CopyCondition;
}

const conditionLabels: Record<CopyCondition, string> = {
  GOOD: "Geen probleem",
  DAMAGED: "Beschadigd",
  BROKEN: "Kapot",
  LOST: "Verloren",
};

export default function ReturnsPage() {
  const apiUrl = process.env.NEXT_PUBLIC_API_URL;

  // --- Auth/School ---
  const [barcodesEnabled, setBarcodesEnabled] = useState(false);

  // --- Kolom 1: Lener States ---
  const [userQuery, setUserQuery] = useState("");
  const [userSearchResults, setUserSearchResults] = useState<SmartschoolUser[]>(
    [],
  );
  const [selectedUser, setSelectedUser] = useState<SmartschoolUser | null>(
    null,
  );

  // --- Kolom 2: Uitgeleende boeken States ---
  const [borrowedBooks, setBorrowedBooks] = useState<BorrowedItem[]>([]);
  const [bookQuery, setBookQuery] = useState("");

  // --- Kolom 3: Retour Mandje States ---
  const [returnCart, setReturnCart] = useState<ReturnCartItem[]>([]);

  // --- Condition states (no-barcode mode) ---
  const [conditionSingle, setConditionSingle] = useState<
    Record<number, CopyCondition>
  >({});
  const [damagedCounts, setDamagedCounts] = useState<Record<number, number>>(
    {},
  );
  const [brokenCounts, setBrokenCounts] = useState<Record<number, number>>({});
  const [lostCounts, setLostCounts] = useState<Record<number, number>>({});

  // --- Barcode mode states ---
  const [copyConditions, setCopyConditions] = useState<
    Record<number, ScannedCopy[]>
  >({});
  const [barcodeInputs, setBarcodeInputs] = useState<Record<number, string>>(
    {},
  );
  const [scanErrors, setScanErrors] = useState<Record<number, string>>({});
  const [scanLoading, setScanLoading] = useState<Record<number, boolean>>({});
  const barcodeRefs = useRef<Record<number, HTMLInputElement | null>>({});

  // --- Fetch barcodesEnabled on mount ---
  useEffect(() => {
    async function loadSettings() {
      try {
        const meRes = await fetch(`${apiUrl}/auth/me`, {
          credentials: "include",
        });
        if (!meRes.ok) return;
        const me = await meRes.json();
        const sid = me?.school?.id;
        if (!sid) return;
        const settingsRes = await fetch(
          `${apiUrl}/admin/schools/${sid}/library-settings`,
          { credentials: "include" },
        );
        if (!settingsRes.ok) return;
        const settings = await settingsRes.json();
        if (settings?.barcodesEnabled != null)
          setBarcodesEnabled(settings.barcodesEnabled);
      } catch {
        // non-fatal
      }
    }
    loadSettings();
  }, []);

  const [toast, setToast] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  function showToast(type: "success" | "error", message: string) {
    setToast({ type, message });
    setTimeout(() => setToast(null), 4000);
  }

  // --- 1. Lener Zoeken & Selecteren ---
  const handleSearchSmartschoolUser = async () => {
    if (!userQuery.trim()) {
      setUserSearchResults([]);
      return;
    }
    try {
      const response = await fetch(
        `${apiUrl}/smartschool/users?query=${encodeURIComponent(userQuery.trim())}`,
        { credentials: "include" },
      );
      if (!response.ok) throw new Error("Failed to fetch Smartschool users");
      const data = await response.json();
      setUserSearchResults(data);
    } catch (err) {
      console.error("Error searching users:", err);
      showToast(
        "error",
        "Kan momenteel geen verbinding maken met de Smartschool API.",
      );
    }
  };

  const handleSelectUser = async (user: SmartschoolUser) => {
    setSelectedUser(user);
    setUserSearchResults([]);
    setUserQuery("");
    setReturnCart([]);
    clearConditionState();
    fetchUserLoans(user.smartschoolUserId);
  };

  const handleRemoveUser = () => {
    setSelectedUser(null);
    setBorrowedBooks([]);
    setReturnCart([]);
    clearConditionState();
  };

  const clearConditionState = () => {
    setConditionSingle({});
    setDamagedCounts({});
    setBrokenCounts({});
    setLostCounts({});
    setCopyConditions({});
    setBarcodeInputs({});
  };

  // --- 2. Ophalen van actieve leningen ---
  const fetchUserLoans = async (smartschoolUserId: string) => {
    try {
      const response = await fetch(
        `${apiUrl}/loans/active?smartschoolUserId=${smartschoolUserId}`,
        { credentials: "include" },
      );
      if (!response.ok) throw new Error("Kan leningen niet ophalen");
      const data = await response.json();

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
      showToast(
        "error",
        "Fout bij het ophalen van de uitgeleende boeken voor deze gebruiker.",
      );
    }
  };

  const filteredBorrowedBooks = borrowedBooks.filter(
    (item) =>
      item.book.title.toLowerCase().includes(bookQuery.toLowerCase()) ||
      item.book.authors?.some((a) =>
        a.toLowerCase().includes(bookQuery.toLowerCase()),
      ),
  );

  const handleBorrowedBooksBarcodeInput = async (value: string) => {
    setBookQuery(value);
    if (!/^\d{13}$/.test(value.trim())) return;
    if (!selectedUser) return;

    try {
      const res = await fetch(
        `${apiUrl}/books/by-barcode?barcode=${encodeURIComponent(value.trim())}`,
        { credentials: "include" },
      );
      if (!res.ok) {
        showToast("error", "Barcode niet gevonden.");
        return;
      }
      const book: Book = await res.json();
      const borrowed = borrowedBooks.find((b) => b.book.id === book.id);
      if (!borrowed) {
        showToast("error", "Dit boek heeft de lener niet in bezit.");
        return;
      }
      handleAddToReturnCart(borrowed);
      setBookQuery("");
    } catch {
      showToast("error", "Fout bij het opzoeken van de barcode.");
    }
  };

  // --- 3. Return Cart Handlers ---
  const handleAddToReturnCart = (borrowedItem: BorrowedItem) => {
    setReturnCart((prev) => {
      const existing = prev.find((i) => i.book.id === borrowedItem.book.id);
      if (existing) return prev;
      return [
        ...prev,
        {
          book: borrowedItem.book,
          quantityToReturn: 1,
          maxQuantity: borrowedItem.quantityBorrowed,
        },
      ];
    });
  };

  const updateReturnQuantity = (bookId: number, delta: number) => {
    setReturnCart((prev) =>
      prev.map((item) => {
        if (item.book.id !== bookId) return item;
        const newQty = item.quantityToReturn + delta;
        if (newQty < 1 || newQty > item.maxQuantity) return item;
        // Reset condition counters if their sum exceeds the new quantity
        const total =
          (damagedCounts[bookId] ?? 0) +
          (brokenCounts[bookId] ?? 0) +
          (lostCounts[bookId] ?? 0);
        if (total > newQty) {
          setDamagedCounts((d) => ({ ...d, [bookId]: 0 }));
          setBrokenCounts((b) => ({ ...b, [bookId]: 0 }));
          setLostCounts((l) => ({ ...l, [bookId]: 0 }));
        }
        return { ...item, quantityToReturn: newQty };
      }),
    );
  };

  const handleRemoveFromReturnCart = (bookId: number) => {
    setReturnCart(returnCart.filter((item) => item.book.id !== bookId));
    setConditionSingle((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
    setDamagedCounts((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
    setBrokenCounts((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
    setLostCounts((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
    setCopyConditions((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
    setBarcodeInputs((s) => {
      const c = { ...s };
      delete c[bookId];
      return c;
    });
  };

  // --- Barcode scan helpers ---
  const handleBarcodeKeyDown = async (
    e: React.KeyboardEvent<HTMLInputElement>,
    bookId: number,
  ) => {
    if (e.key !== "Enter") return;
    const val = barcodeInputs[bookId]?.trim();
    if (!val) return;

    if ((copyConditions[bookId] ?? []).find((c) => c.barcode === val)) {
      setScanErrors((prev) => ({ ...prev, [bookId]: "Barcode al gescand." }));
      return;
    }

    setScanLoading((prev) => ({ ...prev, [bookId]: true }));
    setScanErrors((prev) => ({ ...prev, [bookId]: "" }));

    try {
      const res = await fetch(
        `${apiUrl}/books/${bookId}/copies/by-barcode?barcode=${encodeURIComponent(val)}`,
        { credentials: "include" },
      );
      if (!res.ok) {
        setScanErrors((prev) => ({
          ...prev,
          [bookId]:
            res.status === 404
              ? "Barcode niet gevonden of hoort niet bij dit boek."
              : "Fout bij validatie.",
        }));
        return;
      }
      setCopyConditions((prev) => ({
        ...prev,
        [bookId]: [
          ...(prev[bookId] ?? []),
          { barcode: val, condition: "GOOD" },
        ],
      }));
      setBarcodeInputs((prev) => ({ ...prev, [bookId]: "" }));
    } catch {
      setScanErrors((prev) => ({
        ...prev,
        [bookId]: "Kan barcode niet valideren.",
      }));
    } finally {
      setScanLoading((prev) => ({ ...prev, [bookId]: false }));
    }
  };

  const updateScannedCondition = (
    bookId: number,
    barcode: string,
    condition: CopyCondition,
  ) => {
    setCopyConditions((prev) => ({
      ...prev,
      [bookId]: (prev[bookId] ?? []).map((c) =>
        c.barcode === barcode ? { ...c, condition } : c,
      ),
    }));
  };

  const removeScannedBarcode = (bookId: number, barcode: string) => {
    setCopyConditions((prev) => ({
      ...prev,
      [bookId]: (prev[bookId] ?? []).filter((c) => c.barcode !== barcode),
    }));
  };

  // --- Counter helpers for multi-copy mode ---
  const updateCount = (
    setter: React.Dispatch<React.SetStateAction<Record<number, number>>>,
    bookId: number,
    delta: number,
    max: number,
    others: number[],
  ) => {
    setter((prev) => {
      const current = prev[bookId] ?? 0;
      const newVal = current + delta;
      const otherSum = others.reduce((a, b) => a + b, 0);
      if (newVal < 0 || otherSum + newVal > max) return prev;
      return { ...prev, [bookId]: newVal };
    });
  };

  // --- 4. Inleveren ---
  const handleCancel = () => {
    setReturnCart([]);
    setSelectedUser(null);
    setBorrowedBooks([]);
    setUserSearchResults([]);
    setBookQuery("");
    setUserQuery("");
    clearConditionState();
  };

  const handleRegisterReturn = async () => {
    if (returnCart.length === 0 || !selectedUser) return;

    const payload = returnCart.map((item) => {
      const bookId = item.book.id;
      if (barcodesEnabled) {
        const scanned = copyConditions[bookId] ?? [];
        return {
          bookId,
          quantity: item.quantityToReturn,
          smartschoolUserId: selectedUser.smartschoolUserId,
          copyConditions: scanned.map((c) => ({
            copyId: null,
            barcode: c.barcode,
            condition: c.condition,
            notes: null,
          })),
          damagedCount: 0,
          brokenCount: 0,
          lostCount: 0,
        };
      } else if (item.quantityToReturn === 1) {
        const cond = conditionSingle[bookId] ?? "GOOD";
        return {
          bookId,
          quantity: item.quantityToReturn,
          smartschoolUserId: selectedUser.smartschoolUserId,
          copyConditions: [],
          damagedCount: cond === "DAMAGED" ? 1 : 0,
          brokenCount: cond === "BROKEN" ? 1 : 0,
          lostCount: cond === "LOST" ? 1 : 0,
        };
      } else {
        return {
          bookId,
          quantity: item.quantityToReturn,
          smartschoolUserId: selectedUser.smartschoolUserId,
          copyConditions: [],
          damagedCount: damagedCounts[bookId] ?? 0,
          brokenCount: brokenCounts[bookId] ?? 0,
          lostCount: lostCounts[bookId] ?? 0,
        };
      }
    });

    try {
      const response = await fetch(`${apiUrl}/loans/return`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });
      if (!response.ok)
        throw new Error("Fout bij het registreren van de retour.");

      showToast(
        "success",
        `Succes! De boeken van ${selectedUser.name} zijn succesvol ingeleverd.`,
      );
      setReturnCart([]);
      clearConditionState();
      fetchUserLoans(selectedUser.smartschoolUserId);
      setBookQuery("");
    } catch (err) {
      console.error(err);
      showToast(
        "error",
        "Er ging iets mis bij het inleveren van de boeken. Controleer de verbinding.",
      );
    }
  };

  return (
    <ProtectedRoute allowedRoles={["LIBRARIAN"]}>
      <main className="returnsPageLayout">
        <div className="pageHeader">
          <h1>Boeken Inleveren</h1>
        </div>

        <div className="uitleenGrid driekolomsGrid">
          {/* --- KOLOM 1: LENER SELECTEREN --- */}
          <div className="gridColumn borderRight">
            <div className="sectieHeader">
              <h2>Lener (retour)</h2>
            </div>

            <div className="userProfileCard">
              {selectedUser ? (
                <>
                  {/* {selectedUser.photoUrl ? (
                  <img src={selectedUser.photoUrl} alt="Profile" className="userPhotoPlaceholder cover" />
                ) : (
                  <div className="userPhotoPlaceholder">👤</div>
                )}
                */}
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
                <p className="placeholderText">Nog geen lener geselecteerd</p>
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
                  Typ een deel van de naam in
                </p>
              ) : (
                userSearchResults.map((user, idx) => (
                  <div key={idx} className="listItem">
                    {/*
                  {user.photoUrl ? (
                    <img src={user.photoUrl} alt="Profile" className="userPhotoSmall cover" />
                  ) : (
                    <div className="userPhotoSmall">👤</div>
                  )}
                  */}
                    <div className="itemDetails">
                      <strong>{user.name}</strong>
                      <span>{user.classGroup}</span>
                    </div>
                    <button
                      className="actionBtn addBtn"
                      onClick={() => handleSelectUser(user)}
                    >
                      {selectedUser?.smartschoolUserId ===
                      user.smartschoolUserId
                        ? "✓"
                        : "+"}
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* --- KOLOM 2: UITGELEENDE BOEKEN LIJST --- */}
          <div className="gridColumn borderRight">
            <div className="sectieHeader">
              <h2>Uitgeleende boeken</h2>
            </div>

            <div className="searchbar">
              <input
                type="text"
                placeholder="Filter of scan barcode..."
                value={bookQuery}
                onChange={(e) =>
                  handleBorrowedBooksBarcodeInput(e.target.value)
                }
                disabled={!selectedUser}
              />
            </div>

            <div className="resultsFrame">
              {!selectedUser ? (
                <p className="placeholderText centered">
                  Selecteer eerst een lener
                </p>
              ) : borrowedBooks.length === 0 ? (
                <p className="placeholderText centered">
                  Deze persoon heeft geen boeken in bezit
                </p>
              ) : filteredBorrowedBooks.length === 0 ? (
                <p className="placeholderText centered">
                  Geen boeken gevonden met deze filter
                </p>
              ) : (
                filteredBorrowedBooks.map((item) => {
                  const isSelectedForReturn = returnCart.find(
                    (c) => c.book.id === item.book.id,
                  );

                  return (
                    <div key={item.book.id} className="listItem">
                      <img
                        src={item.book.thumbnail || "/book-closed.png"}
                        alt="cover"
                        className="itemThumbnail"
                      />
                      <div className="itemDetails">
                        <strong>{item.book.title}</strong>
                        <span>{item.book.authors?.join(", ")}</span>
                      </div>

                      {isSelectedForReturn ? (
                        <div className="addArea">
                          <span className="stock-ok">
                            {item.quantityBorrowed} in bezit
                          </span>
                          <div className="quantityControl syncedControl">
                            <button
                              className="qtyBtn"
                              onClick={() =>
                                updateReturnQuantity(item.book.id, -1)
                              }
                            >
                              -
                            </button>
                            <span className="qtyDisplay">
                              {isSelectedForReturn.quantityToReturn}
                            </span>
                            <button
                              className="qtyBtn"
                              onClick={() =>
                                updateReturnQuantity(item.book.id, 1)
                              }
                            >
                              +
                            </button>
                          </div>
                        </div>
                      ) : (
                        <div className="addArea">
                          <span className="stock-ok">
                            {item.quantityBorrowed} in bezit
                          </span>
                          <button
                            className="actionBtn addBtn"
                            onClick={() => handleAddToReturnCart(item)}
                            title="Voeg toe aan retour"
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

          {/* --- KOLOM 3: RETOUR MANDJE --- */}
          <div className="gridColumn flexBetween">
            <div className="flexColumnGrow">
              <div className="sectieHeader">
                <h2>
                  Terugbrengen (
                  {returnCart.reduce(
                    (total, item) => total + item.quantityToReturn,
                    0,
                  )}
                  )
                </h2>
              </div>

              <div className="resultsFrame extraMargin">
                {returnCart.length === 0 ? (
                  <p className="placeholderText centered">
                    Geen boeken geselecteerd voor inlevering
                  </p>
                ) : (
                  returnCart.map((item) => {
                    const bookId = item.book.id;
                    const qty = item.quantityToReturn;
                    const damaged = damagedCounts[bookId] ?? 0;
                    const broken = brokenCounts[bookId] ?? 0;
                    const lost = lostCounts[bookId] ?? 0;
                    const scanned = copyConditions[bookId] ?? [];

                    return (
                      <div key={bookId} className="cartItemBlock">
                        {/* Book row */}
                        <div className="listItem selectedItem">
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
                                onClick={() => updateReturnQuantity(bookId, -1)}
                              >
                                -
                              </button>
                              <span className="qtyDisplay">{qty}</span>
                              <button
                                className="qtyBtn"
                                onClick={() => updateReturnQuantity(bookId, 1)}
                              >
                                +
                              </button>
                            </div>
                          </div>
                          <button
                            className="actionBtn removeBtn"
                            onClick={() => handleRemoveFromReturnCart(bookId)}
                            title="Verwijder uit selectie"
                          >
                            ✕
                          </button>
                        </div>

                        {/* Condition controls */}
                        {barcodesEnabled ? (
                          <div className="conditionSection">
                            <div className="barcodeInputRow">
                              <input
                                type="text"
                                className={`barcodeInput${scanLoading[bookId] ? " barcodeInput--loading" : ""}`}
                                placeholder={
                                  scanLoading[bookId]
                                    ? "Valideren…"
                                    : "Scan barcode..."
                                }
                                value={barcodeInputs[bookId] ?? ""}
                                disabled={!!scanLoading[bookId]}
                                ref={(el) => {
                                  barcodeRefs.current[bookId] = el;
                                }}
                                onChange={(e) => {
                                  setBarcodeInputs((prev) => ({
                                    ...prev,
                                    [bookId]: e.target.value,
                                  }));
                                  setScanErrors((prev) => ({
                                    ...prev,
                                    [bookId]: "",
                                  }));
                                }}
                                onKeyDown={(e) =>
                                  handleBarcodeKeyDown(e, bookId)
                                }
                              />
                              {scanErrors[bookId] && (
                                <p className="scanError">
                                  {scanErrors[bookId]}
                                </p>
                              )}
                            </div>
                            {scanned.length > 0 && (
                              <div className="scannedPills">
                                {scanned.map((sc) => (
                                  <div
                                    key={sc.barcode}
                                    className="scannedPill"
                                    data-condition={sc.condition}
                                  >
                                    <span className="pillBarcode">
                                      {sc.barcode}
                                    </span>
                                    <select
                                      className="pillConditionSelect"
                                      value={sc.condition}
                                      onChange={(e) =>
                                        updateScannedCondition(
                                          bookId,
                                          sc.barcode,
                                          e.target.value as CopyCondition,
                                        )
                                      }
                                    >
                                      {(
                                        Object.keys(
                                          conditionLabels,
                                        ) as CopyCondition[]
                                      ).map((k) => (
                                        <option key={k} value={k}>
                                          {conditionLabels[k]}
                                        </option>
                                      ))}
                                    </select>
                                    <button
                                      className="pillRemove"
                                      onClick={() =>
                                        removeScannedBarcode(bookId, sc.barcode)
                                      }
                                    >
                                      ✕
                                    </button>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>
                        ) : qty === 1 ? (
                          <div className="conditionSection">
                            <select
                              className="conditionSelect"
                              value={conditionSingle[bookId] ?? "GOOD"}
                              onChange={(e) =>
                                setConditionSingle((prev) => ({
                                  ...prev,
                                  [bookId]: e.target.value as CopyCondition,
                                }))
                              }
                            >
                              {(
                                Object.keys(conditionLabels) as CopyCondition[]
                              ).map((k) => (
                                <option key={k} value={k}>
                                  {conditionLabels[k]}
                                </option>
                              ))}
                            </select>
                          </div>
                        ) : (
                          <div className="conditionSection conditionCounters">
                            {(
                              [
                                {
                                  label: "Beschadigd",
                                  count: damaged,
                                  setter: setDamagedCounts,
                                  others: [broken, lost],
                                },
                                {
                                  label: "Kapot",
                                  count: broken,
                                  setter: setBrokenCounts,
                                  others: [damaged, lost],
                                },
                                {
                                  label: "Verloren",
                                  count: lost,
                                  setter: setLostCounts,
                                  others: [damaged, broken],
                                },
                              ] as const
                            ).map(({ label, count, setter, others }) => (
                              <div key={label} className="conditionCounter">
                                <span className="conditionCounterLabel">
                                  {label}
                                </span>
                                <div className="conditionCounterControl">
                                  <button
                                    className="qtyBtn"
                                    onClick={() =>
                                      updateCount(
                                        setter as React.Dispatch<
                                          React.SetStateAction<
                                            Record<number, number>
                                          >
                                        >,
                                        bookId,
                                        -1,
                                        qty,
                                        [...others],
                                      )
                                    }
                                  >
                                    −
                                  </button>
                                  <span className="qtyDisplay">{count}</span>
                                  <button
                                    className="qtyBtn"
                                    onClick={() =>
                                      updateCount(
                                        setter as React.Dispatch<
                                          React.SetStateAction<
                                            Record<number, number>
                                          >
                                        >,
                                        bookId,
                                        1,
                                        qty,
                                        [...others],
                                      )
                                    }
                                  >
                                    +
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })
                )}
              </div>
            </div>

            <div className="actionFooter">
              <button
                className="primaryBtn"
                onClick={handleRegisterReturn}
                disabled={returnCart.length === 0 || !selectedUser}
              >
                Geselecteerde boeken inleveren
              </button>
              <button
                className="secondaryBtn"
                onClick={handleCancel}
                disabled={
                  returnCart.length === 0 &&
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
      {toast && (
        <div
          className={`toastNotification ${toast.type === "success" ? "toastSuccess" : "toastError"}`}
        >
          <span className="toastMessage">{toast.message}</span>
          <button
            className="toastClose"
            onClick={() => setToast(null)}
            aria-label="Sluiten"
          >
            ✕
          </button>
        </div>
      )}
    </ProtectedRoute>
  );
}
