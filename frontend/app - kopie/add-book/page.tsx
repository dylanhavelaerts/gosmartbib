"use client";

import { useState } from "react";
import styles from "./addBookPage.module.css";

import AddBookWithIsbn from "./components/addBookWithIsbn";
import AddBookWithoutIsbn from "./components/addBookWithoutIsbn";
import BookListImport from "./components/bookListImport";

type TabId = "Boek" | "Boek zonder ISBN" | "Boekenlijst";

export default function AddBookPage() {
  const [selected, setSelected] = useState<TabId>("Boek");

  const cls = (id: TabId) =>
    `${styles.tabBtn} ${selected === id ? styles.selectedCategory : ""}`.trim();

  return (
    <div className={styles.page}>
      <nav className={styles.lowerNav}>
        <button className={cls("Boek")} onClick={() => setSelected("Boek")}>
          Boek toevoegen
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
