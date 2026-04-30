"use client";

import { useState } from "react";
import "./addBookPage.css";

import AddBookWithIsbn from "./components/addBookWithIsbn";
import AddBookWithoutIsbn from "./components/addBookWithoutIsbn";
import BookListImport from "./components/bookListImport";

type TabId = "Boek" | "Boek zonder ISBN" | "Boekenlijst";

export default function AddBookPage() {
  const [selected, setSelected] = useState<TabId>("Boek");

  const cls = (id: TabId) =>
    `tabBtn ${selected === id ? "selectedCategory" : ""}`.trim();

  return (
    <div className="mainPage">
      <nav className="lowerNav">
        <button className={cls("Boek")} onClick={() => setSelected("Boek")}>
          Boek toe
        </button>
        <button
          className={cls("Boek zonder ISBN")}
          onClick={() => setSelected("Boek zonder ISBN")}
        >
          Boek zonder ISBN
        </button>
        <button
          className={cls("Boekenlijst")}
          onClick={() => setSelected("Boekenlijst")}
        >
          Boekenlijst toevoegen
        </button>
      </nav>

      {selected === "Boek" && <AddBookWithIsbn />}
      {selected === "Boek zonder ISBN" && <AddBookWithoutIsbn />}
      {selected === "Boekenlijst" && <BookListImport />}
    </div>
  );
}
