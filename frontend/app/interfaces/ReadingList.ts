export type ListType = "CLASS" | "PERSONAL";

export type ReadingListTargetType = "STUDENTS" | "CLASSES" | "YEARS" | "GRADES";

export interface ReadingListOverview {
  id: number;
  publicUid?: string | null;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  creatorName?: string | null;
  bookCount?: number;

  /**
   * Older frontend code used bookIds.
   * The newer backend overview mainly uses bookCount.
   * Keeping this optional prevents existing frontend code from breaking.
   */
  bookIds?: number[];

  listType: ListType;
  ownList: boolean;
  publicVisible?: boolean;

  targetType?: ReadingListTargetType | null;

  /**
   * These are live-resolved names from the backend.
   * They are only for display.
   * Do not send names back to the backend.
   */
  targetStudentDisplayNames?: string[];
  targetClassNames?: string[];

  targetYears?: number[];
  targetGrades?: number[];
  targetAllSchools?: boolean;
}

export interface ReadingListBookItem {
  id: number;
  title: string;
  authors: string[];
  thumbnail?: string | null;
  isbn: string;
  availableCopies: number;
}

export interface ReadingListDetail {
  id: number;
  publicUid?: string | null;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  listType: ListType;
  ownList: boolean;
  publicVisible?: boolean;
  creatorName?: string | null;

  targetType?: ReadingListTargetType | null;

  /**
   * IDs are only returned when the viewer is allowed to see local target details.
   * For cross-school year/grade lists, these will usually be empty.
   */
  targetStudentIds?: number[];
  targetStudentDisplayNames?: string[];
  targetStudents?: ReadingListStudentTarget[];
  targetClassIds?: number[];
  targetClassNames?: string[];

  targetYears?: number[];
  targetGrades?: number[];
  targetAllSchools?: boolean;

  books: ReadingListBookItem[];
}

export interface PublicReadingListDetail {
  publicUid: string;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  creatorRole?: string | null;
  books: ReadingListBookItem[];
}

export interface ReadingListStudentTarget {
  id: number;
  displayName: string;
  classNames: string[];
}

export interface ReadingListClassTarget {
  id: number;
  name: string;
  year?: number | null;
  grade?: number | null;
}

export interface ReadingListAssignmentTargets {
  students: ReadingListStudentTarget[];
  classes: ReadingListClassTarget[];
  years: number[];
  grades: number[];
}

export interface CreateReadingListPayload {
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  bookIds: number[];

  targetType?: ReadingListTargetType | null;
  targetStudentIds?: number[];
  targetClassIds?: number[];
  targetYears?: number[];
  targetGrades?: number[];
  targetAllSchools?: boolean;
}

export interface UpdateReadingListVisibilityPayload {
  publicVisible: boolean;
}
