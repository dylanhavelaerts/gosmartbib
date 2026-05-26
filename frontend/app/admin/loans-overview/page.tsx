"use client";

import { useEffect, useState } from "react";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import "./loans-overview.css";
import Link from "next/dist/client/link";
import Pagination from "@/app/catalog/pagination";

interface ClassOption {
  id: number;
  name: string;
  grade?: string | null;
}

interface AdminActiveLoan {
  loanId: number;
  smartschoolUserId: string;
  borrowerDisplayName: string;
  borrowerClassNames: string[];
  quantity: number;
  loanDate: string;
  dueDate: string;
  extensionStatus: string;
  book: {
    id: number;
    title: string;
    thumbnail: string | null;
    isbn: string;
    authors: string[];
  } | null;
}

interface AdminLoanHistory {
  id: number;
  bookTitle: string;
  author: string;
  loanDate: string;
  returnDate: string;
  quantity: number;
  borrowerDisplayName: string;
  borrowerClassNames: string[];
  damagedCount: number;
  brokenCount: number;
  lostCount: number;
}

export default function LoansOverviewPage() {
  const [activeTab, setActiveTab] = useState<"active" | "history">("active");
  const [classes, setClasses] = useState<ClassOption[]>([]);
  const [selectedClassId, setSelectedClassId] = useState<number | null>(null);
  const [activeLoans, setActiveLoans] = useState<AdminActiveLoan[]>([]);
  const [loanHistory, setLoanHistory] = useState<AdminLoanHistory[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activePage, setActivePage] = useState(1);
  const [historyPage, setHistoryPage] = useState(1);
  const [activeTotalPages, setActiveTotalPages] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const PAGE_SIZE = 20;
  const [activeTotalElements, setActiveTotalElements] = useState(0);
  const [historyTotalElements, setHistoryTotalElements] = useState(0);
  const [overdueTotal, setOverdueTotal] = useState(0);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState<string | null>(null);

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
    fetch(`${apiUrl}/loans/school/classes`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((data) => setClasses(data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const fetchActive = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
        const classParam =
          selectedClassId != null ? `&classId=${selectedClassId}` : "";
        const res = await fetch(
          `${apiUrl}/loans/school/active?page=${activePage - 1}&size=${PAGE_SIZE}${classParam}`,
          { credentials: "include" },
        );
        if (!res.ok) throw new Error("Kon de gegevens niet ophalen.");
        const data = await res.json();
        setActiveLoans(data.content);
        setActiveTotalPages(data.totalPages);
        setActiveTotalElements(data.totalElements);
      } catch (err: any) {
        setError(err.message || "Er is een onbekende fout opgetreden.");
      } finally {
        setIsLoading(false);
      }
    };

    fetchActive();
  }, [selectedClassId, activePage]);

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
    fetch(`${apiUrl}/statistics/overview`, { credentials: "include" })
      .then((r) => r.json())
      .then((data) => setOverdueTotal(data.overdueLoans))
      .catch(() => {});
  }, []);

  useEffect(() => {
    const fetchHistory = async () => {
      setIsHistoryLoading(true);
      setHistoryError(null);
      try {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
        const classParam =
          selectedClassId != null ? `&classId=${selectedClassId}` : "";
        const res = await fetch(
          `${apiUrl}/loans/school/history?page=${historyPage - 1}&size=${PAGE_SIZE}${classParam}`,
          { credentials: "include" },
        );
        if (!res.ok) throw new Error("Kon de geschiedenis niet ophalen.");
        const data = await res.json();
        setLoanHistory(data.content);
        setHistoryTotalPages(data.totalPages);
        setHistoryTotalElements(data.totalElements);
      } catch (err: any) {
        setHistoryError(err.message || "Er is een onbekende fout opgetreden.");
      } finally {
        setIsHistoryLoading(false);
      }
    };
    fetchHistory();
  }, [selectedClassId, historyPage]);

  const formatDate = (dateString: string) => {
    if (!dateString) return "Onbekend";
    return new Date(dateString).toLocaleDateString("nl-BE", {
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
    const diffDays = Math.ceil(
      (due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24),
    );
    if (diffDays < 0)
      return {
        text: `${Math.abs(diffDays)} dagen te laat`,
        cls: "status-late",
      };
    if (diffDays <= 3)
      return { text: `Nog ${diffDays} dagen`, cls: "status-soon" };
    return { text: `Nog ${diffDays} dagen`, cls: "status-ok" };
  };

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER"]}>
      <main className="loansOverviewPage pageLayout">
        <div className="pageHeader">
          <div className="pageHeaderText">
            <h1>Uitleenoverzicht</h1>
            <p className="pageSubtitle">
              Overzicht van alle leningen binnen jouw school
            </p>
          </div>
          {!isLoading && !error && (
            <div className="statsRow">
              <span className="statChip">
                <strong>{activeTotalElements}</strong> actieve leningen
              </span>

              {overdueTotal > 0 && (
                <span className="statChip statChip--late">
                  <strong>{overdueTotal}</strong> te laat
                </span>
              )}
            </div>
          )}
        </div>

        <div className="filterBar">
          <label htmlFor="classFilter" className="filterLabel">
            Filter op klas:
          </label>
          <select
            id="classFilter"
            className="classSelect"
            value={selectedClassId ?? ""}
            onChange={(e) => {
              setSelectedClassId(
                e.target.value === "" ? null : Number(e.target.value),
              );
              setActivePage(1);
              setHistoryPage(1);
            }}
          >
            <option value="">Alle klassen</option>
            {classes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
                {c.grade ? ` — ${c.grade}` : ""}
              </option>
            ))}
          </select>
        </div>

        <div className="tabBar">
          <button
            className={`tab ${activeTab === "active" ? "tab--active" : ""}`}
            onClick={() => setActiveTab("active")}
          >
            Actieve leningen
            {!isLoading && (
              <span className="tabBadge">{activeTotalElements}</span>
            )}
          </button>
          <button
            className={`tab ${activeTab === "history" ? "tab--active" : ""}`}
            onClick={() => setActiveTab("history")}
          >
            Ontleengeschiedenis
            {!isLoading && (
              <span className="tabBadge">{historyTotalElements}</span>
            )}
          </button>
        </div>

        {isLoading ? (
          <div className="loadingState">Gegevens ophalen...</div>
        ) : error ? (
          <div className="errorState">
            <h3>Fout bij ophalen</h3>
            <p>{error}</p>
            <button onClick={() => window.location.reload()}>
              Probeer opnieuw
            </button>
          </div>
        ) : activeTab === "active" ? (
          activeLoans.length === 0 ? (
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
                {selectedClassId
                  ? "Deze klas heeft momenteel geen actieve leningen."
                  : "Er zijn momenteel geen actieve leningen in de school."}
              </p>
            </div>
          ) : (
            <div className="historyTableWrapper">
              <table className="historyTable">
                <thead>
                  <tr>
                    <th>Boek</th>
                    <th>Leerling</th>
                    <th>Klas</th>
                    <th>Geleend op</th>
                    <th>Inleveren voor</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {activeLoans.map((loan) => {
                    const status = getDueDateStatus(loan.dueDate);
                    return (
                      <tr key={loan.loanId}>
                        <td>
                          <div className="bookTitleCell">
                            {loan.book?.title || "Onbekend boek"}
                          </div>
                          <div className="bookAuthorCell">
                            {loan.book?.authors && loan.book.authors.length > 0
                              ? loan.book.authors.join(", ")
                              : "Auteur onbekend"}
                          </div>
                        </td>
                        <td>{loan.borrowerDisplayName}</td>
                        <td>
                          <div className="classChips">
                            {loan.borrowerClassNames.length > 0 ? (
                              loan.borrowerClassNames.map((cls) => (
                                <span key={cls} className="classChip">
                                  {cls}
                                </span>
                              ))
                            ) : (
                              <span className="classChip classChip--unknown">
                                —
                              </span>
                            )}
                          </div>
                        </td>
                        <td>{formatDate(loan.loanDate)}</td>
                        <td>
                          <span className={`dueDateBadge ${status.cls}`}>
                            {formatDate(loan.dueDate)}
                          </span>
                          <div className="dueDateSub">{status.text}</div>
                        </td>
                        <td>
                          <Link
                            href="/admin/loan-return/return"
                            className="returnButtonSmall"
                          >
                            Retourneer
                          </Link>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              <Pagination
                currentPage={activePage}
                totalPages={activeTotalPages}
                onPageChange={setActivePage}
              />
            </div>
          )
        ) : isHistoryLoading ? (
          <div className="loadingState">Gegevens ophalen...</div>
        ) : historyError ? (
          <div className="errorState">
            <h3>Fout bij ophalen</h3>
            <p>{historyError}</p>
            <button onClick={() => setHistoryPage((p) => p)}>
              Probeer opnieuw
            </button>
          </div>
        ) : loanHistory.length === 0 ? (
          <div className="emptyState">
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
              />
            </svg>
            <p className="emptyTitle">Geen geschiedenis</p>
            <p className="emptyText">
              {selectedClassId
                ? "Geen teruggebrachte boeken gevonden voor deze klas."
                : "Er zijn nog geen boeken teruggebracht."}
            </p>
          </div>
        ) : (
          <div className="historyTableWrapper">
            <table className="historyTable">
              <thead>
                <tr>
                  <th>Boek</th>
                  <th>Leerling</th>
                  <th>Klas</th>
                  <th>Geleend op</th>
                  <th>Ingeleverd op</th>
                  <th style={{ textAlign: "center" }}>Aantal</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {loanHistory.map((h) => (
                  <tr key={h.id}>
                    <td>
                      <div className="bookTitleCell">{h.bookTitle}</div>
                      <div className="bookAuthorCell">{h.author}</div>
                    </td>
                    <td>{h.borrowerDisplayName}</td>
                    <td>
                      <div className="classChips">
                        {h.borrowerClassNames.length > 0 ? (
                          h.borrowerClassNames.map((cls) => (
                            <span key={cls} className="classChip">
                              {cls}
                            </span>
                          ))
                        ) : (
                          <span className="classChip classChip--unknown">
                            —
                          </span>
                        )}
                      </div>
                    </td>
                    <td>{formatDate(h.loanDate)}</td>
                    <td>
                      <span className="statusReturned">
                        {formatDate(h.returnDate)}
                      </span>
                    </td>
                    <td style={{ textAlign: "center" }}>
                      <span className="qtyBadge">{h.quantity}</span>
                    </td>
                    <td>
                      <div className="conditionBadges">
                        {h.damagedCount > 0 && (
                          <span className="conditionBadge conditionBadge--damaged">
                            {h.damagedCount}× beschadigd
                          </span>
                        )}
                        {h.brokenCount > 0 && (
                          <span className="conditionBadge conditionBadge--broken">
                            {h.brokenCount}× kapot
                          </span>
                        )}
                        {h.lostCount > 0 && (
                          <span className="conditionBadge conditionBadge--lost">
                            {h.lostCount}× verloren
                          </span>
                        )}
                        {!h.damagedCount && !h.brokenCount && !h.lostCount && (
                          <span className="conditionBadge conditionBadge--ok">OK</span>
                        )}
                      </div>
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
      </main>
    </ProtectedRoute>
  );
}
