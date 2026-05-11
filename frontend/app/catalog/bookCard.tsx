import { Book } from "../interfaces/Book";
import Link from "next/link";
import "./bookCard.css";
import { useState } from "react";

interface Props {
  book: Book;
  isSelected: boolean;
  onToggle: () => void;
  withCheckbox?: boolean;
  selectionControl?: "checkbox" | "add" | "remove";
}

export default function BookCard({
  book,
  isSelected,
  onToggle,
  withCheckbox = true,
  selectionControl = "checkbox",
}: Props) {
  const [imgSrc, setImgSrc] = useState(
    book.thumbnail || "/No-Image-Available-Placeholder.png",
  );

  const renderSelectionControl = () => {
    if (!withCheckbox) return null;

    if (selectionControl === "add" || selectionControl === "remove") {
      return (
        <button
          type="button"
          className={`bookCardActionToggle ${
            isSelected ? "bookCardActionToggleActive" : ""
          }`}
          aria-label={
            selectionControl === "add"
              ? "Boek selecteren om toe te voegen aan de kijker"
              : "Boek selecteren om te verwijderen uit de kijker"
          }
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            onToggle();
          }}
        >
          {selectionControl === "add" ? "+" : "−"}
        </button>
      );
    }

    return (
      <input
        type="checkbox"
        checked={isSelected}
        onChange={onToggle}
        className="bookCheckBox"
      />
    );
  };

  return (
    <div className={`bookCard ${isSelected ? "selected" : ""}`}>
      {renderSelectionControl()}

      <Link href={`/detailpage/${book.id}`}>
        <div className="bookCover">
          <img
            src={imgSrc}
            alt={book.title}
            onError={() =>
              setImgSrc("/No-Image-Available-Placeholder.png")
            }
          />
        </div>
      </Link>

      <h2 className="bookTitle">{book.title}</h2>
    </div>
  );
}