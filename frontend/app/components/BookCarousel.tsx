"use client";
import { useRef } from "react";
import Link from "next/link";
import { SnowballSection } from "@/app/interfaces/Book";
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

  return (
    <section className="snowball-section">
      <h2 className="snowball-title">{section.title}</h2>
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
            <Link
              key={book.id}
              href={`/detailpage/${book.id}`}
              className="snowball-card"
            >
              <div className="snowball-cover-wrapper">
                <img
                  src={book.thumbnail || "/No-Image-Available-Placeholder.png"}
                  alt={book.title}
                  className="snowball-cover"
                  onError={(e) => {
                    (e.target as HTMLImageElement).src =
                      "/No-Image-Available-Placeholder.png";
                  }}
                />
              </div>
              <p className="snowball-card-title">{book.title}</p>
              <p className="snowball-card-author">{book.authors?.join(", ")}</p>
            </Link>
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
