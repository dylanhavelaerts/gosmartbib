export interface ReviewSummary {
  id: number;
  userId: number;
  userRole: string;
  text: string;
  reviewDate: string;
  rating: number;
  spoiler: boolean;
  moderationNotice?: string | null;
}

export interface ReviewSectionProps {
  isbn: string;
  onReviewSubmitted?: () => void;
}

export type ReportReason = "FOUT_TAALGEBRUIK" | "SPAM" | "ANDERE";

export const REPORT_REASON_OPTIONS: { value: ReportReason; label: string }[] = [
  { value: "FOUT_TAALGEBRUIK", label: "Fout taalgebruik" },
  { value: "SPAM", label: "Spam" },
  { value: "ANDERE", label: "Andere" },
];
