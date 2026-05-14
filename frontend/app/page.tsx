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

type TabId = "spotlight" | "new";
type ReadingLevel = "A" | "B" | "C" | "D";

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

function getDefaultReadingLevelForUser(user: {
  role?: string;
  classes?: {
    name?: string | null;
  }[];
} | null): ReadingLevel {
  if (user?.role !== "STUDENT") {
    return "A";
  }

  const className = user.classes?.[0]?.name;

  return getDefaultReadingLevelFromClassName(className);
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

  useEffect(() => {
    setGreeting(GREETINGS[Math.floor(Math.random() * GREETINGS.length)]);
  }, []);

  const [personalLists, setPersonalLists] = useState<ReadingListOverview[]>([]);
  const [listsLoading, setListsLoading] = useState(true);
  const [listsError, setListsError] = useState<string | null>(null);

  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const activeReadingLevel =
  selectedReadingLevel ?? getDefaultReadingLevelForUser(user);

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

  const cls = (id: TabId) =>
    `tabBtn ${selected === id ? "selectedCategory" : ""}`;

useEffect(() => {
  if (authLoading || !user) return;

  let cancelled = false;

  const fetchBooks = async () => {
    setBooksLoading(true);

    try {
      const endpoint =
        selected === "spotlight"
          ? `/books/spotlight?readingLevel=${encodeURIComponent(
              activeReadingLevel,
            )}`
          : "/books/latest";

      const response = await fetch(`${apiUrl}${endpoint}`, {
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error("Kon boeken niet laden.");
      }

      const data: Book[] = await response.json();

      if (cancelled) return;

      if (selected === "spotlight") {
        setBooks(data);
        return;
      }

      if (data.length > 0) {
        setBooks(data);
        return;
      }

      // Alleen voor "Nieuw in bibliotheek":
      // fallback naar top-rated als de nieuwste boeken leeg zijn.
      const fallbackResponse = await fetch(`${apiUrl}/books/top-rated`, {
        credentials: "include",
      });

      if (!fallbackResponse.ok) {
        throw new Error("Kon aanbevolen boeken niet laden.");
      }

      const fallbackData: Book[] = await fallbackResponse.json();

      if (!cancelled) {
        setBooks(fallbackData);
      }
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
}, [selected, activeReadingLevel, apiUrl, authLoading, user]);

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
    if (authLoading) return;

    if (!user) {
      setPersonalLists([]);
      setListsLoading(false);
      return;
    }

    fetchPersonalLists();
  }, [authLoading, user, fetchPersonalLists]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (!search.trim()) return;
    router.push(`/catalog?search=${search}`);
  };

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

        <div className="main-content-grid">
          <div id="dashboard">
            <nav className="tabs-nav">
              <button
                className={cls("spotlight")}
                onClick={() => setSelected("spotlight")}
              >
                In de kijker
              </button>
              <button className={cls("new")} onClick={() => setSelected("new")}>
                Nieuw in bibliotheek
              </button>
               {selected === "spotlight" && (
                <label className="spotlightLevelFilter">
                  <span>Leesniveau</span>
                  <select
                    value={activeReadingLevel}
                    onChange={(e) =>
                      setSelectedReadingLevel(e.target.value as ReadingLevel)
                    }
                    aria-label="Kies leesniveau voor In de kijker"
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

            <div id="bookListDashboard">
              {booksLoading ? (
                <p className="dashboardBookMessage">Laden van boeken...</p>
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
                    : "Geen boeken gevonden."}
                </p>
              )}
            </div>
          </div>

          <aside className="home-sidebar">
            <HomeReadingLists
              lists={personalLists}
              loading={listsLoading}
              error={listsError}
              onOpenList={(id) => router.push(`/reading-lists/${id}`)}
              onOpenPersonal={() => router.push("/reading-lists/personal")}
              onOpenAll={() => router.push("/reading-lists")}
              onRetry={fetchPersonalLists}
            />
          </aside>
        </div>
      </main>
    </div>
  );
}
