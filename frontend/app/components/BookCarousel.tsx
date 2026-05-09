"use client";
import { useRef } from "react";
import { SnowballSection } from "@/app/interfaces/Book";
import BookCard from "@/app/catalog/bookCard";
import "./BookCarousel.css";

export default function BookCarousel({
  section,
}: {
  section: SnowballSection;
}) {
  const trackRef = useRef<HTMLDivElement>(null);

  const scroll = (dir: "left" | "right") => {
    if (!trackRef.current) return;
    const amount = trackRef.current.clientWidth * 0.75;
    trackRef.current.scrollBy({
      left: dir === "right" ? amount : -amount,
      behavior: "smooth",
    });
  };

  const title =
    section.type === "AUTHOR"
      ? `Meer van ${section.value}`
      : `Meer ${section.value}`;

  return (
    <section className="snowball-section">
      <h2 className="snowball-title">{title}</h2>
      <div className="snowball-wrapper">
        <button
          className="snowball-btn snowball-btn--left"
          onClick={() => scroll("left")}
          aria-label="Vorige"
        >
          ‹
        </button>
        <div className="snowball-track" ref={trackRef}>
          {section.books.map((book) => (
            <div key={book.id} className="snowball-card">
              <BookCard
                book={book}
                isSelected={false}
                onToggle={() => {}}
                withCheckbox={false}
              />
            </div>
          ))}
        </div>
        <button
          className="snowball-btn snowball-btn--right"
          onClick={() => scroll("right")}
          aria-label="Volgende"
        >
          ›
        </button>
      </div>
    </section>
  );
}
