"use client";

import { useEffect, useState, useRef } from "react";
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
  const [leastPopularBooks, setLeastPopularBooks] = useState<
    BookPopularityDTO[]
  >([]);
  const [selectedGrade, setSelectedGrade] = useState("");
  const [selectedClass, setSelectedClass] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const isFirstFetch = useRef(true);

  const availableGrades = [...new Set(classes.map((c) => c.grade))].sort();
  const availableClasses = classes
    .filter((c) => !selectedGrade || c.grade === selectedGrade)
    .map((c) => c.className)
    .filter((v, i, a) => a.indexOf(v) === i)
    .sort();

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
    const searchParams = new URLSearchParams();
    if (selectedClass) searchParams.set("className", selectedClass);
    if (selectedGrade) searchParams.set("grade", selectedGrade);
    const params = searchParams.size > 0 ? `?${searchParams}` : "";

    if (isFirstFetch.current) setIsLoading(true);

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
          fetch(`${apiUrl}/statistics/overview${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/popular-books${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/popular-genres${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/highest-count-class`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/return-punctuality${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/loan-duration-distribution${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/most-wanted-books`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/top-readers${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/loans-per-month${params}`, {
            credentials: "include",
          }),
          fetch(`${apiUrl}/statistics/least-popular-books${params}`, {
            credentials: "include",
          }),
        ]);

        if (
          !overviewRes.ok ||
          !booksRes.ok ||
          !genresRes.ok ||
          !classesRes.ok ||
          !punctualityRes.ok ||
          !durationsRes.ok ||
          !wantedRes.ok ||
          !topReadersRes.ok ||
          !loansPerMonthRes.ok ||
          !leastPopularRes.ok
        ) {
          throw new Error("Kon statistieken niet ophalen");
        }

        const [
          overviewData,
          booksData,
          genresData,
          classesData,
          punctualityData,
          durationsData,
          wantedData,
          topReadersData,
          loansPerMonthData,
          leastPopularData,
        ] = await Promise.all([
          overviewRes.json(),
          booksRes.json(),
          genresRes.json(),
          classesRes.json(),
          punctualityRes.json(),
          durationsRes.json(),
          wantedRes.json(),
          topReadersRes.json(),
          loansPerMonthRes.json(),
          leastPopularRes.json(),
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
        isFirstFetch.current = false;
      }
    };

    fetchAll();
  }, [selectedClass, selectedGrade]);

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
        <div className="statsFilterBar">
          <label htmlFor="gradeFilter">Jaar:</label>
          <select
            id="gradeFilter"
            value={selectedGrade}
            onChange={(e) => {
              setSelectedGrade(e.target.value);
              setSelectedClass("");
            }}
            className="statsClassFilter"
          >
            <option value="">Alle jaren</option>
            {availableGrades.map((grade) => (
              <option key={grade} value={grade}>
                {grade}
              </option>
            ))}
          </select>

          <label htmlFor="classFilter">Klas:</label>
          <select
            id="classFilter"
            value={selectedClass}
            onChange={(e) => setSelectedClass(e.target.value)}
            className="statsClassFilter"
          >
            <option value="">Alle klassen</option>
            {availableClasses.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </select>

          {(selectedGrade || selectedClass) && (
            <button
              onClick={() => {
                setSelectedGrade("");
                setSelectedClass("");
              }}
              className="statsResetFilter"
            >
              Alles tonen
            </button>
          )}
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
