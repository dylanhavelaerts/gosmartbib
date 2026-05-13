"use client";

import { useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import { LoanDurationStatsDTO } from "../types";

interface Props {
  loanDurations: LoanDurationStatsDTO[];
}

export default function LoanDurationSection({ loanDurations }: Props) {
  const [view, setView] = useState<"list" | "chart">("list");

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Meest voorkomende uitleenduur</h2>
        <div className="statsViewToggle">
          <button
            className={`statsViewBtn${view === "list" ? " active" : ""}`}
            onClick={() => setView("list")}
          >
            Lijst
          </button>
          <button
            className={`statsViewBtn${view === "chart" ? " active" : ""}`}
            onClick={() => setView("chart")}
          >
            Staafgrafiek
          </button>
        </div>
      </div>

      {loanDurations.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ol className="statsList">
          {loanDurations.map((d) => (
            <li key={d.durationDays} className="statsItem">
              <span className="statsName">{d.durationDays} dagen</span>
              <span className="statsCount">{d.count} uitleningen</span>
            </li>
          ))}
        </ol>
      ) : (
        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={loanDurations.map((d) => ({
                name: `${d.durationDays}d`,
                uitleningen: d.count,
              }))}
              margin={{ top: 10, right: 20, left: -10, bottom: 0 }}
            >
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip
                formatter={(value) => [`${value} uitleningen`, "Aantal"]}
                labelFormatter={(label) => `${label} dagen`}
              />
              <Bar dataKey="uitleningen" fill="#c9577b" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
