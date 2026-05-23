import "./BookStatsWidget.css";

interface PersonalStats {
  totalBooksRead: number;
  topGenres: string[];
}

export default function BookStatsWidget({
  stats,
  loading,
}: {
  stats: PersonalStats | null;
  loading: boolean;
}) {
  return (
    <div className="widget widget--stats">
      <p className="widget-label">Leesstatistieken</p>

      {loading ? (
        <p className="widget-loading">Laden…</p>
      ) : (
        <>
          <div className="stats-row">
            <div className="stat-block">
              <span className="stat-value">{stats?.totalBooksRead ?? 0}</span>
              <span className="stat-label">Boeken gelezen</span>
            </div>
            <div className="stat-block">
              <span className="stat-value">{stats?.topGenres?.[0] ?? "—"}</span>
              <span className="stat-label">Favoriete genre</span>
            </div>
          </div>

          {stats?.topGenres && stats.topGenres.length > 1 && (
            <div className="genre-tags">
              {stats.topGenres.slice(1).map((g) => (
                <span key={g} className="genre-tag">
                  {g}
                </span>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}

export type { PersonalStats };
