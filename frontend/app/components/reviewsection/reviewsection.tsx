"use client";

import { useEffect, useState } from "react";
import "./reviewsection.css";
import Pagination from "../../catalog/pagination";

const REVIEWS_PER_PAGE = 5;

interface ReviewSummary {
  id: number;
  userId: number;
  userRole: string;
  text: string;
  reviewDate: string;
  rating: number;
}

interface ReviewSectionProps {
  isbn: string;
  onReviewSubmitted?: () => void;
}

function StarRating({
  value,
  onChange,
}: {
  value: number;
  onChange?: (v: number) => void;
}) {
  const [hovered, setHovered] = useState(0);
  const interactive = !!onChange;

  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((star) => (
        <span
          key={star}
          className={`star ${star <= (hovered || value) ? "filled" : ""} ${interactive ? "interactive" : ""}`}
          onMouseEnter={() => interactive && setHovered(star)}
          onMouseLeave={() => interactive && setHovered(0)}
          onClick={() => onChange?.(star)}
        >
          ★
        </span>
      ))}
    </div>
  );
}

function ReviewCard({
  review,
  onFlag,
  isFlagging,
}: {
  review: ReviewSummary;
  onFlag: (reviewId: number) => void;
  isFlagging: boolean;
}) {
  const formattedDate = new Date(review.reviewDate).toLocaleDateString(
    "nl-BE",
    {
      day: "numeric",
      month: "long",
      year: "numeric",
    },
  );

  const roleLabel: Record<string, string> = {
    STUDENT: "Student",
    TEACHER: "Leerkracht",
    LIBRARIAN: "Bibliothecaris",
  };

  return (
    <div className="reviewCard">
      <div className="reviewCardHeader">
        <div className="reviewCardMeta">
          <span className="reviewRole">
            {roleLabel[review.userRole] ?? review.userRole}
          </span>
          <span className="reviewDate">{formattedDate}</span>
        </div>
        <StarRating value={review.rating} />
      </div>
      {review.text && <p className="reviewText">{review.text}</p>}
      <div className="reviewCardActions">
        <button
          className="flagReviewBtn"
          onClick={() => onFlag(review.id)}
          disabled={isFlagging}
          title="Meld review"
          aria-label="Meld review"
        >
          {isFlagging ? "..." : "⚑"}
        </button>
      </div>
    </div>
  );
}

function ReviewForm({
  isbn,
  onSubmitted,
}: {
  isbn: string;
  onSubmitted: () => void;
}) {
  const [rating, setRating] = useState(0);
  const [text, setText] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

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

    setError("");
    setLoading(true);

    try {
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/reviews`, {
        method: "POST",
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
      <h3 className="reviewFormTitle">Jouw review</h3>
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
          {loading ? "Bezig..." : "Plaatsen"}
        </button>
      </div>
    </div>
  );
}

export default function ReviewSection({
  isbn,
  onReviewSubmitted,
}: ReviewSectionProps) {
  const [reviews, setReviews] = useState<ReviewSummary[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [flaggingReviewId, setFlaggingReviewId] = useState<number | null>(null);

  const totalPages = Math.ceil(reviews.length / REVIEWS_PER_PAGE);
  const paginatedReviews = reviews.slice(
    (currentPage - 1) * REVIEWS_PER_PAGE,
    currentPage * REVIEWS_PER_PAGE,
  );

  function fetchReviews() {
    setLoading(true);
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/reviews/book/${isbn}`)
      .then((res) => res.json())
      .then((data: ReviewSummary[]) => {
        setReviews(data);
        setCurrentPage(1);
      })
      .catch((err) => console.error(err))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    fetchReviews();
  }, [isbn]);

  function handleSubmitted() {
    setShowForm(false);
    fetchReviews();
    onReviewSubmitted?.();
  }

  async function handleFlag(reviewId: number) {
    setFlaggingReviewId(reviewId);

    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reviews/${reviewId}/flag`,
        {
          method: "PATCH",
          credentials: "include",
        },
      );

      if (!res.ok) {
        return;
      }

      fetchReviews();
    } catch {
    } finally {
      setFlaggingReviewId(null);
    }
  }

  return (
    <div className="reviewSection">
      <div className="reviewSectionHeader">
        <h2 className="reviewSectionTitle">Reviews</h2>
        <button
          className="placeReview"
          onClick={() => setShowForm((prev) => !prev)}
        >
          {showForm ? "Annuleren" : "Plaats review"}
        </button>
      </div>

      {showForm && <ReviewForm isbn={isbn} onSubmitted={handleSubmitted} />}

      {loading ? (
        <p className="reviewLoading">Reviews laden...</p>
      ) : reviews.length === 0 ? (
        <p className="reviewEmpty">Nog geen reviews, wees de eerste!</p>
      ) : (
        <>
          <div className="reviewList">
            {paginatedReviews.map((review) => (
              <ReviewCard
                key={review.id}
                review={review}
                onFlag={handleFlag}
                isFlagging={flaggingReviewId === review.id}
              />
            ))}
          </div>

          {reviews.length > REVIEWS_PER_PAGE && (
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          )}
        </>
      )}
    </div>
  );
}
