import "./AchievementsWidget.css";

export interface Achievement {
  categoryKey: string;
  categoryLabel: string;
  currentTier: string | null;
  currentValue: number;
  nextThreshold: number;
  nextTier: string | null;
}

const TIER_LABELS: Record<string, string> = {
  BRONZE: "Brons",
  SILVER: "Zilver",
  GOLD: "Goud",
  PLATINUM: "Platina",
  DIAMOND: "Diamant",
  LEGENDARY: "Legendarisch",
};

const CATEGORY_ICONS: Record<string, string> = {
  BOOKS_READ: "/achievements/book.png",
  PAGES_READ: "/achievements/page.png",
  GENRES_EXPLORED: "/achievements/category.png",
  REVIEWS_WRITTEN: "/achievements/review.png",
  AUTHORS_READ: "/achievements/author.png",
};

function AchievementRow({ achievement }: { achievement: Achievement }) {
  const { categoryKey, categoryLabel, currentTier, currentValue, nextThreshold, nextTier } = achievement;

  const tierClass = currentTier ? `tier-${currentTier.toLowerCase()}` : "tier-locked";
  const tierLabel = currentTier ? TIER_LABELS[currentTier] : null;
  const icon = CATEGORY_ICONS[categoryKey] ?? "/achievements/book.png";

  const progressPct = nextThreshold > 0
    ? Math.min((currentValue / nextThreshold) * 100, 100)
    : 100;

  const firstThreshold = nextThreshold > 0 && currentTier === null ? nextThreshold : null;
  const progressLabel = currentTier === null
    ? `${currentValue} / ${firstThreshold ?? nextThreshold}`
    : nextThreshold > 0
    ? `${currentValue} / ${nextThreshold}`
    : `${currentValue}`;

  return (
    <div className="achievement-row">
      <div className={`achievement-circle ${tierClass}`}>
        <img src={icon} alt={categoryLabel} className="achievement-icon" />
      </div>
      <div className="achievement-info">
        <div className="achievement-header">
          <span className="achievement-label">{categoryLabel}</span>
          {tierLabel && (
            <span className={`achievement-tier-pill ${tierClass}`}>{tierLabel}</span>
          )}
        </div>
        <div className="achievement-progress-bar">
          <div
            className={`achievement-progress-fill ${tierClass}`}
            style={{ width: `${progressPct}%` }}
          />
        </div>
        <div className="achievement-progress-text">
          <span>{progressLabel}</span>
          {nextTier && (
            <span className="achievement-next">→ {TIER_LABELS[nextTier]}</span>
          )}
          {!nextTier && currentTier && (
            <span className="achievement-maxed">Maximaal bereikt</span>
          )}
        </div>
      </div>
    </div>
  );
}

export default function AchievementsWidget({
  achievements,
  loading,
}: {
  achievements: Achievement[];
  loading: boolean;
}) {
  return (
    <div className="widget widget--achievements">
      <p className="widget-label">Badges &amp; prestaties</p>

      {loading ? (
        <p className="widget-loading">Laden…</p>
      ) : (
        <div className="achievements-list">
          {achievements.map((a) => (
            <AchievementRow key={a.categoryKey} achievement={a} />
          ))}
        </div>
      )}
    </div>
  );
}
