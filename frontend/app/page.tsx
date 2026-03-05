"use client";

import { useEffect, useState } from "react";
import { Book } from "./interfaces/Book";
import BookCard from "./catalog/bookCard";
import "./dashboard.css"
import Link from "next/link";

type TabId = "spotlight" | "new";

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const cls = (id: TabId) => `tabBtn ${selected === id ? "selectedCategory" : ""}`;
  const [books, setBooks] = useState<Book[]>([]);

  useEffect(() => {
    const endpoint = selected === "spotlight" ? "/api/books/spotlight" : "/api/books/latest";
    fetch(endpoint)
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, [selected]);

  return (
    <>
      <main>
        <div id="searchBox">
          <div className="searchbar">
            <input type="text" placeholder="Titel, auteur, genre, onderwerp" />
            <button id="searchButton">🔎︎</button>
          </div>
          <Link href="/catalog">
          <button className="semitransparentButton">Bekijk Catalogus →</button>
          </Link>
        </div>
        <div id="dashboard">
          <nav>
            <button className={cls("spotlight")} onClick={() => setSelected("spotlight")}>in de kijker</button>
            <button className={cls("new")} onClick={() => setSelected("new")}>Nieuw in bibliotheek</button>
          </nav>
          <div id="bookList">
                  {books.map((book) => (
                    <BookCard key={book.id} book={book} />
                  ))}
          </div>
        </div>
      </main></>
  )
}
