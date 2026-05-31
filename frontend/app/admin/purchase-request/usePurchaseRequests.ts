"use client";

import { useCallback, useEffect, useState } from "react";

export type PurchaseStatus = "PENDING" | "APPROVED" | "REJECTED";

export interface PurchaseRequest {
  id: number;
  title: string;
  authors: string[];
  isbn: string | null;
  status: PurchaseStatus;
  userId: number;
  userSmartschoolUid: string;
  requestDate: string;
  note: string | null;
}

export interface CreatePurchaseRequestData {
  title: string;
  authors: string[];
  isbn: string;
}

export function usePurchaseRequests(apiUrl?: string) {
  const [requests, setRequests] = useState<PurchaseRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [approvingId, setApprovingId] = useState<number | null>(null);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchRequests = useCallback(async () => {
    if (!apiUrl) throw new Error("API URL ontbreekt");

    const response = await fetch(`${apiUrl}/purchase-requests`, {
      credentials: "include",
    });

    if (!response.ok) throw new Error("Kon aankoopverzoeken niet ophalen");

    const data: PurchaseRequest[] = await response.json();
    setRequests(data);
  }, [apiUrl]);

  useEffect(() => {
    async function load() {
      if (!apiUrl) {
        setError("API URL ontbreekt");
        setLoading(false);
        return;
      }
      try {
        setLoading(true);
        setError("");
        await fetchRequests();
      } catch {
        setError("Er ging iets mis bij het laden van de aankoopverzoeken.");
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [apiUrl, fetchRequests]);

  /**
   * Maakt een nieuw aankoopverzoek aan.
   * Doet dit via een POST request naar het /purchase-requests endpoint van de API, met de titel, auteurs en ISBN van het boek in de body
   * @param data de gegevens voor het aankoopverzoek
   * @returns {Promise<boolean>} true als het aanmaken succesvol was, anders false
   */
  async function createRequest(
    data: CreatePurchaseRequestData,
  ): Promise<boolean> {
    if (!apiUrl || isSubmitting) return false;

    try {
      setIsSubmitting(true);
      setActionError("");

      const response = await fetch(`${apiUrl}/purchase-requests`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: data.title,
          authors: data.authors.filter((a) => a.trim()),
          isbn: data.isbn.trim() || null,
        }),
      });
      if (!response.ok) {
        setActionError(
          "Er ging iets mis bij het indienen van het aankoopverzoek.",
        );

        return false;
      }
      await fetchRequests();
      return true;
    } catch {
      setActionError(
        "Er ging iets mis bij het indienen van het aankoopverzoek.",
      );
      return false;
    } finally {
      setIsSubmitting(false);
    }
  }

  /**
   * Keurt een aankoopverzoek goed
   * Doet dit via een PATCH request naar het /approve endpoint van het aankoopverzoek
   * @param id het ID van het aankoopverzoek
   * @param note de notitie voor het goedkeuren
   * @returns {Promise<boolean>} true als het goedkeuren succesvol was, anders false
   */
  async function approveRequest(id: number, note: string): Promise<boolean> {
    if (!apiUrl || approvingId !== null) return false;

    try {
      setApprovingId(id);
      setActionError("");

      const response = await fetch(
        `${apiUrl}/purchase-requests/${id}/approve`,
        {
          method: "PATCH",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ note: note.trim() || null }),
        },
      );

      if (!response.ok) {
        setActionError("Er ging iets mis bij het goedkeuren.");
        return false;
      }

      await fetchRequests();
      return true;
    } catch {
      setActionError("Er ging iets mis bij het goedkeuren.");
      return false;
    } finally {
      setApprovingId(null);
    }
  }

  /**
   * Keurt een aankoopverzoek af
   * Doet dit via een soft-delete (roept het /reject endpoint aan, niet /admin-delete)
   * @param id het ID van het aankoopverzoek
   * @param note de notitie voor het afwijzen
   * @returns {Promise<boolean>} true als het afwijzen succesvol was, anders false
   */
  async function rejectRequest(id: number, note: string): Promise<boolean> {
    if (!apiUrl || rejectingId !== null) return false;

    try {
      setRejectingId(id);
      setActionError("");

      const response = await fetch(`${apiUrl}/purchase-requests/${id}/reject`, {
        method: "PATCH",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ note: note.trim() || null }),
      });

      if (!response.ok) {
        setActionError("Er ging iets mis bij het afwijzen.");
        return false;
      }

      await fetchRequests();
      return true;
    } catch {
      setActionError("Er ging iets mis bij het afwijzen.");
      return false;
    } finally {
      setRejectingId(null);
    }
  }

  async function deleteRequest(id: number): Promise<boolean> {
    if (!apiUrl || deletingId !== null) return false;

    try {
      setDeletingId(id);
      setActionError("");

      const response = await fetch(`${apiUrl}/purchase-requests/${id}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (!response.ok) {
        setActionError("Er ging iets mis bij het verwijderen.");
        return false;
      }

      await fetchRequests();
      return true;
    } catch {
      setActionError("Er ging iets mis bij het verwijderen.");
      return false;
    } finally {
      setDeletingId(null);
    }
  }

  return {
    requests,
    loading,
    error,
    actionError,
    isSubmitting,
    approvingId,
    rejectingId,
    deletingId,
    setActionError,
    createRequest,
    approveRequest,
    rejectRequest,
    deleteRequest,
  };
}
