"use client";

import {
  type CSSProperties,
  use,
  useCallback,
  useEffect,
  useRef,
  useState,
} from "react";
import { Book, BookCopy, SnowballSection } from "../../interfaces/Book";
import { MeResponse } from "../../interfaces/user";
import Link from "next/link";
import { useRouter } from "next/navigation";
import "./detailpage.css";
import ReviewSection from "@/app/components/reviewsection/reviewsection";
import NotificationBell from "@/app/components/Notifications/Notification";
import BookCarousel from "@/app/components/BookCarousel";
import LessonTipSection from "./LessonTipSection";

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
  const [snowballSections, setSnowballSections] = useState<SnowballSection[]>(
    [],
  );
  const [activeTab, setActiveTab] = useState<"info" | "lestips">("info");
  const [showAllLocations, setShowAllLocations] = useState(false);
  const [expandedInventories, setExpandedInventories] = useState<Set<number>>(new Set());
  const [copiesCache, setCopiesCache] = useState<Record<number, BookCopy[]>>({});
  const [copiesLoading, setCopiesLoading] = useState<Set<number>>(new Set());
  const router = useRouter();

  const isStaff =
    currentUser?.role === "TEACHER" ||
    currentUser?.role === "LIBRARIAN" ||
    currentUser?.role === "ADMIN";

  const canSeeLestips =
    currentUser?.role === "TEACHER" ||
    currentUser?.role === "LIBRARIAN";

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
      .then((res) => {
        if (!res.ok) throw new Error("Book not found");
        return res.json();
      })
      .then((data: ExtendedBook) => {
        setBook(data);
        setImgSrc(
          data.thumbnail?.trim() || "/No-Image-Available-Placeholder.png",
        );
        fetchAverageReviewRating(data.isbn);
      })
      .catch((error) => console.error(error));
  }, [id, fetchAverageReviewRating]);

  // Ophalen van de snowball-secties bepaald boek
  useEffect(() => {
    if (!book) return;
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/${book.id}/snowball`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: SnowballSection[]) => {
        const category = data.filter((s) => s.type === "CATEGORY");
        const author = data.find((s) => s.type === "AUTHOR");
        setSnowballSections(
          [...category, author].filter(Boolean) as SnowballSection[],
        );
      })
      .catch(() => {});
  }, [book]);

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

  // Bepaal de beschikbaarheid op basis van de inventory
  // Studenten zien enkel hun eigen school (wordt ook al door backend gefilterd)
  // Staff ziet alle scholen en campussen.
  const userInventories = currentUser
    ? book.inventories?.filter(
        (i) => isStaff || i.schoolId === currentUser.school?.id,
      ) || []
    : [];

  // Bereken de totalen over alle gefilterde campussen
  const available = userInventories.reduce(
    (acc, inv) => acc + (inv.availableCopies || 0),
    0,
  );
  const total = userInventories.reduce(
    (acc, inv) => acc + (inv.totalCopies || 0),
    0,
  );

  // Bepaal of het campus-lijstje zichtbaar moet zijn:
  // Altijd voor staf (zien scholen), voor studenten enkel als er minstens één echte campusnaam is ingevuld.
  const showCampusBreakdown =
    userInventories.length > 0 &&
    (isStaff ||
      userInventories.some((inv) => inv.campus && inv.campus.trim() !== ""));

  // Fix http naar https als de opgeslagen link nog http is
  let displayLink = book.previewLink;
  if (displayLink && displayLink.startsWith("http://")) {
    displayLink = displayLink.replace("http://", "https://");
  }

  return (
    <main className="detailPage">
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
          {currentUser && (
            <div className="availabilityWrapper">
              <span
                className={`coverBadge ${available > 0 ? "available" : "unavailable"}`}
              >
                {userInventories.length > 0
                  ? `${available}/${total} beschikbaar`
                  : "Niet beschikbaar"}
              </span>
            </div>
          )}

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
          {showCampusBreakdown && (
            <div className="campusBreakdown">
              <p className="campusBreakdownTitle">Locaties:</p>
              <ul className="campusList">
                {(() => {
                  const formatSchoolName = (name: string | undefined) => {
                    if (!name) return "";
                    const cleanName = name
                      .replace("https://", "")
                      .replace(".smartschool.be", "")
                      .replace("/", "");
                    return cleanName
                      .split("-")
                      .map(
                        (word) => word.charAt(0).toUpperCase() + word.slice(1),
                      )
                      .join(" ");
                  };

                  const ownSchoolInventories = isStaff
                    ? userInventories.filter(
                        (i) => i.schoolId === currentUser?.school?.id,
                      )
                    : userInventories;
                  const otherInventories = isStaff
                    ? userInventories.filter(
                        (i) => i.schoolId !== currentUser?.school?.id,
                      )
                    : [];
                  const visibleInventories =
                    isStaff && !showAllLocations
                      ? ownSchoolInventories
                      : userInventories;

                  const conditionLabel: Record<string, string> = {
                    GOOD: "Goed",
                    DAMAGED: "Beschadigd",
                    BROKEN: "Kapot",
                    LOST: "Verloren",
                  };

                  const handleToggleCampus = async (invId: number) => {
                    if (expandedInventories.has(invId)) {
                      setExpandedInventories((prev) => {
                        const next = new Set(prev);
                        next.delete(invId);
                        return next;
                      });
                      return;
                    }
                    setExpandedInventories((prev) => new Set(prev).add(invId));
                    if (copiesCache[invId]) return;
                    setCopiesLoading((prev) => new Set(prev).add(invId));
                    try {
                      const res = await fetch(
                        `${process.env.NEXT_PUBLIC_API_URL}/books/${book.id}/copies/labels?inventoryId=${invId}`,
                        { credentials: "include" },
                      );
                      if (res.ok) {
                        const data: BookCopy[] = await res.json();
                        setCopiesCache((prev) => ({ ...prev, [invId]: data }));
                      }
                    } finally {
                      setCopiesLoading((prev) => {
                        const next = new Set(prev);
                        next.delete(invId);
                        return next;
                      });
                    }
                  };

                  return (
                    <>
                      {visibleInventories.map((inv, idx) => {
                        const invId = inv.id as number;
                        const displayName =
                          isStaff && inv.schoolName
                            ? `${formatSchoolName(inv.schoolName)} (${inv.campus || "Hoofdcampus"})`
                            : inv.campus || "Hoofdcampus";
                        const isExpanded = expandedInventories.has(invId);
                        const copies = copiesCache[invId];
                        const loading = copiesLoading.has(invId);
                        return (
                          <li key={idx} className="campusItem campusItemExpandable">
                            <button
                              className="campusItemRow"
                              onClick={() => invId && handleToggleCampus(invId)}
                              aria-expanded={isExpanded}
                            >
                              <span className="campusName" title={displayName}>
                                <span className="campusChevron">{isExpanded ? "▾" : "▸"}</span>
                                {displayName}
                              </span>
                              <span
                                className={`campusCount ${inv.availableCopies > 0 ? "text-success" : "text-error"}`}
                              >
                                {inv.availableCopies}/{inv.totalCopies}
                              </span>
                            </button>
                            {isExpanded && (
                              <ul className="copyList">
                                {loading && (
                                  <li className="copyItem copyItem--loading">Laden…</li>
                                )}
                                {!loading && copies?.map((copy) => (
                                  <li key={copy.copyId} className="copyItem">
                                    <span className="copyNumber">#{copy.copyNumber}</span>
                                    <span className="copyBarcode">{copy.barcode ?? "—"}</span>
                                    <span className={`copyCondition copyCondition--${copy.condition.toLowerCase()}`}>
                                      {conditionLabel[copy.condition]}
                                    </span>
                                  </li>
                                ))}
                              </ul>
                            )}
                          </li>
                        );
                      })}
                      {isStaff && otherInventories.length > 0 && (
                        <li className="campusToggleItem">
                          <button
                            className="campusToggleBtn"
                            onClick={() =>
                              setShowAllLocations((prev) => !prev)
                            }
                          >
                            {showAllLocations
                              ? "Minder tonen"
                              : `Toon alle (${otherInventories.length} andere ${otherInventories.length === 1 ? "locatie" : "locaties"})`}
                          </button>
                        </li>
                      )}
                    </>
                  );
                })()}
              </ul>
            </div>
          )}

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
          <p className="detailAuthors">
            door{" "}
            {book.authors?.map((author, index) => (
              <span key={index}>
                <Link
                  href={`/catalog?search=${encodeURIComponent(author)}`}
                  className="authorLink"
                >
                  {author}
                </Link>
                {index < book.authors.length - 1 ? ", " : ""}
              </span>
            ))}
          </p>

          {canSeeLestips && (
            <nav className="detailNavBar">
              <button
                className={activeTab === "info" ? "active" : ""}
                onClick={() => setActiveTab("info")}
              >
                Boekinfo
              </button>
              <button
                className={activeTab === "lestips" ? "active" : ""}
                onClick={() => setActiveTab("lestips")}
              >
                Lestips
              </button>
            </nav>
          )}

          <div className="tabContent">
            {activeTab === "info" && (
              <>
                <div className="detailDescription">
                  <h2>Waar gaat het over?</h2>
                  <p>{book.description}</p>
                </div>

                <hr className="detailDivider" />

                <div className="detailMetaRow">
                  {book.readingLevel && (
                    <div className="metaCol">
                      <span className="metaLabel">Leesniveau</span>
                      <span className="metaValue">{book.readingLevel}</span>
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
                      <span className="metaValue">
                        {book.pageCount} pagina's
                      </span>
                    </div>
                  )}
                </div>

                <hr className="detailDivider" />

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
                    onReviewSubmitted={() =>
                      fetchAverageReviewRating(book.isbn)
                    }
                  />
                </div>
              </>
            )}

            {activeTab === "lestips" && canSeeLestips && (
              <LessonTipSection bookId={book.id} />
            )}
          </div>
        </div>
      </div>

      {snowballSections.length > 0 && (
        <div className="snowballContainer">
          {snowballSections.map((section, i) => (
            <BookCarousel key={i} section={section} />
          ))}
        </div>
      )}
    </main>
  );
}
