"use client";

import { useState } from "react";
import "./pagination.css";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * Component voor paginering van zoekresultaten.
 * Toont knoppen voor navigatie tussen pagina's, inclusief "Eerste", "Laatste" en "..." voor snelle navigatie.
 */
export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}: PaginationProps) {
  // State voor het bijhouden van de "..." input velden
  const [jumpInput, setJumpInput] = useState<"before" | "after" | null>(null);
  const [jumpValue, setJumpValue] = useState("");

  if (totalPages <= 0) return null;

  // Bereken welke pagina's zichtbaar moeten zijn (±1 rond huidige pagina)
  const visiblePages: number[] = [];
  const start = Math.max(1, currentPage - 1);
  const end = Math.min(totalPages, currentPage + 1);
  for (let i = start; i <= end; i++) {
    visiblePages.push(i);
  }

  const showPrev = currentPage > 1;
  const showNext = currentPage < totalPages;
  const showDotsAfter = end < totalPages;

  // Handler voor het springen naar een specifieke pagina via de "..." input
  const handleJump = () => {
    const page = parseInt(jumpValue);
    if (page >= 1 && page <= totalPages) {
      onPageChange(page);
    }
    setJumpInput(null);
    setJumpValue("");
  };

  return (
    <div className="pagination">
      {showPrev && (
        <button className="paginationBtn paginationArrow" onClick={() => onPageChange(currentPage - 1)} aria-label="Vorige pagina">
          <img src="/back-no-stripe.png" alt="" className="paginationArrowIcon" />
        </button>
      )}

      {visiblePages.map((page) => (
        <button
          key={page}
          className={`paginationBtn paginationNumber ${page === currentPage ? "active" : ""}`}
          onClick={() => onPageChange(page)}
        >
          {page}
        </button>
      ))}

      {showDotsAfter &&
        (jumpInput === "after" ? (
          <input
            className="paginationJumpInput"
            type="number"
            min={1}
            max={totalPages}
            value={jumpValue}
            onChange={(e) => setJumpValue(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleJump()}
            onBlur={handleJump}
            autoFocus
          />
        ) : (
          <button
            className="paginationBtn paginationDots"
            onClick={() => {
              setJumpInput("after");
              setJumpValue("");
            }}
          >
            ...
          </button>
        ))}

      {showNext && (
        <button
          className="paginationBtn paginationArrow"
          onClick={() => onPageChange(currentPage + 1)}
          aria-label="Volgende pagina"
        >
          <img src="/back-no-stripe.png" alt="" className="paginationArrowIcon paginationArrowFlipped" />
        </button>
      )}
    </div>
  );
}
