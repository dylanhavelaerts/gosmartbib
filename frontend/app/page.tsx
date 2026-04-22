"use client";

import { useCallback, useEffect, useState } from "react";
import HomeReadingLists from "./components/home/HomeReadingLists";
import { ReadingListOverview } from "./interfaces/ReadingList";
import { Book } from "./interfaces/Book";
import BookCard from "./catalog/bookCard";
import "./dashboard.css";
import { useRouter } from "next/navigation";
import { useAuth } from "./context/AuthContext";

type TabId = "spotlight" | "new";

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const [books, setBooks] = useState<Book[]>([]);
  const [search, setSearch] = useState("");
  const router = useRouter();
  const [personalLists, setPersonalLists] = useState<ReadingListOverview[]>([]);
  const [listsLoading, setListsLoading] = useState(true);
  const [listsError, setListsError] = useState<string | null>(null);

  const { user, loading: authLoading } = useAuth();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

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
          return fetch(`${apiUrl}/books/top-rated`, {
            credentials: "include",
          })
            .then((res) => res.json())
            .then((fallbackData: Book[]) => setBooks(fallbackData));
        }
      })
      .catch((error) => console.error(error));
  }, [selected, apiUrl, authLoading, user]);
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
      setListsError(null);
      setListsLoading(false);
      return;
    }

    fetchPersonalLists();
  }, [authLoading, user, fetchPersonalLists]);

  const handleSearch = (e: React.SubmitEvent) => {
    e.preventDefault();

    if (!search.trim()) return;

    router.push(`/catalog?search=${search}`);
  };

  return (
    <>
      <main>
        <div id="searchBox">
          <form className="searchbar" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Titel, auteur, genre, onderwerp"
              value={search}
              onChange={(text) => setSearch(text.target.value)}
            />
            <button id="searchButton" type="submit">
              🔎︎
            </button>
          </form>
          <button
            className="semitransparentButton"
            onClick={() =>
              router.push(
                search.trim() ? `/catalog?search=${search}` : "/catalog",
              )
            }
          >
            Bekijk catalogus →
          </button>
        </div>
        <div id="dashboard">
          <nav>
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
            {books.map((book) => (
              <BookCard
                key={book.id}
                book={book}
                isSelected={false}
                onToggle={() => {}}
                withCheckbox={false}
              />
            ))}
          </div>
        </div>
        <HomeReadingLists
          lists={personalLists}
          loading={listsLoading}
          error={listsError}
          onOpenList={(id) => router.push(`/reading-lists/${id}`)}
          onOpenPersonal={() => router.push("/reading-lists/personal")}
          onOpenAll={() => router.push("/reading-lists")}
          onRetry={fetchPersonalLists}
        />
      </main>
    </>
  );
}
