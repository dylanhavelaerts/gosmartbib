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
