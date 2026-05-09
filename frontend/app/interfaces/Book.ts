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
  didacticTag: boolean;
  readingLevel: string;
  labels: string[];
  ageRange: string;
  inventories?: BookInventory[];
}

export const AGE_RANGE = ["Eerste graad", "Tweede graad", "Derde graad"];

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

export const BOOK_LABELS = [
  "Liefde & relatie",
  "Vriendschap",
  "Identiteit & zelfbeeld",
  "Gender & seksualiteit",
  "Diversiteit & inclusie",
  "Mentale gezondheid",
  "Rouw & verlies",
  "Familie",
  "School & prestatiedruk",
  "Sociale media",
  "Migratie & afkomst",
  "Armoede & ongelijkheid",
  "Macht & onrecht",
  "Avontuur & ontdekking",
  "Overleven",
  "Toekomst & technologie",
];

export interface BookInventory {
  id?: number | null;
  schoolId: number | null;
  schoolName?: string;
  campus: string;
  totalCopies: number;
  availableCopies: number;
}

export interface SnowballSection {
  type: "AUTHOR" | "CATEGORY";
  value: string;
  books: Book[];
}
