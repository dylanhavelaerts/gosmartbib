"use client";

import {
  type CSSProperties,
  use,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Book } from "../../interfaces/Book";
import { MeResponse } from "../../interfaces/user";
import Link from "next/link";
import "./detailpage.css";
import ReviewSection from "@/app/components/reviewsection/reviewsection";
import NotificationBell from "@/app/components/Notifications/Notification";

interface ReviewWithRating {
  rating: number;
}

interface PersonalList {
  id: number;
  title: string;
  taskDescription?: string | null;
  bookIds: number[];
}

type ExtendedBook = Book & { previewLink?: string };

export default function DetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [book, setBook] = useState<ExtendedBook | null>(null);
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png");
  const [averageReviewRating, setAverageReviewRating] = useState<number | null>(
    null,
  );
  const [currentUser, setCurrentUser] = useState<MeResponse | null>(null);
  const [readingLists, setReadingLists] = useState<PersonalList[]>([]);
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [addingToList, setAddingToList] = useState<number | null>(null);
  const [addMsg, setAddMsg] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

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
    if (!currentUser) return;
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/reading-lists`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: PersonalList[]) => {
        const personal = (Array.isArray(data) ? data : []).filter(
          (l: PersonalList & { listType?: string; ownList?: boolean }) =>
            l.listType === "PERSONAL" && l.ownList !== false,
        );
        setReadingLists(personal);
      })
      .catch(() => setReadingLists([]));
  }, [currentUser]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(e.target as Node)
      ) {
        setDropdownOpen(false);
      }
    };
    if (dropdownOpen)
      document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [dropdownOpen]);

  // Ophalen van het boek (Inclusief de kant-en-klare link uit de backend!)
  useEffect(() => {
    if (!id) return;
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/${id}`, {
      credentials: "include",
    })
      .then((res) => res.json())
      .then((data: ExtendedBook) => {
        setBook(data);
        setImgSrc(
          data.thumbnail?.trim() || "/No-Image-Available-Placeholder.png",
        );
        fetchAverageReviewRating(data.isbn);
      })
      .catch((error) => console.error(error));
  }, [id, fetchAverageReviewRating]);

  const handleAddToList = async (list: PersonalList) => {
    if (!book) return;
    if (list.bookIds.includes(book.id)) {
      setAddMsg({
        type: "error",
        text: `"${book.title}" staat al in "${list.title}".`,
      });
      setDropdownOpen(false);
      setTimeout(() => setAddMsg(null), 3000);
      return;
    }

    setAddingToList(list.id);
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/reading-lists/personal/${list.id}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          credentials: "include",
          body: JSON.stringify({
            title: list.title,
            taskDescription: list.taskDescription ?? null,
            bookIds: [...list.bookIds, book.id],
          }),
        },
      );
      if (!res.ok) throw new Error();
      setReadingLists((prev) =>
        prev.map((l) =>
          l.id === list.id ? { ...l, bookIds: [...l.bookIds, book.id] } : l,
        ),
      );
      setAddMsg({ type: "success", text: `Toegevoegd aan "${list.title}".` });
    } catch {
      setAddMsg({ type: "error", text: "Toevoegen mislukt. Probeer opnieuw." });
    } finally {
      setAddingToList(null);
      setDropdownOpen(false);
      setTimeout(() => setAddMsg(null), 3000);
    }
  };

  if (!book) return <p>Loading...</p>;
  // Bepaal de beschikbaarheid op basis van de inventory voor de school van de gebruiker
  const inv = currentUser
    ? book.inventories?.find((i) => i.schoolId === currentUser.school?.id)
    : undefined;
  const available = inv?.availableCopies ?? 0;
  const total = inv?.totalCopies ?? 0;
  // Fix http naar https als de opgeslagen link nog http is
  let displayLink = book.previewLink;
  if (displayLink && displayLink.startsWith("http://")) {
    displayLink = displayLink.replace("http://", "https://");
  }

  // Bepaal de tekst op basis van het type link
  const isReaderLink = displayLink?.includes("play.google.com/books/reader");

  return (
    <main className="detailPage">
      <Link href="/catalog" className="backLink">
        ← Terug naar catalogus
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
          </div>
          {currentUser && available === 0 && (
            <NotificationBell
              apiPath={`/books/${book.id}/notification`}
              className="coverBell"
              label="Notificaties aanzetten"
            />
          )}

          {/* Beschikbaarheidsbadge staat nu onder de cover */}
          {currentUser && (
            <span
              className={`coverBadge ${available > 0 ? "available" : "unavailable"}`}
            >
              {inv ? `${available}/${total} beschikbaar` : "Niet beschikbaar"}
            </span>
          )}

          {/* Toevoegen aan leeslijst */}
          {currentUser && (
            <div className="addToListWrapper" ref={dropdownRef}>
              <button
                className="addToListBtn"
                onClick={() => setDropdownOpen((o) => !o)}
                aria-expanded={dropdownOpen}
              >
                + Toevoegen
                <span className="addToListChevron">
                  {dropdownOpen ? "▲" : "▼"}
                </span>
              </button>

              {dropdownOpen && (
                <div className="addToListDropdown">
                  {readingLists.length === 0 ? (
                    <p className="addToListEmpty">Geen leeslijsten gevonden.</p>
                  ) : (
                    readingLists.map((list) => {
                      const alreadyAdded = (list.bookIds ?? []).includes(
                        book.id,
                      );
                      return (
                        <button
                          key={list.id}
                          className={`addToListItem ${alreadyAdded ? "addToListItem--added" : ""}`}
                          onClick={() => handleAddToList(list)}
                          disabled={addingToList === list.id || alreadyAdded}
                        >
                          <span className="addToListItemTitle">
                            {list.title}
                          </span>
                          {alreadyAdded && (
                            <span className="addToListItemCheck">✓</span>
                          )}
                        </button>
                      );
                    })
                  )}
                </div>
              )}

              {addMsg && (
                <p className={`addToListMsg addToListMsg--${addMsg.type}`}>
                  {addMsg.text}
                </p>
              )}
            </div>
          )}

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

            <hr className="detailDivider" />

            {/* DE KNOP NAAR GOOGLE PLAY BOOKS PREVIEW */}
            <div className="detailPreviewSection">
              <h2>Leesvoorbeeld</h2>

              {displayLink ? (
                <a
                  href={displayLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="detailPreviewBtn"
                >
                  Bekijk de eerste pagina's
                </a>
              ) : (
                <p className="detailPreviewEmpty">
                  Voor dit boek is helaas geen digitaal leesvoorbeeld
                  beschikbaar.
                </p>
              )}
            </div>

            <hr className="detailDivider" />

            <div className="detailReviews">
              <ReviewSection
                isbn={book.isbn}
                onReviewSubmitted={() => fetchAverageReviewRating(book.isbn)}
              />
            </div>
          </div>
        </div>
      </div>
    </main>
  );
}
