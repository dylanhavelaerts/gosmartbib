import { ClassReadingStatsDTO } from "../types";

interface Props {
  classes: ClassReadingStatsDTO[];
}

export default function ClassStatsSection({ classes }: Props) {
  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Klassen die het meest lezen</h2>
      </div>
      {classes.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : (
        <ol className="statsList">
          {classes.map((cls) => (
            <li
              key={`${cls.className}-${cls.schoolYear}`}
              className="statsItem"
            >
              <span className="statsName">
                {cls.className}
                <span className="statsSubtext">
                  {" "}
                  — {cls.grade} ({cls.schoolYear})
                </span>
              </span>
              <span className="statsCount">{cls.loanCount} uitleningen</span>
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}
