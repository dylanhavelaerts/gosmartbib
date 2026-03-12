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

  // Bereken welke pagina's zichtbaar moeten zijn
  const visiblePages: number[] = [];
  const start = Math.max(1, currentPage - 2);
  const end = Math.min(totalPages, currentPage + 2);
  for (let i = start; i <= end; i++) {
    visiblePages.push(i);
  }

  // Bepaal de eerste en laatste zichtbare pagina
  const firstVisible = visiblePages[0];
  const lastVisible = visiblePages[visiblePages.length - 1];

  // Bepaal of we de "Eerste", "Laatste" knoppen en de "..." moeten tonen
  const showEerste = firstVisible > 1;
  const showDotsBefore = firstVisible > 2;
  const showDotsAfter = lastVisible < totalPages - 1;
  const showLaatste = lastVisible < totalPages;

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
      {showEerste && (
        <button className="paginationBtn" onClick={() => onPageChange(1)}>
          Eerste
        </button>
      )}

      {showDotsBefore &&
        (jumpInput === "before" ? (
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
              setJumpInput("before");
              setJumpValue("");
            }}
          >
            ...
          </button>
        ))}

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

      {showLaatste && (
        <button
          className="paginationBtn"
          onClick={() => onPageChange(totalPages)}
        >
          Laatste
        </button>
      )}
    </div>
  );
}
