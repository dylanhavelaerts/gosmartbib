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
import { MostWantedBookDTO } from "../types";

interface Props {
  wantedBooks: MostWantedBookDTO[];
}

export default function MostWantedBooksSection({ wantedBooks }: Props) {
  const [view, setView] = useState<"list" | "chart">("list");

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Meest gewenste boeken</h2>
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

      {wantedBooks.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ol className="statsList">
          {wantedBooks.map((book) => (
            <li key={book.isbn} className="statsItem">
              <span className="statsName">
                {book.title}
                <span className="statsSubtext"> — {book.authors.join(", ")}</span>
              </span>
              <span className="statsCount">{book.notificationCount} meldingen</span>
            </li>
          ))}
        </ol>
      ) : (
        <div style={{ width: "100%", height: wantedBooks.length * 50 + 20 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              layout="vertical"
              data={wantedBooks.map((b) => ({
                name: b.title.length > 50 ? b.title.slice(0, 50) + "…" : b.title,
                fullTitle: b.title,
                meldingen: b.notificationCount,
              }))}
              margin={{ top: 0, right: 30, left: -10, bottom: 0 }}
            >
              <XAxis type="number" allowDecimals={false} />
              <YAxis type="category" dataKey="name" width={170} tick={{ fontSize: 12 }} />
              <Tooltip
                formatter={(value, _, props) => [
                  `${value} meldingen`,
                  props.payload.fullTitle,
                ]}
              />
              <Bar dataKey="meldingen" fill="#c9577b" radius={[0, 10, 10, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
