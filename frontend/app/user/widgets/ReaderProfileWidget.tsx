import "./ReaderProfileWidget.css";

export interface ReaderProfile {
  profileType: string | null;
  profileLabel: string | null;
  booksRead: number;
  booksNeeded: number;
}

const PROFILE_ICONS: Record<string, string> = {
  AVONTURIER: "/badges/green.png",
  PIONIER: "/badges/pink.png",
  SPRINTER: "/badges/yellow.png",
  TITAAN: "/badges/blue.png",
};

const PROFILE_DESCRIPTIONS: Record<string, string> = {
  AVONTURIER:
    "Jij durft alles aan! Van thriller tot poëzie, geen genre is veilig voor jou. Jouw brede smaak maakt je tot de meest gevarieerde lezer van de bibliotheek.",
  PIONIER:
    "Terwijl anderen de populaire boeken pakken, kies jij de onontdekte parels. Jij geeft elk boek een kans, ook als niemand anders dat doet.",
  SPRINTER:
    "Boeken zijn voor jou geen hobby, het is een sport. Je leest snel, veel en zonder pauze. De bibliotheek kan jou nauwelijks bijhouden.",
  TITAAN:
    "Dikke boeken? Geen probleem. Jij neemt de tijd voor lange, diepe verhalen en leest pagina's die anderen laten liggen.",
};

export default function ReaderProfileWidget({
  profile,
  distribution,
  loading,
}: {
  profile: ReaderProfile | null;
  distribution: Record<string, number> | null;
  loading: boolean;
}) {
  if (loading) {
    return (
      <div className="widget widget--profile">
        <p className="widget-label">Wat voor lezer ben jij?</p>
        <p className="widget-loading">Laden…</p>
      </div>
    );
  }

  if (!profile || profile.profileType === null) {
    const read = profile?.booksRead ?? 0;
    const needed = profile?.booksNeeded ?? 5;
    const total = read + needed;

    return (
      <div className="widget widget--profile">
        <p className="widget-label">Wat voor lezer ben jij?</p>
        <div className="profile-body">
          <div className="profile-icon-wrap profile-icon-wrap--locked">
            <span className="profile-lock-icon">?</span>
          </div>
          <div className="profile-text">
            <p className="profile-title">Nog niet ontgrendeld</p>
            <p className="profile-desc">
              Lees nog {needed} {needed === 1 ? "boek" : "boeken"} om je
              lezersprofiel te ontdekken
            </p>
            <div className="profile-progress">
              {Array.from({ length: total }).map((_, i) => (
                <span
                  key={i}
                  className={`progress-dot${i < read ? " progress-dot--filled" : ""}`}
                />
              ))}
              <span className="progress-label">
                {read}/{total}
              </span>
            </div>
          </div>
        </div>
      </div>
    );
  }

  const type = profile.profileType;
  const icon = PROFILE_ICONS[type] ?? "/badges/pink.png";
  const desc = PROFILE_DESCRIPTIONS[type] ?? "";
  const pct = distribution?.[type];

  return (
    <div className="widget widget--profile">
      <p className="widget-label">Wat voor lezer ben jij?</p>
      <div className="profile-body">
        <div className="profile-icon-wrap">
          <img
            src={icon}
            alt={profile.profileLabel ?? ""}
            className="profile-icon"
          />
        </div>
        <div className="profile-text">
          <p className="profile-title">{profile.profileLabel}</p>
          <p className="profile-desc">{desc}</p>
          {pct !== undefined && (
            <p className="profile-distribution">
              {pct}% van de lezers in jouw school is ook een{" "}
              {profile.profileLabel}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
