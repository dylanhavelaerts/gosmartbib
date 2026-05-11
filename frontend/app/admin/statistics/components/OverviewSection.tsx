import { OverviewStatsDTO } from "../types";

interface Props {
  overview: OverviewStatsDTO;
}

interface StatCard {
  label: string;
  value: number;
  subtext: string;
  alertWhenPositive?: boolean;
}

export default function OverviewSection({ overview }: Props) {
  const cards: StatCard[] = [
    {
      label: "Actieve uitleningen",
      value: overview.activeLoans,
      subtext: "Boeken momenteel uitgeleend",
    },
    {
      label: "Open verlengingsverzoeken",
      value: overview.pendingExtensions,
      subtext: "Wachten op goedkeuring",
    },
    {
      label: "Te laat teruggebracht",
      value: overview.overdueLoans,
      subtext: "Voorbij de uitleendatum",
      alertWhenPositive: true,
    },
    {
      label: "Inactieve leerlingen",
      value: overview.inactiveStudents,
      subtext: "Geen uitleningen in 4 weken",
      alertWhenPositive: true,
    },
  ];

  return (
    <div className="overviewGrid">
      {cards.map((card) => {
        const isAlert = !!card.alertWhenPositive && card.value > 0;
        return (
          <div key={card.label} className="overviewCard">
            <span
              className={`overviewCard__value${isAlert ? " overviewCard__value--alert" : ""}`}
            >
              {card.value}
            </span>
            <span className="overviewCard__label">{card.label}</span>
            <span className="overviewCard__subtext">{card.subtext}</span>
          </div>
        );
      })}
    </div>
  );
}
