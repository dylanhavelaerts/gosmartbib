"use client";

import Link from "next/link";
import { useAuth } from "@/app/context/AuthContext";
import { OverviewStatsDTO } from "../types";

interface Props {
  overview: OverviewStatsDTO;
}

interface StatCard {
  label: string;
  value: number;
  subtext: string;
  alertWhenPositive?: boolean;
  href?: string;
}

export default function OverviewSection({ overview }: Props) {
  const { user } = useAuth();
  const isBeheerder = user?.role === "LIBRARIAN";

  const cards: StatCard[] = [
    // Bibbeheerder kan hier op de actieve leningen drukken en wordt dan gestuurd naar de algmene leenpagina -> Leerkracht kan niet drukken.

    {
      label: "Actieve uitleningen",
      value: overview.activeLoans,
      subtext: "Boeken momenteel uitgeleend",
      href: isBeheerder ? "/admin/loans-overview" : undefined,
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
        const inner = (
          <>
            <span
              className={`overviewCard__value${isAlert ? " overviewCard__value--alert" : ""}`}
            >
              {card.value}
            </span>
            <span className="overviewCard__label">{card.label}</span>
            <span className="overviewCard__subtext">{card.subtext}</span>
          </>
        );

        return card.href ? (
          <Link
            key={card.label}
            href={card.href}
            className="overviewCard overviewCard--link"
          >
            {inner}
          </Link>
        ) : (
          <div key={card.label} className="overviewCard">
            {inner}
          </div>
        );
      })}
    </div>
  );
}
