"use client";

import { useEffect, useState } from "react";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import {
  BookPopularityDTO,
  ClassReadingStatsDTO,
  GenreStatsDTO,
  LoanDurationStatsDTO,
  LoansPerMonthDTO,
  MostWantedBookDTO,
  OverviewStatsDTO,
  ReturnPunctualityDTO,
  TopReaderStudentDTO,
} from "./types";
import PopularBooksSection from "./components/PopularBooksSection";
import GenreStatsSection from "./components/GenreStatsSection";
import ClassStatsSection from "./components/ClassStatsSection";
import ReturnPunctualitySection from "./components/ReturnPunctualitySection";
import LoanDurationSection from "./components/LoanDurationSection";
import MostWantedBooksSection from "./components/MostWantedBooksSection";
import OverviewSection from "./components/OverviewSection";
import TopReadersSection from "./components/TopReadersSection";
import LoansPerMonthSection from "./components/LoansPerMonthSection";
import LeastPopularBooksSection from "./components/LeastPopularBooksSection";
import "./statistics.css";

type Category = "books" | "loans" | "students";

const CATEGORIES: { id: Category; label: string; icon: string }[] = [
  { id: "books", label: "Boekstatistieken", icon: "/book-icon.png" },
  { id: "loans", label: "Uitleenstatistieken", icon: "/history.png" },
  { id: "students", label: "Leerlingstatistieken", icon: "/user.png" },
];

export default function StatisticsPage() {
  const [activeCategory, setActiveCategory] = useState<Category>("books");
  const [overview, setOverview] = useState<OverviewStatsDTO | null>(null);
  const [books, setBooks] = useState<BookPopularityDTO[]>([]);
  const [genres, setGenres] = useState<GenreStatsDTO[]>([]);
  const [classes, setClasses] = useState<ClassReadingStatsDTO[]>([]);
  const [punctuality, setPunctuality] = useState<ReturnPunctualityDTO | null>(
    null,
  );
  const [loanDurations, setLoanDurations] = useState<LoanDurationStatsDTO[]>(
    [],
  );
  const [wantedBooks, setWantedBooks] = useState<MostWantedBookDTO[]>([]);
  const [topReaders, setTopReaders] = useState<TopReaderStudentDTO[]>([]);
  const [loansPerMonth, setLoansPerMonth] = useState<LoansPerMonthDTO[]>([]);
  const [leastPopularBooks, setLeastPopularBooks] = useState<BookPopularityDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";

    const fetchAll = async () => {
      try {
        const [
          overviewRes,
          booksRes,
          genresRes,
          classesRes,
          punctualityRes,
          durationsRes,
          wantedRes,
          topReadersRes,
          loansPerMonthRes,
          leastPopularRes,
        ] = await Promise.all([
          fetch(`${apiUrl}/statistics/overview`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/popular-books`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/popular-genres`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/highest-count-class`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/return-punctuality`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/loan-duration-distribution`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/most-wanted-books`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/top-readers`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/loans-per-month`, { credentials: "include" }),
          fetch(`${apiUrl}/statistics/least-popular-books`, { credentials: "include" }),
        ]);

        if (
          !overviewRes.ok || !booksRes.ok || !genresRes.ok || !classesRes.ok ||
          !punctualityRes.ok || !durationsRes.ok || !wantedRes.ok ||
          !topReadersRes.ok || !loansPerMonthRes.ok || !leastPopularRes.ok
        ) {
          throw new Error("Kon statistieken niet ophalen");
        }

        const [
          overviewData, booksData, genresData, classesData,
          punctualityData, durationsData, wantedData,
          topReadersData, loansPerMonthData, leastPopularData,
        ] = await Promise.all([
          overviewRes.json(), booksRes.json(), genresRes.json(), classesRes.json(),
          punctualityRes.json(), durationsRes.json(), wantedRes.json(),
          topReadersRes.json(), loansPerMonthRes.json(), leastPopularRes.json(),
        ]);

        setOverview(overviewData);
        setBooks(booksData);
        setGenres(genresData);
        setClasses(classesData);
        setPunctuality(punctualityData);
        setLoanDurations(durationsData);
        setWantedBooks(wantedData);
        setTopReaders(topReadersData);
        setLoansPerMonth(loansPerMonthData);
        setLeastPopularBooks(leastPopularData);
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

        {overview && <OverviewSection overview={overview} />}

        <nav className="statsCategoryNav">
          {CATEGORIES.map((cat) => (
            <button
              key={cat.id}
              className={`statsCategoryBtn${activeCategory === cat.id ? " active" : ""}`}
              onClick={() => setActiveCategory(cat.id)}
            >
              <img src={cat.icon} alt="" className="statsCategoryIcon" />
              {cat.label}
            </button>
          ))}
        </nav>

        {activeCategory === "books" && (
          <>
            <PopularBooksSection books={books} />
            <LeastPopularBooksSection books={leastPopularBooks} />
            <MostWantedBooksSection wantedBooks={wantedBooks} />
            <GenreStatsSection genres={genres} />
          </>
        )}

        {activeCategory === "loans" && (
          <>
            <LoansPerMonthSection loansPerMonth={loansPerMonth} />
            <ReturnPunctualitySection punctuality={punctuality} />
            <LoanDurationSection loanDurations={loanDurations} />
          </>
        )}

        {activeCategory === "students" && (
          <>
            <TopReadersSection topReaders={topReaders} />
            <ClassStatsSection classes={classes} />
          </>
        )}
      </main>
    </ProtectedRoute>
  );
}
