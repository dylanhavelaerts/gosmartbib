"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "./context/AuthContext";

// Componenten
import HomeReadingLists from "./components/home/HomeReadingLists";
import BookCard from "./catalog/bookCard";

// Interfaces & Styling
import { ReadingListOverview } from "./interfaces/ReadingList";
import { Book } from "./interfaces/Book";
import "./dashboard.css";

type TabId = "urgent" | "spotlight" | "new" | "none";
type ReadingLevel = "A" | "B" | "C" | "D";
interface UrgentLoan {
  loanId: number;
  daysLeft: number;
  book: {
    id: number;
    title: string;
    thumbnail: string | null;
    isbn: string;
    authors: string[];
  } | null;
}

const READING_LEVELS: ReadingLevel[] = ["A", "B", "C", "D"];

const GREETINGS = [
  "Klaar om te lezen?",
  "Veel leesplezier!",
  "Wat ga je vandaag lezen?",
  "Duik in een goed boek!",
  "Ontdek je volgende favoriete boek!",
  "Tijd voor een nieuw avontuur tussen de pagina's!",
];

function getDefaultReadingLevelFromClassName(
  className?: string | null,
): ReadingLevel {
  if (!className) return "A";

  const year = Number(className.trim().charAt(0));

  if (year === 1 || year === 2) return "A";
  if (year === 3 || year === 4) return "B";
  if (year === 5 || year === 6 || year === 7) return "C";

  return "A";
}

function getDefaultReadingLevelForUser(
  user: {
    role?: string;
    classes?: {
      name?: string | null;
    }[];
  } | null,
): ReadingLevel {
  if (user?.role !== "STUDENT") {
    return "A";
  }

  const className = user.classes?.[0]?.name;

  return getDefaultReadingLevelFromClassName(className);
}

