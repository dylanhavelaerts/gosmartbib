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
import { BookPopularityDTO } from "../types";

interface Props {
  books: BookPopularityDTO[];
}

export default function PopularBooksSection({ books }: Props) {
  const [view, setView] = useState<"list" | "chart">("list");

  return (
    <section className="statsSection">
      <div className="statsSectionHeader">
        <h2>Populairste boeken</h2>
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

      {books.length === 0 ? (
        <p className="statsEmpty">Geen gegevens beschikbaar</p>
      ) : view === "list" ? (
        <ol className="statsList">
          {books.map((book) => (
            <li key={book.isbn} className="statsItem">
              <span className="statsName">
                {book.title}
                <span className="statsSubtext"> — {book.authors.join(", ")}</span>
              </span>
              <span className="statsCount">{book.loanCount} uitleningen</span>
            </li>
          ))}
        </ol>
      ) : (
        <div style={{ width: "100%", height: books.length * 50 + 20 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              layout="vertical"
              data={books.map((b) => ({
                name: b.title.length > 50 ? b.title.slice(0, 50) + "…" : b.title,
                fullTitle: b.title,
                uitleningen: b.loanCount,
              }))}
              margin={{ top: 0, right: 30, left: -10, bottom: 0 }}
            >
              <XAxis type="number" allowDecimals={false} />
              <YAxis type="category" dataKey="name" width={170} tick={{ fontSize: 12 }} />
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
      )}
    </section>
  );
}
