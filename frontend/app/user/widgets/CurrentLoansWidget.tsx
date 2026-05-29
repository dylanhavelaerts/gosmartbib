import "./CurrentLoansWidget.css";

interface ActiveLoan {
  loanId: number;
  quantity: number;
  dueDate: string;
  book: {
    title: string;
    thumbnail: string | null;
    authors?: string[];
  } | null;
}

function formatDue(dueDateString: string): { text: string; cls: string } {
  const due = new Date(dueDateString);
  const now = new Date();
  due.setHours(0, 0, 0, 0);
  now.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((due.getTime() - now.getTime()) / 86_400_000);
  if (diffDays < 0)
    return { text: `${Math.abs(diffDays)}d te laat`, cls: "due-late" };
  if (diffDays <= 3) return { text: `Nog ${diffDays}d`, cls: "due-soon" };
  return { text: `Nog ${diffDays}d`, cls: "due-ok" };
}

/**
 * Dashboard-widget die de actieve leningen van de ingelogde gebruiker toont.
 * Toont maximaal 4 leningen; bij meer verschijnt een "+N meer" indicator.
 * Pure display component data en laadstatus worden door de parent aangeleverd via props.
 */
export default function CurrentLoansWidget({
  loans,
  loading,
}: {
  loans: ActiveLoan[];
  loading: boolean;
}) {
  const total = loans.reduce((s, l) => s + l.quantity, 0);

  return (
    <div className="widget widget--loans">
      <div className="widget-header-row">
        <p className="widget-label">Momenteel ontleend</p>
        {total > 0 && <span className="widget-count">{total}</span>}
      </div>

      {loading ? (
        <p className="widget-loading">Laden…</p>
      ) : loans.length === 0 ? (
        <p className="widget-empty">Geen actieve uitleningen</p>
      ) : (
        <div className="loans-list">
          {loans.slice(0, 4).map((loan) => {
            const due = formatDue(loan.dueDate);
            return (
              <div key={loan.loanId} className="loan-item">
                <div className="loan-cover">
                  {loan.book?.thumbnail ? (
                    <img src={loan.book.thumbnail} alt={loan.book.title} />
                  ) : (
                    <div className="loan-cover-empty" />
                  )}
                </div>
                <div className="loan-info">
                  <p className="loan-title">{loan.book?.title ?? "Onbekend"}</p>
                  <p className="loan-author">
                    {loan.book?.authors?.join(", ") ?? "Auteur onbekend"}
                  </p>
                  <span className={`loan-due loan-due--${due.cls}`}>
                    {due.text}
                  </span>
                </div>
              </div>
            );
          })}
          {loans.length > 4 && (
            <p className="loans-more">+{loans.length - 4} meer</p>
          )}
        </div>
      )}
    </div>
  );
}

export type { ActiveLoan };
