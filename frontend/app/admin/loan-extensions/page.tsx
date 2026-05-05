"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import "./loanExtensions.css";

interface LoanBookDTO {
  id: number;
  title: string;
  thumbnail: string | null;
  isbn: string;
  authors?: string[];
}

interface LoanExtensionRequestDTO {
  loanId: number;
  smartschoolUserId: string;
  borrowerDisplayName: string | null;
  borrowerRole: "STUDENT" | "TEACHER" | "BIBLIOTHEEKBEHEERDER" | "ADMIN" | string;
  quantity: number;
  loanDate: string;
  currentDueDate: string;
  proposedDueDate: string;
  requestedAt: string | null;
  book: LoanBookDTO | null;
}

export default function LoanExtensionsPage() {
  const [requests, setRequests] = useState<LoanExtensionRequestDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);
  const [workingLoanId, setWorkingLoanId] = useState<number | null>(null);

  const fetchRequests = async () => {
    try {
      setIsLoading(true);
      setMessage(null);

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
      const res = await fetch(`${apiUrl}/loans/extension-requests/pending`, {
        credentials: "include",
      });

      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(data?.message || "Kon verlengingsaanvragen niet ophalen.");
      }

      const data = await res.json();
      setRequests(data);
    } catch (err: any) {
      setMessage(err.message || "Er ging iets mis bij het ophalen.");
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Onbekend";

    const date = new Date(dateString);
    return date.toLocaleDateString("nl-BE", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });
  };

  const getRoleLabel = (role: string) => {
    switch (role) {
      case "STUDENT":
        return "Leerling";
      case "TEACHER":
        return "Leerkracht";
      default:
        return role;
    }
  };

  const decideRequest = async (loanId: number, decision: "approve" | "deny") => {
    try {
      setWorkingLoanId(loanId);
      setMessage(null);

      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
      const res = await fetch(`${apiUrl}/loans/${loanId}/extension-request/${decision}`, {
        method: "POST",
        credentials: "include",
      });

      if (!res.ok) {
        const data = await res.json().catch(() => null);
        throw new Error(data?.message || "Kon de aanvraag niet verwerken.");
      }

      setRequests((current) => current.filter((request) => request.loanId !== loanId));

      setMessage(
        decision === "approve"
          ? "De verlenging werd goedgekeurd."
          : "De verlenging werd geweigerd."
      );
    } catch (err: any) {
      setMessage(err.message || "Er ging iets mis bij het verwerken.");
    } finally {
      setWorkingLoanId(null);
    }
  };

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER"]}>
      <main className="loanExtensionsPage">
        <div className="loanExtensionsHeader">
          <div>
            <h1>Verlengingsaanvragen</h1>
            <p>
              Keur verlengingen van leerlingen en leerkrachten van je eigen school goed of af.
            </p>
          </div>

          <button className="refreshButton" onClick={fetchRequests} disabled={isLoading}>
            Vernieuwen
          </button>
        </div>

        {message && <div className="loanExtensionsMessage">{message}</div>}

        {isLoading ? (
          <div className="loanExtensionsState">Aanvragen ophalen...</div>
        ) : requests.length === 0 ? (
          <div className="loanExtensionsState">
            Er zijn momenteel geen open verlengingsaanvragen.
          </div>
        ) : (
          <div className="loanExtensionsList">
            {requests.map((request) => (
              <article key={request.loanId} className="loanExtensionCard">
                <div className="loanExtensionCover">
                  {request.book?.thumbnail ? (
                    <Image
                      src={request.book.thumbnail}
                      alt={request.book.title}
                      fill
                      className="object-contain"
                      sizes="120px"
                    />
                  ) : (
                    <span>Geen cover</span>
                  )}
                </div>

                <div className="loanExtensionInfo">
                  <div className="loanExtensionTopRow">
                    <div>
                      <h2>{request.book?.title || "Onbekend boek"}</h2>
                      <p className="loanExtensionAuthor">
                        {request.book?.authors && request.book.authors.length > 0
                          ? request.book.authors.join(", ")
                          : "Auteur onbekend"}
                      </p>
                    </div>

                    <span className="roleBadge">{getRoleLabel(request.borrowerRole)}</span>
                  </div>

                  <div className="loanExtensionMetaGrid">
                    <div>
                      <span>Gebruiker</span>
                      <strong>{request.borrowerDisplayName || getRoleLabel(request.borrowerRole)}</strong>
                    </div>
                    <div>
                      <span>Aantal</span>
                      <strong>{request.quantity}</strong>
                    </div>
                    <div>
                      <span>Geleend op</span>
                      <strong>{formatDate(request.loanDate)}</strong>
                    </div>
                    <div>
                      <span>Huidige datum</span>
                      <strong>{formatDate(request.currentDueDate)}</strong>
                    </div>
                    <div>
                      <span>Nieuwe datum</span>
                      <strong>{formatDate(request.proposedDueDate)}</strong>
                    </div>
                    <div>
                      <span>Aangevraagd op</span>
                      <strong>{formatDate(request.requestedAt)}</strong>
                    </div>
                  </div>

                  <div className="loanExtensionActions">
                    <button
                      className="approveButton"
                      disabled={workingLoanId === request.loanId}
                      onClick={() => decideRequest(request.loanId, "approve")}
                    >
                      Goedkeuren
                    </button>
                    <button
                      className="denyButton"
                      disabled={workingLoanId === request.loanId}
                      onClick={() => decideRequest(request.loanId, "deny")}
                    >
                      Weigeren
                    </button>
                  </div>
                </div>
              </article>
            ))}
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}