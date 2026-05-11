"use client";

import { useState } from "react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
} from "recharts";
import { GenreStatsDTO } from "../types";

interface Props {
  genres: GenreStatsDTO[];
}

export default function GenreStatsSection({ genres }: Props) {
  const [view, setView] = useState<"list" | "chart" | "radar">("list");

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Populairste genres</h2>
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
          <button
            className={`statsViewBtn${view === "radar" ? " active" : ""}`}
            onClick={() => setView("radar")}
          >
            Radar
          </button>
        </div>
      </div>

      {genres.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ol className="statsList">
          {genres.map((genre) => (
            <li key={genre.genre} className="statsItem">
              <span className="statsName">{genre.genre}</span>
              <span className="statsCount">{genre.loanCount} uitleningen</span>
            </li>
          ))}
        </ol>
      ) : view === "chart" ? (
        <div style={{ width: "100%", height: genres.length * 50 + 20 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              layout="vertical"
              data={genres.map((g) => ({
                name: g.genre.length > 25 ? g.genre.slice(0, 25) + "…" : g.genre,
                fullTitle: g.genre,
                uitleningen: g.loanCount,
              }))}
              margin={{ top: 0, right: 30, left: -10, bottom: 0 }}
            >
              <XAxis type="number" allowDecimals={false} />
              <YAxis type="category" dataKey="name" width={160} tick={{ fontSize: 12 }} />
              <Tooltip
                formatter={(value, _, props) => [
                  `${value} uitleningen`,
                  props.payload.fullTitle,
                ]}
              />
              <Bar dataKey="uitleningen" fill="#c9577b" radius={[0, 10, 10, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div style={{ width: "100%", height: 400 }}>
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart
              outerRadius="70%"
              data={genres.map((g) => ({
                subject: g.genre,
                uitleningen: g.loanCount,
              }))}
            >
              <PolarGrid stroke="#ece6f0" />
              <PolarAngleAxis dataKey="subject" tick={{ fontSize: 12, fill: "#8e2446" }} />
              <PolarRadiusAxis tick={{ fontSize: 10, fill: "#8e2446" }} allowDecimals={false} />
              <Radar
                name="Uitleeningen"
                dataKey="uitleningen"
                stroke="#c9577b"
                fill="#c9577b"
                fillOpacity={0.25}
              />
              <Tooltip formatter={(value) => [`${value} uitleningen`]} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
