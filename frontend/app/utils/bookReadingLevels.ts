import type { Book } from "../interfaces/Book";

export type ReadingLevelSection = {
  readingLevel: string;
  books: Book[];
};

const LEVEL_ORDER = ["A", "B", "C", "D"];
export const UNKNOWN_LEVEL_LABEL = "Geen leesniveau";

export function normalizeReadingLevel(readingLevel?: string | null) {
  const trimmed = readingLevel?.trim();
  return trimmed ? trimmed.toUpperCase() : UNKNOWN_LEVEL_LABEL;
}

function getReadingLevelSortValue(readingLevel: string) {
  const index = LEVEL_ORDER.indexOf(readingLevel);
  return index === -1 ? LEVEL_ORDER.length : index;
}

export function formatReadingLevelTitle(readingLevel: string) {
  if (readingLevel === UNKNOWN_LEVEL_LABEL) return UNKNOWN_LEVEL_LABEL;
  return `Leesniveau ${readingLevel}`;
}

/**
 * Groepeert boeken op basis van hun leesniveau
 * Doet dit door de boeken te normaliseren op hun leesniveau, ze te groeperen in een object,
 * en vervolgens de groepen te sorteren op leesniveau en alfabetisch binnen elk niveau
 * @param books - de lijst van boeken
 * @return de gegroepeerde boeken per leesniveau
 */
export function groupBooksByReadingLevel(books: Book[]): ReadingLevelSection[] {
  const groupedBooks = books.reduce<Record<string, Book[]>>((groups, book) => {
    const readingLevel = normalizeReadingLevel(book.readingLevel);

    if (!groups[readingLevel]) {
      groups[readingLevel] = [];
    }

    groups[readingLevel].push(book);
    return groups;
  }, {});

  return Object.entries(groupedBooks)
    .sort(([levelA], [levelB]) => {
      const levelOrderDifference =
        getReadingLevelSortValue(levelA) - getReadingLevelSortValue(levelB);

      if (levelOrderDifference !== 0) return levelOrderDifference;
      return levelA.localeCompare(levelB, "nl");
    })
    .map(([readingLevel, groupedBooksForLevel]) => ({
      readingLevel,
      books: groupedBooksForLevel,
    }));
}
