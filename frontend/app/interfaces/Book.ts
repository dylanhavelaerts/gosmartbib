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

export const BOOK_READING_LEVELS = ["A", "B", "C", "D"];

export const BOOK_LANGUAGE_PRESETS = ["nl", "en", "fr"];

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
  damagedCopies?: number;
  brokenCopies?: number;
  lostCopies?: number;
}

export interface SnowballSection {
  type: "AUTHOR" | "CATEGORY";
  value: string;
  books: Book[];
}

export type ImportMismatch = {
  rowNumber: number;
  isbn?: string;
  excelTitle: string;
  fetchedTitle?: string | null;
  reason: string;
  amount: number | null;
};

export type DuplicateWarning = {
  rowNumber: number;
  existingBookId: number;
  title: string;
  authors: string[];
  publisher: string;
  campus: string;
  totalCopiesToAdd: number;
  availableCopiesToAdd: number;
  currentTotalCopies: number;
  currentAvailableCopies: number;
  reason: string;
};

export type BulkImportResult = {
  totalRows: number;
  savedCount: number;
  mismatchCount: number;
  mismatches: ImportMismatch[];
  duplicateWarningCount: number;
  duplicateWarnings: DuplicateWarning[];
};
