"use client";

import { useEffect, useState } from "react";
import StarRating from "./StarRating";

interface ReviewFormProps {
  isbn: string;
  onSubmitted: () => void;
  mode?: "create" | "edit";
  reviewId?: number;
  initialText?: string;
  initialRating?: number;
}

export default function ReviewForm({
  isbn,
  onSubmitted,
  mode = "create",
  reviewId,
  initialText = "",
  initialRating = 0,
}: ReviewFormProps) {
  const [rating, setRating] = useState(initialRating);
  const [text, setText] = useState(initialText);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setText(initialText);
    setRating(initialRating);
    setError("");
  }, [initialText, initialRating, mode, reviewId]);

  async function extractErrorMessage(res: Response): Promise<string> {
    try {
      const payload = await res.clone().json();
      if (typeof payload?.message === "string" && payload.message.trim()) {
        return payload.message;
      }
      if (typeof payload?.error === "string" && payload.error.trim()) {
        return payload.error;
      }
      if (typeof payload?.detail === "string" && payload.detail.trim()) {
        return payload.detail;
      }
    } catch {
      // Als er niet een specifiek bericht is, geef dan een generieke foutmelding terug.
    }

    try {
      const text = (await res.text()).trim();
      if (text) return text;
    } catch {
      // Als er niet een specifiek bericht is, geef dan een generieke foutmelding terug.
    }

    return "Er liep iets fout. Probeer opnieuw.";
  }

  async function handleSubmit() {
    if (rating === 0) {
      setError("Geef minstens een ster.");
      return;
    }

    if (mode === "edit" && !reviewId) {
      setError("Review niet gevonden.");
      return;
    }

    setError("");
    setLoading(true);

    try {
      const endpoint =
        mode === "edit"
          ? `${process.env.NEXT_PUBLIC_API_URL}/reviews/${reviewId}`
          : `${process.env.NEXT_PUBLIC_API_URL}/reviews`;

      const res = await fetch(endpoint, {
        method: mode === "edit" ? "PATCH" : "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ bookIsbn: isbn, text, rating }),
      });

      if (!res.ok) {
        setError(await extractErrorMessage(res));
        return;
      }

      setText("");
      setRating(0);
      onSubmitted();
    } catch {
      setError("Er liep iets fout. Probeer opnieuw.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="reviewForm">
      <h3 className="reviewFormTitle">
        {mode === "edit" ? "Bewerk je review" : "Jouw review"}
      </h3>
      <StarRating value={rating} onChange={setRating} />
      <textarea
        className="reviewTextArea"
        placeholder="Schrijf een review... (optioneel)"
        value={text}
        onChange={(e) => setText(e.target.value)}
        maxLength={255}
        rows={4}
      />
      <div className="reviewFormFooter">
        <span className="charCount">{text.length}/255</span>
        {error && <span className="reviewError">{error}</span>}
        <button
          className="submitReviewBtn"
          onClick={handleSubmit}
          disabled={loading}
        >
          {loading ? "Bezig..." : mode === "edit" ? "Opslaan" : "Plaatsen"}
        </button>
      </div>
    </div>
  );
}
