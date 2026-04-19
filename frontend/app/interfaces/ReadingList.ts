export type ListType = "CLASS" | "PERSONAL";

export interface ReadingListOverview {
  id: number;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  listType: ListType;
  ownList: boolean;
  creatorName?: string | null;
  bookIds: number[];
  bookCount?: number;
}
