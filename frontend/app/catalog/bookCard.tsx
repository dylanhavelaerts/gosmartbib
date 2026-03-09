import { Book } from "../interfaces/Book";
import Link from "next/link";
import "./bookCard.css";

interface Props {
  book: Book;
  isSelected: boolean;
  onToggle: () => void;
  withCheckbox?: Boolean;
}

export default function BookCard({ book, isSelected, onToggle, withCheckbox = true }: Props) {
  return (
    <div className={`bookCard ${isSelected ? "selected" : ""}`}>
      {withCheckbox && (
        <input
          type="checkbox"
          checked={isSelected}
          onChange={onToggle}
          className="bookCheckBox"
        />
      )}
      <Link href={`/detailpage/${book.id}`}>
        <div className="bookCover">
          <img src={book.thumbnail} alt={book.title} />
        </div>
      </Link>

      <h2 className="bookTitle">{book.title}</h2>
    </div>
  );
}
