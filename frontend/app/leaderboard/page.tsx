"use client";

import Image from "next/image";
import { useEffect, useState } from "react";
import "./leaderboard.css";

interface ClassReadingStatsDTO {
  className: string;
  grade: string;
  schoolYear: string;
  loanCount: number;
}

interface TopReaderStudentDTO {
  smartschoolUid: string;
  loanCount: number;
}

const MEDAL_IMAGES = [
  "/leaderboard/silver.png",
  "/leaderboard/gold.png",
  "/leaderboard/bronze.png",
];
const RANKS = [2, 1, 3];

export default function LeaderboardPage() {
  const [topClasses, setTopClasses] = useState<ClassReadingStatsDTO[]>([]);
  const [topStudents, setTopStudents] = useState<TopReaderStudentDTO[]>([]);

  useEffect(() => {
    const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";
    fetch(`${apiUrl}/statistics/highest-count-class`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data) setTopClasses(data);
      })
      .catch(() => {});
    fetch(`${apiUrl}/statistics/top-readers`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        if (data) setTopStudents(data);
      })
      .catch(() => {});
  }, []);

  const podium = [topClasses[1], topClasses[0], topClasses[2]];

  return (
    <main className="leaderboard-page">
      <div className="leaderboard-header">
        <h1>Leaderboard</h1>
        <p>Bekijk de top lezers en klassen van jouw school</p>
      </div>

      <div className="leaderboard-stage">
        <div className="podium">
          {RANKS.map((rank, i) => (
            <div key={rank} className={`podium-slot podium-slot--${rank}`}>
              <div className="podium-info">
                <Image
                  className="podium-medal"
                  src={MEDAL_IMAGES[i]}
                  alt={`Rank ${RANKS[i]}`}
                  width={48}
                  height={48}
                />
                {podium[i] ? (
                  <>
                    <span className="podium-class">{podium[i]!.className}</span>
                    <span className="podium-grade">{podium[i]!.grade}</span>
                    <span className="podium-count">
                      {podium[i]!.loanCount} uitleeningen
                    </span>
                  </>
                ) : (
                  <span className="podium-empty">—</span>
                )}
              </div>
              <div className="podium-block">
                <span className="podium-rank">{rank}</span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="leaderboard-lists">
        <div className="leaderboard-widget">
          <h2>Top 10 klassen</h2>
          {topClasses.length === 0 ? (
            <p className="leaderboard-empty">Geen gegevens beschikbaar</p>
          ) : (
            <ol className="leaderboard-list">
              {topClasses.slice(0, 10).map((cls) => (
                <li
                  key={`${cls.className}-${cls.schoolYear}`}
                  className="leaderboard-list-item"
                >
                  <span className="leaderboard-list-name">
                    {cls.className}
                    <span className="leaderboard-list-sub"> — {cls.grade}</span>
                  </span>
                  <span className="leaderboard-list-count">
                    {cls.loanCount} uitleeningen
                  </span>
                </li>
              ))}
            </ol>
          )}
        </div>

        <div className="leaderboard-widget">
          <h2>Top 10 leerlingen</h2>
          {topStudents.length === 0 ? (
            <p className="leaderboard-empty">Geen gegevens beschikbaar</p>
          ) : (
            <ol className="leaderboard-list">
              {topStudents.slice(0, 10).map((student) => (
                <li
                  key={student.smartschoolUid}
                  className="leaderboard-list-item"
                >
                  <span className="leaderboard-list-name">
                    {student.smartschoolUid}
                  </span>
                  <span className="leaderboard-list-count">
                    {student.loanCount} pagina's
                  </span>
                </li>
              ))}
            </ol>
          )}
        </div>
      </div>
    </main>
  );
}
