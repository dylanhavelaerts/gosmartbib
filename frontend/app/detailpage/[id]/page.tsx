"use client";

import { use, useEffect, useState } from "react";
import { Book } from "../../interfaces/Book";
import Link from "next/link";
import "./detailpage.css";

export default function DetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const [book, setBook] = useState<Book | null>(null);

  useEffect(() => {
    if (!id) return;
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/book/${id}`)
      .then((res) => res.json())
      .then((data: Book) => setBook(data));
  }, [id]);

  if (!book) return <p>Loading...</p>;

  return (
    <main className="detailPage">
      <Link href="/catalog" className="backLink">
        ← Terug naar catalogus
      </Link>

      <div className="detailContainer">
        {/* linker kolom: cover */}
        <div className="detailLeft">
          <img src={book.thumbnail} alt={book.title} className="detailCover" />
          <div className="detailUnder">
            <p>
              <img className="bookIcon" src={"/book-alt.png"} />{" "}
              {book.pageCount} pagina's
            </p>
            <p>
              <img className="bookIcon" src={"/book-closed.png"} />{" "}
              {book.language.toUpperCase()}
            </p>
          </div>
        </div>

        {/* rechter kolom: info */}
        <div className="detailRight">
          <h1 className="detailTitle">{book.title}</h1>
          <p className="detailAuthors">door {book.authors?.join(", ")}</p>

          <div className="detailBadges">
            {book.categories?.map((cat) => (
              <span key={cat} className="badge">
                {cat}
              </span>
            ))}
          </div>
          <div className="detailDescription">
            <h2>Waar gaat het over?</h2>
            <p>{book.description}</p>
          </div>
          <div className="detailInfoBoxes">
            <div className="infoBox infoBoxUitgever">
              <span className="infoBoxLabel">Uitgever</span>
              <span className="infoBoxValue">{book.publisher}</span>
            </div>
            <div className="infoBox infoBoxJaar">
              <span className="infoBoxLabel">Jaar</span>
              <span className="infoBoxValue">{book.publishedYear}</span>
            </div>
          </div>
          <div className="infoBox infoBoxFull">
            <span className="infoBoxLabel">ISBN</span>
            <span className="infoBoxValue">{book.isbn}</span>
          </div>
        </div>
      </div>
    </main>
  );
}
