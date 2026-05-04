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

const GREETINGS = [
  "Klaar om te lezen?",
  "Veel leesplezier!",
  "Wat ga je vandaag lezen?",
  "Duik in een goed boek!",
  "Ontdek je volgende favoriete boek!",
  "Tijd voor een nieuw avontuur tussen de pagina's!",
];

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const [books, setBooks] = useState<Book[]>([]);
  const [search, setSearch] = useState("");
  const [greeting, setGreeting] = useState(GREETINGS[0]);
  const [firstName, setFirstName] = useState<string | null>(null);

  useEffect(() => {
    setGreeting(GREETINGS[Math.floor(Math.random() * GREETINGS.length)]);
  }, []);

  const [personalLists, setPersonalLists] = useState<ReadingListOverview[]>([]);
  const [listsLoading, setListsLoading] = useState(true);
  const [listsError, setListsError] = useState<string | null>(null);

  const router = useRouter();
  const { user, loading: authLoading } = useAuth();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  useEffect(() => {
    if (authLoading || !user?.smartschoolUid) return;

    fetch(`${apiUrl}/users/display-names`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ uids: [user.smartschoolUid] }),
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data?.success && data.displayNames?.[user.smartschoolUid]) {
          const fullName: string = data.displayNames[user.smartschoolUid];
          setFirstName(fullName.split(" ")[0]);
        }
      })
      .catch(() => {});
  }, [authLoading, user, apiUrl]);

  const cls = (id: TabId) =>
    `tabBtn ${selected === id ? "selectedCategory" : ""}`;

  useEffect(() => {
    if (authLoading || !user) return;

    const endpoint =
      selected === "spotlight" ? "/books/spotlight" : "/books/latest";

    fetch(`${apiUrl}${endpoint}`, {
      credentials: "include",
    })
      .then((res) => res.json())
      .then((data: Book[]) => {
        if (data.length > 0) {
          setBooks(data);
        } else {
          // Fallback naar top-rated boeken als de lijst leeg is
          return fetch(`${apiUrl}/books/top-rated`, {
            credentials: "include",
          })
            .then((res) => res.json())
            .then((fallbackData: Book[]) => setBooks(fallbackData));
        }
      })
      .catch((error) => console.error("Fout bij laden boeken:", error));
  }, [selected, apiUrl, authLoading, user]);

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
            </nav>

            <div id="bookListDashboard">
              {books.length > 0 ? (
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
                <p>Laden van boeken...</p>
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
