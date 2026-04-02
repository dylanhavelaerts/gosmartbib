"use client";

import {
  type CSSProperties,
  use,
  useCallback,
  useEffect,
  useState,
} from "react";
import { Book } from "../../interfaces/Book";
import Link from "next/link";
import "./detailpage.css";
import ReviewSection from "@/app/components/reviewsection/reviewsection";

interface ReviewWithRating {
  rating: number;
}

export default function DetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [book, setBook] = useState<Book | null>(null);
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png");
  const [averageReviewRating, setAverageReviewRating] = useState<number | null>(
    null,
  );

  const [activeTab, setActiveTab] = useState<"details" | "reviews">("details");

  const normalizedRating =
    typeof averageReviewRating === "number"
      ? Math.min(5, Math.max(0, averageReviewRating))
      : 0;
  const roundedRating = normalizedRating.toFixed(1);

  const fetchAverageReviewRating = useCallback(async (isbn: string) => {
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reviews/book/${isbn}`,
      );

      if (!res.ok) {
        setAverageReviewRating(null);
        return;
      }

      const reviews: ReviewWithRating[] = await res.json();
      if (!Array.isArray(reviews) || reviews.length === 0) {
        setAverageReviewRating(null);
        return;
      }

      const sum = reviews.reduce(
        (acc, review) => acc + (review.rating || 0),
        0,
      );
      setAverageReviewRating(sum / reviews.length);
    } catch {
      setAverageReviewRating(null);
    }
  }, []);

  useEffect(() => {
    if (!id) return;

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/${id}`)
      .then((res) => res.json())
      .then((data: Book) => {
        setBook(data);
        setImgSrc(
          data.thumbnail?.trim() || "/No-Image-Available-Placeholder.png",
        );
        fetchAverageReviewRating(data.isbn);
      })
      .catch((error) => console.error(error));
  }, [id, fetchAverageReviewRating]);

  if (!book) return <p>Loading...</p>;

  return (
    <main className="detailPage">
      <Link href="/catalog" className="backLink">
        ← Terug naar catalogus
      </Link>

      <div className="detailContainer">
        <div className="detailLeft">
          <img
            src={imgSrc}
            alt={book.title}
            onError={() => setImgSrc("/No-Image-Available-Placeholder.png")}
            className="detailCover"
          />
          <div className="detailUnder">
            <div className="detailRating">
              <div
                className="detailRatingStars"
                aria-label={`Gemiddelde score ${roundedRating} op 5`}
              >
                {[1, 2, 3, 4, 5].map((star) => (
                  <span
                    key={star}
                    className="ratingStar"
                    style={
                      {
                        "--fill": `${
                          Math.max(
                            0,
                            Math.min(1, normalizedRating - (star - 1)),
                          ) * 100
                        }%`,
                      } as CSSProperties
                    }
                  >
                    ★
                  </span>
                ))}
              </div>
              <p className="detailRatingText">
                {averageReviewRating === null ? "-/5" : `${roundedRating}/5`}
              </p>
            </div>
            <p>
              <img className="bookIcon" src={"/book-alt.png"} alt="Pages" />{" "}
              {book.pageCount} pagina's
            </p>
            <p>
              <img
                className="bookIcon"
                src={"/book-closed.png"}
                alt="Language"
              />{" "}
              Taal: {book.language.toUpperCase()}
            </p>
          </div>
        </div>

        <div className="detailRight">
          <h1 className="detailTitle">{book.title}</h1>
          <p className="detailAuthors">door {book.authors?.join(", ")}</p>

          <div className="detailBadges">
            {book.categories?.map((cat) => (
              <span key={cat} className="badge">
                {cat}
              </span>
            ))}
          </div>

          <div className="detailNavBar">
            <button
              className={activeTab === "details" ? "active" : ""}
              onClick={() => setActiveTab("details")}
            >
              Details
            </button>
            <button
              className={activeTab === "reviews" ? "active" : ""}
              onClick={() => setActiveTab("reviews")}
            >
              Reviews
            </button>
          </div>

          {activeTab === "details" ? (
            <div className="tabContent">
              <div className="detailDescription">
                <h2>Waar gaat het over?</h2>
                <p>{book.description}</p>
              </div>
              <div className="detailInfoBoxes">
                <div className="infoBox infoBoxUitgever">
                  <span className="infoBoxLabel">Uitgever</span>
                  <span className="infoBoxValue">{book.publisher}</span>
                </div>
                <div className="infoBox infoBoxJaar">
                  <span className="infoBoxLabel">Jaar</span>
                  <span className="infoBoxValue">{book.publishedYear}</span>
                </div>
              </div>
              <div className="infoBox ISBNBox">
                <span className="infoBoxLabel">ISBN</span>
                <span className="infoBoxValue">{book.isbn}</span>
              </div>
            </div>
          ) : (
            <div className="tabContent">
              <div className="detailDescription">
                <ReviewSection
                  isbn={book.isbn}
                  onReviewSubmitted={() => fetchAverageReviewRating(book.isbn)}
                />
              </div>
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
