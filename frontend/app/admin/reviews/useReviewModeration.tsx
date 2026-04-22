"use client";

import { useCallback, useEffect, useState } from "react";

export type ReviewStatus = "AWAITING_MODERATION" | "APPROVED" | "REJECTED";
export type ReportReason = "FOUT_TAALGEBRUIK" | "SPAM" | "ANDERE";

export interface MeResponse {
  role: "STUDENT" | "TEACHER" | "BIBLIOTHEEKBEHEERDER" | "ADMIN" | "OTHER";
  school: {
    id: number;
    name: string;
  } | null;
}

export interface ReviewFlagDetail {
  flaggerUid: string;
  reason: ReportReason;
}

export interface ModerationReview {
  id: number;
  userId: number;
  userSmartschoolUid: string;
  userRole: string;
  schoolId: number;
  schoolName: string;
  bookISBN: string;
  bookTitle: string;
  text: string;
  reviewDate: string;
  reviewStatus: ReviewStatus;
  rating: number;
  flagCount: number;
  flagDetails: ReviewFlagDetail[];
  adminDeleted: boolean;
  adminDeleteNote: string | null;
}

function extractServerErrorMessage(response: Response, fallback: string) {
  return response
    .clone()
    .json()
    .then((payload: Record<string, unknown>) => {
      const message =
        (payload?.message as string | undefined) ??
        (payload?.error as string | undefined) ??
        (payload?.details as string | undefined);

      return message?.trim() || fallback;
    })
    .catch(async () => {
      const text = (await response.text().catch(() => "")).trim();
      return text || fallback;
    });
}

export function useReviewModeration(apiUrl?: string) {
  const [me, setMe] = useState<MeResponse | null>(null);
  const [reviews, setReviews] = useState<ModerationReview[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [deleteReasonError, setDeleteReasonError] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const [approvingReviewId, setApprovingReviewId] = useState<number | null>(
    null,
  );
  const [hardDeletingReviewId, setHardDeletingReviewId] = useState<
    number | null
  >(null);

  const fetchModerationReviews = useCallback(async () => {
    if (!apiUrl) {
      throw new Error("NEXT_PUBLIC_API_URL ontbreekt");
    }

    const response = await fetch(`${apiUrl}/reviews/moderation`, {
      credentials: "include",
    });

    if (!response.ok) {
      throw new Error("Kon reviewmeldingen niet ophalen");
    }

    const data: ModerationReview[] = await response.json();
    setReviews(data);
  }, [apiUrl]);

  useEffect(() => {
    async function load() {
      if (!apiUrl) {
        setError("NEXT_PUBLIC_API_URL ontbreekt");
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError("");

        const meResponse = await fetch(`${apiUrl}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          setError("Je bent niet ingelogd");
          return;
        }

        const meData: MeResponse = await meResponse.json();
        setMe(meData);

        await fetchModerationReviews();
      } catch {
        setError("Er ging iets mis tijdens het laden van reviewmoderatie");
      } finally {
        setLoading(false);
      }
    }

    load();
  }, [apiUrl, fetchModerationReviews]);

  async function approveReview(reviewId: number) {
    if (!apiUrl || approvingReviewId !== null) {
      return;
    }

    try {
      setApprovingReviewId(reviewId);
      setActionError("");

      const response = await fetch(`${apiUrl}/reviews/${reviewId}/approve`, {
        method: "PATCH",
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Kon review niet goedkeuren");
      }

      await fetchModerationReviews();
    } catch {
      setActionError("Er ging iets mis tijdens het goedkeuren van de review.");
    } finally {
      setApprovingReviewId(null);
    }
  }

  async function softRejectReview(reviewId: number | null, reason: string) {
    if (!apiUrl || reviewId === null || isDeleting) {
      return false;
    }

    const trimmedReason = reason.trim();

    try {
      setIsDeleting(true);
      setDeleteReasonError("");
      setActionError("");

      const response = await fetch(
        `${apiUrl}/reviews/${reviewId}/admin-delete`,
        {
          method: "PATCH",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ reason: trimmedReason || null }),
        },
      );

      if (!response.ok) {
        const fallback = `Kon review niet afkeuren (HTTP ${response.status}).`;
        const message = await extractServerErrorMessage(response, fallback);
        setDeleteReasonError(message);
        return false;
      }

      await fetchModerationReviews();
      return true;
    } catch {
      setActionError("Er ging iets mis tijdens het afkeuren van de review.");
      return false;
    } finally {
      setIsDeleting(false);
    }
  }

  async function hardDeleteReview(reviewId: number) {
    if (!apiUrl || hardDeletingReviewId !== null) {
      return;
    }

    try {
      setHardDeletingReviewId(reviewId);
      setActionError("");

      const response = await fetch(`${apiUrl}/reviews/${reviewId}/librarian`, {
        method: "DELETE",
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Kon review niet permanent verwijderen");
      }

      await fetchModerationReviews();
    } catch {
      setActionError(
        "Er ging iets mis tijdens het permanent verwijderen van de review.",
      );
    } finally {
      setHardDeletingReviewId(null);
    }
  }

  return {
    me,
    reviews,
    loading,
    error,
    actionError,
    deleteReasonError,
    isDeleting,
    approvingReviewId,
    hardDeletingReviewId,
    setActionError,
    setDeleteReasonError,
    fetchModerationReviews,
    approveReview,
    softRejectReview,
    hardDeleteReview,
  };
}
