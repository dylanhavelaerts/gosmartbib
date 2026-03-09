"use client";

import { useEffect, useState } from "react";
import { Book } from "./interfaces/Book";
import BookCard from "./catalog/bookCard";
import "./dashboard.css";
import Link from "next/link";
import { useRouter } from "next/navigation";

type TabId = "spotlight" | "new";

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const [books, setBooks] = useState<Book[]>([]);
  const [search, setSearch] = useState("");
  const router = useRouter();

  const cls = (id: TabId) =>
    `tabBtn ${selected === id ? "selectedCategory" : ""}`;

  useEffect(() => {
    const endpoint =
      selected === "spotlight" ? "/books/spotlight" : "/books/latest";
    fetch(`${process.env.NEXT_PUBLIC_API_URL}${endpoint}`)
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, [selected]);

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
          <Link href="/catalog">
            <button className="semitransparentButton">
              Bekijk Catalogus →
            </button>
          </Link>
        </div>
        <div id="dashboard">
          <nav>
            <button
              className={cls("spotlight")}
              onClick={() => setSelected("spotlight")}
            >
              in de kijker
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
      </main>
    </>
  );
}
