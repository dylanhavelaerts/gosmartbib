"use client";

import { useState } from "react";
import { PieChart, Pie, Tooltip, Legend, ResponsiveContainer } from "recharts";
import { ReturnPunctualityDTO } from "../types";

interface Props {
  punctuality: ReturnPunctualityDTO | null;
}

export default function ReturnPunctualitySection({ punctuality }: Props) {
  const [view, setView] = useState<"list" | "chart">("list");

  const chartData = punctuality
    ? [
        { name: "Op tijd", value: punctuality.onTimeCount, fill: "#63df84" },
        { name: "Te laat", value: punctuality.lateCount, fill: "#8e2446" },
      ]
    : [];

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Terugbreng punctualiteit</h2>
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
            Donut
          </button>
        </div>
      </div>

      {punctuality === null ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ul className="statsList">
          <li className="statsItem">
            <span className="statsName">Op tijd teruggebracht</span>
            <span className="statsCount">{punctuality.onTimeCount}</span>
          </li>
          <li className="statsItem">
            <span className="statsName">Te laat teruggebracht</span>
            <span className="statsCount">{punctuality.lateCount}</span>
          </li>
        </ul>
      ) : (
        <div style={{ width: "100%", height: 300 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={chartData}
                cx="50%"
                cy="50%"
                innerRadius="50%"
                outerRadius="75%"
                paddingAngle={4}
                dataKey="value"
              />

              <Tooltip formatter={(value) => [`${value}`, ""]} />
              <Legend
                formatter={(value) => (
                  <span style={{ color: "#201713", fontSize: "0.85rem" }}>
                    {value}
                  </span>
                )}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
