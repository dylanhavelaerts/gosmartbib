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
  spotlight: boolean;
}

export const BOOK_CATEGORIES = [
  "Fictie algemeen",
  "Literaire roman",
  "Spanning / thriller",
  "Detective / misdaad",
  "Fantasy",
  "Science fiction",
  "Dystopie",
  "Historische roman",
  "Romantiek",
  "Coming-of-age",
  "Avontuur",
  "Oorlog & conflict",
  "Horror",
  "Humor",
  "Graphic Novel / strip",
  "Poëzie",
  "Non-fictie algemeen",
];
