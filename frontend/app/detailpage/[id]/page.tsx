"use client";

import {
  type CSSProperties,
  use,
  useCallback,
  useEffect,
  useState,
} from "react";
import { Book } from "../../interfaces/Book";
import { MeResponse } from "../../interfaces/user";
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
  const [currentUser, setCurrentUser] = useState<MeResponse | null>(null);
  const [activeTab, setActiveTab] = useState<"details" | "reviews">("details");

  const isStaff =
    currentUser?.role === "TEACHER" ||
    currentUser?.role === "BIBLIOTHEEKBEHEERDER" ||
    currentUser?.role === "ADMIN";

  const normalizedRating =
    typeof averageReviewRating === "number"
      ? Math.min(5, Math.max(0, averageReviewRating))
      : 0;
  const roundedRating = normalizedRating.toFixed(1);

  const fetchAverageReviewRating = useCallback(async (isbn: string) => {
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reviews/book/${isbn}`,
        { credentials: "include" },
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
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/me`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => setCurrentUser(data))
      .catch(() => setCurrentUser(null));
  }, []);

  useEffect(() => {
    if (!id) return;
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/${id}`, {
      credentials: "include",
    })
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
        Terug naar catalogus
      </Link>

      <div className="detailContainer">
        {/* LEFT PANEL */}
        <div className="detailLeft">
          <div className="detailCoverWrapper">
            <img
              src={imgSrc}
              alt={book.title}
              onError={() => setImgSrc("/No-Image-Available-Placeholder.png")}
              className="detailCover"
            />
            {!isStaff &&
              currentUser &&
              (() => {
                const inv = book.inventories?.find(
                  (i) => i.schoolId === currentUser?.school?.id,
                );
                const available = inv?.availableCopies ?? 0;
                const total = inv?.totalCopies ?? 0;
                return (
                  <span
                    className={`coverBadge ${available > 0 ? "available" : "unavailable"}`}
                  >
                    {inv
                      ? `${available}/${total} beschikbaar`
                      : "Niet beschikbaar"}
                  </span>
                );
              })()}
          </div>

          <div className="detailRatingSection">
            <p className="detailInfoSectionTitle">Beoordeling</p>
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
          </div>

          <div className="detailInfoSection">
            <p className="detailInfoSectionTitle">Informatie</p>
            <div className="detailInfoRow">
              <span className="detailInfoLabel">Auteur</span>
              <span className="detailInfoValue">
                {book.authors?.join(", ")}
              </span>
            </div>
            <div className="detailInfoRow">
              <span className="detailInfoLabel">Uitgavedatum</span>
              <span className="detailInfoValue">{book.publishedYear}</span>
            </div>
            <div className="detailInfoRow">
              <span className="detailInfoLabel">ISBN</span>
              <span className="detailInfoValue">{book.isbn}</span>
            </div>
          </div>
        </div>

        {/* RIGHT PANEL */}
        <div className="detailRight">
          <h1 className="detailTitle">{book.title}</h1>
          <p className="detailAuthors">door {book.authors?.join(", ")}</p>

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
              Beoordeling
            </button>
          </div>

          {activeTab === "details" && (
            <div className="tabContent">
              <div className="detailDescription">
                <h2>Waar gaat het over?</h2>
                <p>{book.description}</p>
              </div>

              <hr className="detailDivider" />

              <div className="detailMetaRow">
                {book.ageRange && (
                  <div className="metaCol">
                    <span className="metaLabel">Leeftijd</span>
                    <span className="metaValue">{book.ageRange}</span>
                  </div>
                )}
                {book.categories?.length > 0 && (
                  <div className="metaCol">
                    <span className="metaLabel">Genre</span>
                    <span className="metaValue">
                      {book.categories.join(", ")}
                    </span>
                  </div>
                )}
                <div className="metaCol">
                  <span className="metaLabel">Taal</span>
                  <span className="metaValue">
                    {book.language?.toUpperCase()}
                  </span>
                </div>
                {book.pageCount && (
                  <div className="metaCol">
                    <span className="metaLabel">Dikte</span>
                    <span className="metaValue">{book.pageCount} pagina's</span>
                  </div>
                )}
              </div>
            </div>
          )}

          {activeTab === "reviews" && (
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
