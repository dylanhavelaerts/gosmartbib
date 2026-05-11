"use client";

import { useState } from "react";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  ResponsiveContainer,
} from "recharts";
import { LoansPerMonthDTO } from "../types";

interface Props {
  loansPerMonth: LoansPerMonthDTO[];
}

const DUTCH_MONTHS = [
  "", "januari", "februari", "maart", "april", "mei", "juni",
  "juli", "augustus", "september", "oktober", "november", "december",
];

const DUTCH_MONTHS_SHORT = [
  "", "jan", "feb", "mrt", "apr", "mei", "jun",
  "jul", "aug", "sep", "okt", "nov", "dec",
];

export default function LoansPerMonthSection({ loansPerMonth }: Props) {
  const [view, setView] = useState<"list" | "chart">("chart");

  const chartData = loansPerMonth.map((d) => ({
    label: `${DUTCH_MONTHS_SHORT[d.month]} ${d.year}`,
    uitleningen: d.count,
  }));

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Uitleningen per maand</h2>
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
            Lijndiagram
          </button>
        </div>
      </div>

      {loansPerMonth.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ol className="statsList">
          {loansPerMonth.map((d) => (
            <li key={`${d.year}-${d.month}`} className="statsItem">
              <span className="statsName">
                {DUTCH_MONTHS[d.month]} {d.year}
              </span>
              <span className="statsCount">{d.count} uitleningen</span>
            </li>
          ))}
        </ol>
      ) : (
        <div style={{ width: "100%", height: 280 }}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={chartData}
              margin={{ top: 10, right: 20, left: -10, bottom: 0 }}
            >
              <CartesianGrid stroke="#ece6f0" strokeDasharray="4 4" vertical={false} />
              <XAxis dataKey="label" tick={{ fontSize: 11 }} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(value) => [`${value} uitleningen`, "Aantal"]} />
              <Line
                type="monotone"
                dataKey="uitleningen"
                stroke="#c9577b"
                strokeWidth={2.5}
                dot={{ fill: "#8e2446", r: 4 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