function calcDaysLeft(dueDateString: string): number {
  const due = new Date(dueDateString);
  const now = new Date();
  due.setHours(0, 0, 0, 0);
  now.setHours(0, 0, 0, 0);
  return Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

function formatDaysLabel(days: number): string {
  if (days < 0)
    return `${Math.abs(days)} dag${Math.abs(days) !== 1 ? "en" : ""} te laat`;
  if (days === 0) return "Vandaag inleveren";
  return `Nog ${days} dag${days !== 1 ? "en" : ""}`;
}

function badgeClass(days: number): string {
  return days < 0 ? "badge--overdue" : "badge--soon";
}

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const [books, setBooks] = useState<Book[]>([]);
  const [booksLoading, setBooksLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [greeting, setGreeting] = useState(GREETINGS[0]);
  const [firstName, setFirstName] = useState<string | null>(null);
  const [selectedReadingLevel, setSelectedReadingLevel] =
    useState<ReadingLevel | null>(null);

  // --- Homepage Settings States ---
  const [settingsLoading, setSettingsLoading] = useState(true);
  const [homepageSettings, setHomepageSettings] = useState({
    showSpotlight: true,
    showNewInLibrary: true,
    showReadingLists: true,
    showUrgentLoans: true,
  });

  useEffect(() => {
    setGreeting(GREETINGS[Math.floor(Math.random() * GREETINGS.length)]);
  }, []);

  const [urgentLoans, setUrgentLoans] = useState<UrgentLoan[]>([]);
  const [personalLists, setPersonalLists] = useState<ReadingListOverview[]>([]);
  const [listsLoading, setListsLoading] = useState(true);
  const [listsError, setListsError] = useState<string | null>(null);

  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const activeReadingLevel =
    selectedReadingLevel ?? getDefaultReadingLevelForUser(user);

  // --- Gebruikersnaam Ophalen ---
  useEffect(() => {
    if (authLoading || !user?.smartschoolUid) return;

    const uid = user.smartschoolUid;

    fetch(`${apiUrl}/users/display-names`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ uids: [uid] }),
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data?.success && data.displayNames?.[uid]) {
          const fullName: string = data.displayNames[uid];
          setFirstName(fullName.split(" ")[0]);
        }
      })
      .catch(() => {});
  }, [authLoading, user, apiUrl]);

  // --- Homepage Settings Ophalen ---
  useEffect(() => {
    if (authLoading) return;

    if (!user) {
      setSettingsLoading(false);
      return;
    }

    fetch(`${apiUrl}/schools/my-homepage-settings`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data) {
          setHomepageSettings({
            showSpotlight: data.showSpotlight ?? true,
            showNewInLibrary: data.showNewInLibrary ?? true,
            showReadingLists: data.showReadingLists ?? true,
            showUrgentLoans: data.showUrgentLoans ?? true,
          });
        }
      })
      .catch(() => {})
      .finally(() => {
        setSettingsLoading(false);
      });
  }, [apiUrl, authLoading, user]);

  // --- Fallback als de actieve tab onzichtbaar wordt ---
  useEffect(() => {
    if (settingsLoading) return;

    const canShowUrgent =
      homepageSettings.showUrgentLoans && urgentLoans.length > 0;
    const canShowSpotlight = homepageSettings.showSpotlight;
    const canShowNew = homepageSettings.showNewInLibrary;

    if (selected === "urgent" && !canShowUrgent) {
      if (canShowSpotlight) setSelected("spotlight");
      else if (canShowNew) setSelected("new");
      else setSelected("none");
    } else if (selected === "spotlight" && !canShowSpotlight) {
      if (canShowNew) setSelected("new");
      else if (canShowUrgent) setSelected("urgent");
      else setSelected("none");
    } else if (selected === "new" && !canShowNew) {
      if (canShowSpotlight) setSelected("spotlight");
      else if (canShowUrgent) setSelected("urgent");
      else setSelected("none");
    }
  }, [homepageSettings, selected, urgentLoans.length, settingsLoading]);

  const cls = (id: TabId) =>
    `tabBtn ${selected === id ? "selectedCategory" : ""}`;

  // --- Boeken Ophalen ---
  useEffect(() => {
    if (authLoading || !user || settingsLoading) return;

    if (selected === "urgent" || selected === "none") {
      setBooksLoading(false);
      return;
    }

    let cancelled = false;

    const fetchBooks = async () => {
      setBooksLoading(true);

      try {
        const endpoint =
          selected === "spotlight"
            ? `/books/spotlight?readingLevel=${encodeURIComponent(
                activeReadingLevel,
              )}`
            : `/books/latest?readingLevel=${encodeURIComponent(
                activeReadingLevel,
              )}`;

        const response = await fetch(`${apiUrl}${endpoint}`, {
          credentials: "include",
        });

        if (!response.ok) {
          throw new Error("Kon boeken niet laden.");
        }

        const data: Book[] = await response.json();

        if (cancelled) return;

        if (selected === "spotlight") {
          if (data.length > 0) {
            setBooks(data);
            return;
          }

          const fallbackResponse = await fetch(
            `${apiUrl}/books/top-rated?readingLevel=${encodeURIComponent(
              activeReadingLevel,
            )}`,
            { credentials: "include" },
          );

          if (!fallbackResponse.ok) {
            throw new Error("Kon aanbevolen boeken niet laden.");
          }

          const fallbackData: Book[] = await fallbackResponse.json();

          if (!cancelled) {
            setBooks(fallbackData);
          }

          return;
        }

        setBooks(data);
      } catch (error) {
        console.error("Fout bij laden boeken:", error);

        if (!cancelled) {
          setBooks([]);
        }
      } finally {
        if (!cancelled) {
          setBooksLoading(false);
        }
      }
    };

    fetchBooks();

    return () => {
      cancelled = true;
    };
  }, [
    selected,
    activeReadingLevel,
    apiUrl,
    authLoading,
    user,
    settingsLoading,
  ]);

  // --- Persoonlijke leeslijsten ophalen ---
  const fetchPersonalLists = useCallback(() => {
    setListsLoading(true);
    setListsError(null);

    fetch(`${apiUrl}/reading-lists`, { credentials: "include" })
      .then((res) => {
        if (!res.ok) throw new Error("Kon leeslijsten niet laden.");
        return res.json();
      })
      .then((data: ReadingListOverview[]) => {
        const ownPersonalLists = (Array.isArray(data) ? data : []).filter(
          (list) => list.listType === "PERSONAL" && list.ownList,
        );
        setPersonalLists(ownPersonalLists);
      })
      .catch((err) => {
        console.error(err);
        setListsError("Je leeslijsten konden niet geladen worden.");
      })
      .finally(() => setListsLoading(false));
  }, [apiUrl]);

  useEffect(() => {
    if (authLoading || settingsLoading) return;

    if (!user) {
      setPersonalLists([]);
      setListsLoading(false);
      return;
    }

    if (homepageSettings.showReadingLists) {
      fetchPersonalLists();
    } else {
      setListsLoading(false);
    }
  }, [
    authLoading,
    settingsLoading,
    user,
    homepageSettings.showReadingLists,
    fetchPersonalLists,
  ]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!search.trim()) return;
    router.push(`/catalog?search=${search}`);
  };

  // --- Dringende Leningen Ophalen ---
  useEffect(() => {
    if (authLoading || !user || settingsLoading) return;

    Promise.all([
      fetch(`${apiUrl}/loans/reminder-days`, { credentials: "include" })
        .then((res) => (res.ok ? res.json() : 5))
        .catch(() => 5),
      fetch(`${apiUrl}/loans/active`, { credentials: "include" })
        .then((res) => (res.ok ? res.json() : []))
        .catch(() => []),
    ]).then(
      ([reminderDays, loans]: [
        number,
        Array<{
          loanId: number;
          dueDate: string;
          book: {
            id: number;
            title: string;
            thumbnail: string | null;
            isbn: string;
            authors: string[];
          } | null;
        }>,
      ]) => {
        const urgent = loans
          .map((l) => ({
            loanId: l.loanId,
            book: l.book,
            daysLeft: calcDaysLeft(l.dueDate),
          }))
          .filter((l) => l.daysLeft <= reminderDays)
          .sort((a, b) => a.daysLeft - b.daysLeft);

        setUrgentLoans(urgent);

        if (urgent.length > 0 && homepageSettings.showUrgentLoans) {
          setSelected((prev) => (prev !== "none" ? "urgent" : "urgent"));
        }
      },
    );
  }, [
    authLoading,
    user,
    apiUrl,
    settingsLoading,
    homepageSettings.showUrgentLoans,
  ]);

  return (
    <div className="page-container">
      <main>
        <div id="top-content">
          <div id="greeting">
            {firstName ? (
              <>
                <h1 className="greetingName">Hey {firstName}!</h1>
                <h2 className="greetingText">{greeting}</h2>
              </>
            ) : (
              <h1 className="greetingText">{greeting}</h1>
            )}
          </div>

          <div id="searchBox">
            <form className="searchbar" onSubmit={handleSearch}>
              <input
                type="text"
                placeholder="🔎︎  Zoek op titel, auteur, genre, onderwerp"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <button type="submit" className="semitransparentButton">
                Bekijk catalogus →
              </button>
            </form>
          </div>
        </div>
        <a
          href="https://www.jeugdbibliotheek.nl/12-18-jaar/lezen-voor-de-lijst/niveautest.html"
          target="_blank"
          rel="noopener noreferrer"
          className="readingTestBanner"
        >
          <span className="readingTestText">
            Weet je nog niet op welk leesniveau je zit?
            <strong> Doe hier de gratis niveautest →</strong>
          </span>
        </a>

        {/* Als "selected === none", halen we de grid formatting weg zodat de leeslijst full width wordt! */}
        <div
          className="main-content-grid"
          style={selected === "none" ? { display: "block" } : undefined}
        >
          {settingsLoading ? (
            <div
              id="dashboard"
              style={{
                gridColumn: "1 / -1",
                padding: "3rem",
                textAlign: "center",
              }}
            >
              <p className="dashboardBookMessage">Dashboard laden...</p>
            </div>
          ) : (
            <>
              {/* Als er tabbladen in te laden zijn, tonen we het standaard dashboard. */}
              {selected !== "none" ? (
                <div id="dashboard">
                  <nav className="tabs-nav">
                    {homepageSettings.showUrgentLoans &&
                      urgentLoans.length > 0 && (
                        <button
                          className={cls("urgent")}
                          onClick={() => setSelected("urgent")}
                        >
                          Terug te brengen
                        </button>
                      )}
                    {homepageSettings.showSpotlight && (
                      <button
                        className={cls("spotlight")}
                        onClick={() => setSelected("spotlight")}
                      >
                        In de kijker
                      </button>
                    )}
                    {homepageSettings.showNewInLibrary && (
                      <button
                        className={cls("new")}
                        onClick={() => setSelected("new")}
                      >
                        Nieuw in bibliotheek
                      </button>
                    )}

                    {selected !== "urgent" &&
                      (homepageSettings.showSpotlight ||
                        homepageSettings.showNewInLibrary) && (
                        <label className="spotlightLevelFilter">
                          <span>Leesniveau</span>
                          <select
                            value={activeReadingLevel}
                            onChange={(e) =>
                              setSelectedReadingLevel(
                                e.target.value as ReadingLevel,
                              )
                            }
                            aria-label={
                              selected === "spotlight"
                                ? "Kies leesniveau voor In de kijker"
                                : "Kies leesniveau voor Nieuw in bibliotheek"
                            }
                          >
                            {READING_LEVELS.map((level) => (
                              <option key={level} value={level}>
                                Leesniveau {level}
                              </option>
                            ))}
                          </select>
                        </label>
                      )}
                  </nav>

                  {selected === "urgent" ? (
                    <div id="bookListDashboard">
                      {urgentLoans.map((loan) => (
                        <div key={loan.loanId} className="urgent-book-wrapper">
                          <span
                            className={`urgent-overlay-badge ${badgeClass(loan.daysLeft)}`}
                          >
                            {formatDaysLabel(loan.daysLeft)}
                          </span>
                          <BookCard
                            book={{
                              id: loan.book?.id ?? 0,
                              title: loan.book?.title ?? "Onbekend boek",
                              thumbnail: loan.book?.thumbnail ?? "",
                              authors: loan.book?.authors ?? [],
                              isbn: loan.book?.isbn ?? "",
                              publisher: "",
                              description: "",
                              pageCount: 0,
                              categories: [],
                              language: "",
                              rating: 0,
                              publishedYear: 0,
                              spotlight: false,
                              didacticTag: false,
                              readingLevel: "",
                              labels: [],
                              ageRange: "",
                            }}
                            isSelected={false}
                            onToggle={() => {}}
                            withCheckbox={false}
                          />
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div id="bookListDashboard">
                      {booksLoading ? (
                        <p className="dashboardBookMessage">
                          Laden van boeken...
                        </p>
                      ) : books.length > 0 ? (
                        books.map((book) => (
                          <BookCard
                            key={book.id}
                            book={book}
                            isSelected={false}
                            onToggle={() => {}}
                            withCheckbox={false}
                          />
                        ))
                      ) : (
                        <p className="dashboardBookMessage">
                          {selected === "spotlight"
                            ? `Geen boeken in de kijker voor leesniveau ${activeReadingLevel}.`
                            : `Geen nieuwe boeken gevonden voor leesniveau ${activeReadingLevel}.`}
                        </p>
                      )}
                    </div>
                  )}
                </div>
              ) : // Als ER GEEN tabbladen zijn EN ook GEEN leeslijsten, toon de algemene melding
              !homepageSettings.showReadingLists ? (
                <div id="dashboard" style={{ width: "100%" }}>
                  <div id="bookListDashboard">
                    <p className="dashboardBookMessage">
                      Er is momenteel geen weergave geconfigureerd voor de
                      hoofdpagina. Gebruik de zoekbalk of het menu om boeken te
                      ontdekken.
                    </p>
                  </div>
                </div>
              ) : null}

              {/* Leeslijsten Zijbalk */}
              {homepageSettings.showReadingLists && (
                <aside
                  className="home-sidebar"
                  style={
                    selected === "none"
                      ? { maxWidth: "100%", width: "100%" }
                      : undefined
                  }
                >
                  <HomeReadingLists
                    lists={personalLists}
                    loading={listsLoading}
                    error={listsError}
                    onOpenList={(id) => router.push(`/reading-lists/${id}`)}
                    onOpenPersonal={() =>
                      router.push("/reading-lists/personal")
                    }
                    onOpenAll={() => router.push("/reading-lists")}
                    onRetry={fetchPersonalLists}
                  />
                </aside>
              )}
            </>
          )}
        </div>
      </main>
    </div>
  );
}
