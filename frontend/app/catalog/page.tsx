"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";
import BookCard from "./bookCard";
import "./bookList.css";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);
  const [activeTab, setActiveTab] = useState("Catalogus");

  useEffect(() => {
    fetch("http://localhost:8080/catalog/all")
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, []);

  return (
    <main>
      <h1>Catalog</h1>
      <ul>
        <li
          className={activeTab === "Catalogus" ? "active" : ""}
          onClick={() => setActiveTab("Catalogus")}
        >
          Catalogus
        </li>
        <li
          className={activeTab === "Boek toevoegen" ? "active" : ""}
          onClick={() => setActiveTab("Boek toevoegen")}
        >
          Boek toevoegen
        </li>
      </ul>
      <div id="bookList">
        {books.map((book) => (
          <BookCard key={book.id} book={book} />
        ))}
      </div>
    </main>
  );
}
