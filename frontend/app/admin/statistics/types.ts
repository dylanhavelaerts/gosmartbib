export interface BookPopularityDTO {
  isbn: string;
  title: string;
  authors: string[];
  loanCount: number;
}

export interface GenreStatsDTO {
  genre: string;
  loanCount: number;
}

export interface ClassReadingStatsDTO {
  className: string;
  grade: string;
  schoolYear: string;
  loanCount: number;
}

export interface ReturnPunctualityDTO {
  onTimeCount: number;
  lateCount: number;
  extensionApprovedCount: number;
  extensionPendingCount: number;
  extensionDeniedCount: number;
}

export interface LoanDurationStatsDTO {
  durationDays: number;
  count: number;
}

export interface MostWantedBookDTO {
  isbn: string;
  title: string;
  authors: string[];
  notificationCount: number;
}

export interface OverviewStatsDTO {
  activeLoans: number;
  overdueLoans: number;
  inactiveStudents: number;
  pendingExtensions: number;
}

export interface TopReaderStudentDTO {
  smartschoolUid: string;
  loanCount: number;
}

export interface LoansPerMonthDTO {
  year: number;
  month: number;
  count: number;
}
