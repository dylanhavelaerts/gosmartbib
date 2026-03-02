"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";

export default function Home() {
  const [books, setBooks] = useState<Book[]>([]);

  useEffect(() => {
    fetch("http://localhost:8080/catalog/all")
      .then((res) => res.json())
      .then((data: Book[]) => setBooks(data));
  }, []);
}
