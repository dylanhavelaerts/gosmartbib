export interface Book {
  id: number;
  title: string;
  authors: string[];
  publisher: string;
  description: string;
  pageCount: number;
  categories: string[];
  thumbnail: string;
  language: string;
  rating: number;
  isbn: string;
  publishedYear: number;
  totalCopies?: number;
  availableCopies?: number;
  spotlight: boolean;
}

export const BOOK_CATEGORIES = [
  "Avontuur",
  "Biography",
  "Fantasy",
  "Fiction",
  "Fictie",
  "Historische fictie",
  "History",
  "Horror",
  "Mysterie",
  "Non-Fiction",
  "Non-fictie",
  "Programming",
  "Romantiek",
  "Science Fiction",
  "Thriller",
];
