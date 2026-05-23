import { useAuth } from "../../context/AuthContext";
import "./UserInfoWidget.css";

const PROFILE_STYLES: Record<string, { background: string; color: string }> = {
  AVONTURIER: {
    background: "#71f2a5",
    color: "#14532d",
  },
  PIONIER: {
    background: "#f9b3df",
    color: "#831843",
  },
  SPRINTER: {
    background: "#ffdb3e",
    color: "#713f12",
  },
  TITAN: {
    background: "#6cc6f1",
    color: "#1e3a8a",
  },
};

const TIER_STYLES: Record<string, { background: string; color: string }> = {
  BRONZE: {
    background: "linear-gradient(135deg, #cd7f32, #e8a87c)",
    color: "#fff",
  },
  SILVER: {
    background: "linear-gradient(135deg, #9e9e9e, #d6d6d6)",
    color: "#fff",
  },
  GOLD: {
    background: "linear-gradient(135deg, #d4af37, #f5e27a)",
    color: "#5a4200",
  },
  PLATINUM: {
    background: "linear-gradient(135deg, #78c6e0, #b8e4f0)",
    color: "#1a5a70",
  },
  DIAMOND: {
    background: "linear-gradient(135deg, #667eea, #764ba2)",
    color: "#fff",
  },
  LEGENDARY: {
    background: "linear-gradient(135deg, #3a1c71, #d76d77, #ffaf7b)",
    color: "#fff",
  },
};

const TIER_LABELS: Record<string, string> = {
  BRONZE: "Brons",
  SILVER: "Zilver",
  GOLD: "Goud",
  PLATINUM: "Platina",
  DIAMOND: "Diamant",
  LEGENDARY: "Legendarisch",
};

export default function UserInfoWidget({
  user,
  displayName,
  overallTier,
  profileType,
  profileLabel,
}: {
  user: ReturnType<typeof useAuth>["user"];
  displayName: string | null;
  overallTier: string | null;
  profileType: string | null;
  profileLabel: string | null;
}) {
  const cls = user?.classes?.[0];
  const tierStyle = overallTier ? TIER_STYLES[overallTier] : null;
  const profileStyle = profileType ? PROFILE_STYLES[profileType] : null;

  return (
    <div className="widget widget--user-info">
      <div className="user-info-top">
        <p className="widget-label">Profiel</p>
        <div className="user-info-pills">
          {profileLabel && profileStyle && (
            <span
              className="user-tag user-tag--tier"
              style={{
                background: profileStyle.background,
                color: profileStyle.color,
              }}
            >
              {profileLabel}
            </span>
          )}
          {tierStyle && overallTier && (
            <span
              className="user-tag user-tag--tier"
              style={{
                background: tierStyle.background,
                color: tierStyle.color,
              }}
            >
              {TIER_LABELS[overallTier]}
            </span>
          )}
        </div>
      </div>
      <div className="user-avatar">
        <img src="/user.png" alt="Gebruiker" className="user-avatar-icon" />
      </div>
      <p className="user-name">{displayName ?? "-"}</p>
      {cls && (
        <div className="user-tags">
          {cls.name && <span className="user-tag">{cls.name}</span>}
          {cls.grade && <span className="user-tag">{cls.grade}</span>}
          {cls.schoolYear && <span className="user-tag">{cls.schoolYear}</span>}
        </div>
      )}
    </div>
  );
}
