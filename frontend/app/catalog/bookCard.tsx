import { Book } from "../interfaces/Book";
import "./bookCard.css";

interface Props {
  book: Book;
}

export default function BookCard({ book }: Props) {
  return (
    <div id="bookCard">
      <div id="bookCover">
        <img src={book.thumbnail} alt={book.title} />
      </div>
      <h2 id="bookTitle">{book.title}</h2>
    </div>
  );
}
