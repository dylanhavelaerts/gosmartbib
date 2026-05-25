"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import "./lended-books.css";
import { useAuth } from "../context/AuthContext";
import Pagination from "../catalog/pagination";

// --- Types ---
type ExtensionStatus = "NONE" | "PENDING" | "APPROVED" | "DENIED";

interface LoanBookDTO {
  id: number;
  title: string;
  thumbnail: string | null;
  isbn: string;
  authors?: string[];
}

interface ActiveLoan {
  loanId: number;
  smartschoolUserId: string;
  quantity: number;
  loanDate: string;
  dueDate: string;
  extensionStatus: ExtensionStatus;
  book: LoanBookDTO | null;
}

interface LoanHistory {
  id: number;
  bookTitle: string;
  author: string;
  loanDate: string;
  returnDate: string;
  quantity: number;
}

export default function MijnBoekenPage() {
  const { user } = useAuth();

  const [activeLoans, setActiveLoans] = useState<ActiveLoan[]>([]);
  const [loanHistory, setLoanHistory] = useState<LoanHistory[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [requestingLoanId, setRequestingLoanId] = useState<number | null>(null);
  const [historyPage, setHistoryPage] = useState(1);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<string | null>(null);

  const canRequestExtension =
    user?.role === "STUDENT" || user?.role === "TEACHER";
  useEffect(() => {
    const fetchActiveLoans = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
        const res = await fetch(`${apiUrl}/loans/active`, {
          credentials: "include",
        });
        if (!res.ok)
          throw new Error("Kon je leningen niet ophalen. Ben je wel ingelogd?");
        setActiveLoans(await res.json());
      } catch (err: any) {
        setError(err.message || "Er is een onbekende fout opgetreden");
      } finally {
        setIsLoading(false);
      }
    };
    fetchActiveLoans();
  }, []);

  useEffect(() => {
    const fetchHistory = async () => {
      setIsHistoryLoading(true);
      setHistoryError(null);
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
        const res = await fetch(
          `${apiUrl}/loans/history?page=${historyPage - 1}&size=10`,
          { credentials: "include" },
        );
        if (!res.ok) throw new Error("Kon je historiek niet ophalen.");
        const data = await res.json();
        setLoanHistory(data.content);
        setHistoryTotalPages(data.totalPages);
      } catch (err: any) {
        setHistoryError(err.message || "Er is een onbekende fout opgetreden");
      } finally {
        setIsHistoryLoading(false);
      }
    };
    fetchHistory();
  }, [historyPage]);

  // --- Helpers ---
  const formatDate = (dateString: string) => {
    if (!dateString) return "Onbekend";
    const date = new Date(dateString);
    return date.toLocaleDateString("nl-BE", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getDueDateStatus = (dueDateString: string) => {
    const due = new Date(dueDateString);
    const now = new Date();
    due.setHours(0, 0, 0, 0);
    now.setHours(0, 0, 0, 0);

    const diffTime = due.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return {
        text: `${Math.abs(diffDays)} dagen te laat`,
        statusClass: "status-late",
      };
    } else if (diffDays <= 3) {
      return { text: `Nog ${diffDays} dagen`, statusClass: "status-soon" };
    } else {
      return { text: `Nog ${diffDays} dagen`, statusClass: "status-ok" };
    }
  };

  const getExtensionText = (status: ExtensionStatus) => {
    switch (status) {
      case "PENDING":
        return "Verlenging aangevraagd";
      case "APPROVED":
        return "Verlenging goedgekeurd";
      case "DENIED":
        return "Verlenging geweigerd";
      default:
        return null;
    }
  };

  const requestExtension = async (loanId: number) => {
    try {
      setActionMessage(null);
      setRequestingLoanId(loanId);

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
      const res = await fetch(`${apiUrl}/loans/${loanId}/extension-request`, {
        method: "POST",
        credentials: "include",
      });

      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(
          data?.message || "Kon de verlengingsaanvraag niet versturen",
        );
      }

      setActiveLoans((current) =>
        current.map((loan) =>
          loan.loanId === loanId
            ? { ...loan, extensionStatus: "PENDING" }
            : loan,
        ),
      );

      setActionMessage("Je verlengingsaanvraag werd verstuurd");
    } catch (err: any) {
      setActionMessage(err.message || "Er ging iets mis bij het aanvragen");
    } finally {
      setRequestingLoanId(null);
    }
  };

  // --- UI Components ---
  if (isLoading) {
    return (
      <main className="lendedBooksPage pageLayout">
        <div className="pageHeader">
          <div>
            <h1>Mijn Bibliotheek</h1>
            <p className="pageSubtitle">Gegevens ophalen...</p>
          </div>
        </div>
      </main>
    );
  }

  if (error) {
    return (
      <main className="lendedBooksPage pageLayout">
        <div className="errorState">
          <h3>Fout bij ophalen</h3>
          <p>{error}</p>
          <button onClick={() => window.location.reload()}>
            Probeer opnieuw
          </button>
        </div>
      </main>
    );
  }

  // Bereken het totaal aantal boeken (som van alle aantallen)
  const totalActiveBooks = activeLoans.reduce(
    (sum, loan) => sum + loan.quantity,
    0,
  );

  return (
    <main className="lendedBooksPage pageLayout">
      <div className="pageHeader">
        <div>
          <h1>Mijn Bibliotheek</h1>
          <p className="pageSubtitle">
            Beheer je ontleningen en bekijk je leesgeschiedenis op één plek
          </p>
        </div>
      </div>

      {actionMessage && (
        <div className="loanActionMessage">{actionMessage}</div>
      )}

      <div className="contentGrid">
        {/* --- ACTIEVE UITLENINGEN --- */}
        <section>
          <div className="sectieHeader">
            <h2>
              Momenteel in bezit
              <span className="badge">{totalActiveBooks}</span>
            </h2>
          </div>

          {activeLoans.length === 0 ? (
            <div className="emptyState">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M5 19a2 2 0 01-2-2V7a2 2 0 012-2h4l2 2h4a2 2 0 012 2v1M5 19h14a2 2 0 002-2v-5a2 2 0 00-2-2H9a2 2 0 00-2 2v5a2 2 0 01-2 2z"
                />
              </svg>
              <p className="emptyTitle">Geen actieve leningen</p>
              <p className="emptyText">
                Je hebt op dit moment geen boeken ontleend uit de bibliotheek
              </p>
            </div>
          ) : (
            <div className="resultsFrame">
              {activeLoans.map((loan) => {
                const status = getDueDateStatus(loan.dueDate);
                const extensionText = getExtensionText(loan.extensionStatus);
                const showRequestButton =
                  canRequestExtension && loan.extensionStatus === "NONE";

                return (
                  <div key={loan.loanId} className="bookCard">
                    <div className="bookCardCover">
                      <span className={`statusBadge ${status.statusClass}`}>
                        {status.text}
                      </span>
                      {loan.book?.thumbnail ? (
                        <Image
                          src={loan.book.thumbnail}
                          alt={loan.book.title}
                          fill
                          className="object-contain"
                          sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 25vw"
                        />
                      ) : (
                        <div className="noCover">
                          <svg
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                          >
                            <path
                              strokeLinecap="round"
                              strokeLinejoin="round"
                              strokeWidth={1}
                              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
                            />
                          </svg>
                          <span>Geen cover</span>
                        </div>
                      )}
                    </div>

                    <div className="bookCardDetails">
                      <h3
                        className="bookCardTitle"
                        title={loan.book?.title || "Onbekend"}
                      >
                        {loan.book?.title || "Onbekend boek"}
                      </h3>

                      <p className="bookCardAuthor">
                        {loan.book?.authors && loan.book.authors.length > 0
                          ? loan.book.authors.join(", ")
                          : "Auteur onbekend"}
                      </p>

                      <div className="bookCardDates">
                        <div className="dateRow">
                          <span>Geleend:</span>
                          <strong>{formatDate(loan.loanDate)}</strong>
                        </div>
                        <div className="dateRow">
                          <span>Inleveren:</span>
                          <strong>{formatDate(loan.dueDate)}</strong>
                        </div>
                        {loan.quantity > 1 && (
                          <div
                            className="dateRow"
                            style={{
                              borderTop: "1px solid #ece6f0",
                              paddingTop: "0.25rem",
                              marginTop: "0.25rem",
                            }}
                          >
                            <span>Aantal:</span>
                            <strong style={{ color: "#8e2446" }}>
                              {loan.quantity}
                            </strong>
                          </div>
                        )}

                        {extensionText && (
                          <div
                            className={`extensionStatus extension-${loan.extensionStatus.toLowerCase()}`}
                          >
                            {extensionText}
                          </div>
                        )}

                        {showRequestButton && (
                          <button
                            className="extensionButton"
                            disabled={requestingLoanId === loan.loanId}
                            onClick={() => requestExtension(loan.loanId)}
                          >
                            {requestingLoanId === loan.loanId
                              ? "Aanvragen..."
                              : "Verlenging aanvragen"}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </section>

        {/* --- ONTLEENGESCHIEDENIS --- */}
        <section>
          <div className="sectieHeader">
            <h2>Ontleengeschiedenis</h2>
          </div>

          {isHistoryLoading ? (
            <p>Laden...</p>
          ) : historyError ? (
            <p>{historyError}</p>
          ) : loanHistory.length === 0 ? (
            <div className="emptyState">
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                />
              </svg>
              <p className="emptyTitle">Geen geschiedenis</p>
              <p className="emptyText">
                Je hebt nog geen boeken afgerond en ingeleverd
              </p>
            </div>
          ) : (
            <div className="historyTableWrapper">
              <table className="historyTable">
                <thead>
                  <tr>
                    <th>Boek</th>
                    <th>Geleend op</th>
                    <th>Ingeleverd op</th>
                    <th style={{ textAlign: "center" }}>Aantal</th>
                  </tr>
                </thead>
                <tbody>
                  {loanHistory.map((history) => (
                    <tr key={history.id}>
                      <td>
                        <div className="bookTitleCell">{history.bookTitle}</div>
                        <div className="bookAuthorCell">{history.author}</div>
                      </td>
                      <td>{formatDate(history.loanDate)}</td>
                      <td>
                        <span className="statusReturned">
                          {formatDate(history.returnDate)}
                        </span>
                      </td>
                      <td style={{ textAlign: "center" }}>
                        <span className="qtyBadge">{history.quantity}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pagination
                currentPage={historyPage}
                totalPages={historyTotalPages}
                onPageChange={setHistoryPage}
              />
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
