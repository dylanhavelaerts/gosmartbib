"use client";

import { useEffect, useState } from "react";
import {
  BarChart,
  Radar,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import "./statistics.css";

interface BookPopularityDTO {
  isbn: string;
  title: string;
  authors: string[];
  loanCount: number;
}

interface GenreStatsDTO {
  genre: string;
  loanCount: number;
}

interface ClassReadingStatsDTO {
  className: string;
  grade: string;
  schoolYear: string;
  loanCount: number;
}

export default function StatisticsPage() {
  const [books, setBooks] = useState<BookPopularityDTO[]>([]);
  const [genres, setGenres] = useState<GenreStatsDTO[]>([]);
  const [classes, setClasses] = useState<ClassReadingStatsDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bookView, setBookView] = useState<"list" | "chart">("list");
  const [genreView, setGenreView] = useState<"list" | "chart" | "radar">(
    "list",
  );

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";

    const fetchAll = async () => {
      try {
        const [booksRes, genresRes, classesRes] = await Promise.all([
          fetch(`${apiUrl}/statistics/popular-books`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/popular-genres`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/highest-count-class`, {
            credentials: "include",
          }),
        ]);

        if (!booksRes.ok || !genresRes.ok || !classesRes.ok) {
          throw new Error("Kon statistieken niet ophalen");
        }

        const [booksData, genresData, classesData] = await Promise.all([
          booksRes.json(),
          genresRes.json(),
          classesRes.json(),
        ]);

        setBooks(booksData);
        setGenres(genresData);
        setClasses(classesData);
      } catch (err: any) {
        setError(err.message || "Er ging iets mis");
      } finally {
        setIsLoading(false);
      }
    };

    fetchAll();
  }, []);

  if (isLoading)
    return (
      <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "TEACHER"]}>
        <p className="statsState">Statistieken laden...</p>
      </ProtectedRoute>
    );
  if (error)
    return (
      <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "TEACHER"]}>
        <p className="statsState">{error}</p>
      </ProtectedRoute>
    );

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER", "TEACHER"]}>
      <main className="statisticsPage">
        <div className="statisticsHeader">
          <h1>Statistieken</h1>
          <p>
            Een overzicht van de meest populaire boeken, genres en klassen van
            jouw school
          </p>
        </div>

        {/* Boekene statistieken */}
        <section className="statsSection">
          <div className="statsSectionHeader">
            <h2>Populairste boeken</h2>
            <div className="statsViewToggle">
              <button
                className={`statsViewBtn${bookView === "list" ? " active" : ""}`}
                onClick={() => setBookView("list")}
              >
                Lijst
              </button>
              <button
                className={`statsViewBtn${bookView === "chart" ? " active" : ""}`}
                onClick={() => setBookView("chart")}
              >
                Staafgrafiek
              </button>
            </div>
          </div>
          {books.length === 0 ? (
            <p className="statsEmpty">Geen gegevens beschikbaar</p>
          ) : bookView === "list" ? (
            <ol className="statsList">
              {books.map((book) => (
                <li key={book.isbn} className="statsItem">
                  <span className="statsName">
                    {book.title}
                    <span className="statsSubtext">
                      {" "}
                      — {book.authors.join(", ")}
                    </span>
                  </span>
                  <span className="statsCount">
                    {book.loanCount} uitleningen
                  </span>
                </li>
              ))}
            </ol>
          ) : (
            <div style={{ width: "100%", height: books.length * 50 + 20 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  layout="vertical"
                  data={books.map((b) => ({
                    name:
                      b.title.length > 50
                        ? b.title.slice(0, 50) + "…"
                        : b.title,
                    fullTitle: b.title,
                    uitleningen: b.loanCount,
                  }))}
                  margin={{ top: 0, right: 30, left: -10, bottom: 0 }}
                >
                  <XAxis type="number" allowDecimals={false} />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={170}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(value, _, props) => [
                      `${value} uitleningen`,
                      props.payload.fullTitle,
                    ]}
                  />
                  <Bar
                    dataKey="uitleningen"
                    fill="#c9577b"
                    radius={[0, 10, 10, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        {/* Genre statistieken */}
        <section className="statsSection">
          <div className="statsSectionHeader">
            <h2>Populairste genres</h2>
            <div className="statsViewToggle">
              <button
                className={`statsViewBtn${genreView === "list" ? " active" : ""}`}
                onClick={() => setGenreView("list")}
              >
                Lijst
              </button>
              <button
                className={`statsViewBtn${genreView === "chart" ? " active" : ""}`}
                onClick={() => setGenreView("chart")}
              >
                Staafgrafiek
              </button>
              <button
                className={`statsViewBtn${genreView === "radar" ? " active" : ""}`}
                onClick={() => setGenreView("radar")}
              >
                Radar
              </button>
            </div>
          </div>
          {genres.length === 0 ? (
            <p className="statsEmpty">Geen gegevens beschikbaar</p>
          ) : genreView === "radar" ? (
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
                  <PolarAngleAxis
                    dataKey="subject"
                    tick={{ fontSize: 12, fill: "#8e2446" }}
                  />
                  <PolarRadiusAxis
                    tick={{ fontSize: 10, fill: "#8e2446" }}
                    allowDecimals={false}
                  />
                  <Radar
                    name="Uitleningen"
                    dataKey="uitleningen"
                    stroke="#c9577b"
                    fill="#c9577b"
                    fillOpacity={0.25}
                  />
                  <Tooltip formatter={(value) => [`${value} uitleningen`]} />
                </RadarChart>
              </ResponsiveContainer>
            </div>
          ) : genreView === "list" ? (
            <ol className="statsList">
              {genres.map((genre) => (
                <li key={genre.genre} className="statsItem">
                  <span className="statsName">
                    {genre.genre}
                    <span className="statsSubtext">
                      {" "}
                      — {genre.loanCount} uitleningen
                    </span>
                  </span>
                  <span className="statsCount">
                    {genre.loanCount} uitleningen
                  </span>
                </li>
              ))}
            </ol>
          ) : genreView === "chart" ? (
            <div style={{ width: "100%", height: genres.length * 50 + 20 }}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  layout="vertical"
                  data={genres.map((b) => ({
                    name:
                      b.genre.length > 25
                        ? b.genre.slice(0, 25) + "…"
                        : b.genre,
                    fullTitle: b.genre,
                    uitleningen: b.loanCount,
                  }))}
                  margin={{ top: 0, right: 30, left: -10, bottom: 0 }}
                >
                  <XAxis type="number" allowDecimals={false} />
                  <YAxis
                    type="category"
                    dataKey="name"
                    width={160}
                    tick={{ fontSize: 12 }}
                  />
                  <Tooltip
                    formatter={(value, _, props) => [
                      `${value} uitleningen`,
                      props.payload.fullTitle,
                    ]}
                  />
                  <Bar
                    dataKey="uitleningen"
                    fill="#c9577b"
                    radius={[0, 10, 10, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          ) : null}
        </section>

        <section className="statsSection">
          <h2>Klassen</h2>
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
                  <span className="statsCount">
                    {cls.loanCount} uitleningen
                  </span>
                </li>
              ))}
            </ol>
          )}
        </section>
      </main>
    </ProtectedRoute>
  );
}
