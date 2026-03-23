"use client";

import { useEffect, useState } from "react";
import { Book } from "./interfaces/Book";
import BookCard from "./catalog/bookCard";
import "./dashboard.css";
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
    .then((data: Book[]) => {
      if (data.length > 0) {
        setBooks(data);
      } else {
        return fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/rating/highest`)
          .then((res) => res.json())
          .then((fallbackData: Book[]) => setBooks(fallbackData));
      }
    })
    .catch((error) => console.error(error));
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
          <button
            className="semitransparentButton"
            onClick={() =>
              router.push(
                search.trim() ? `/catalog?search=${search}` : "/catalog",
              )
            }
          >
            Bekijk Catalogus →
          </button>
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
