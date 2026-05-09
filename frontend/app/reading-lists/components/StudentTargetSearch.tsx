"use client";

import { useEffect, useMemo, useState } from "react";
import type { ReadingListStudentTarget } from "@/app/interfaces/ReadingList";

interface StudentTargetSearchProps {
  apiUrl: string;
  selectedStudentIds: number[];
  selectedStudents: ReadingListStudentTarget[];
  onToggleStudent: (student: ReadingListStudentTarget) => void;
  onRemoveStudent: (studentId: number) => void;
}

export default function StudentTargetSearch({
  apiUrl,
  selectedStudentIds,
  selectedStudents,
  onToggleStudent,
  onRemoveStudent,
}: StudentTargetSearchProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<ReadingListStudentTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const selectedIds = useMemo(
    () => new Set(selectedStudentIds),
    [selectedStudentIds],
  );

  useEffect(() => {
    const trimmedQuery = query.trim();

    if (trimmedQuery.length < 2) {
      setResults([]);
      setLoading(false);
      setError("");
      return;
    }

    let cancelled = false;

    const timeoutId = window.setTimeout(async () => {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(
          `${apiUrl}/reading-lists/assignment-targets/students?query=${encodeURIComponent(
            trimmedQuery,
          )}`,
          {
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("Kon leerlingen niet zoeken.");
        }

        const data: ReadingListStudentTarget[] = await response.json();

        if (!cancelled) {
          setResults(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setResults([]);
          setError("Kon leerlingen niet zoeken.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(timeoutId);
    };
  }, [apiUrl, query]);

  const availableResults = results.filter(
    (student) => !selectedIds.has(student.id),
  );

  const handleAddStudent = (student: ReadingListStudentTarget) => {
    onToggleStudent(student);
    setQuery("");
    setResults([]);
    setError("");
  };

  return (
    <div className="student-target-search">
      <label className="field-label" htmlFor="student-target-search-input">
        Leerlingen zoeken
      </label>

      <input
        id="student-target-search-input"
        className="student-target-search-input"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Zoek op naam, gebruikersnaam of klas..."
      />

      <p className="student-target-search-help">
        Typ minstens 2 tekens. Je ziet alleen leerlingen van je eigen school
      </p>

      {loading && (
        <p className="student-target-search-status">Leerlingen zoeken...</p>
      )}

      {error && <p className="student-target-search-error">{error}</p>}

      {!loading &&
        query.trim().length >= 2 &&
        availableResults.length === 0 &&
        !error && (
          <p className="student-target-search-status">
            Geen leerlingen gevonden
          </p>
        )}

      {availableResults.length > 0 && (
        <div className="student-target-search-results">
          {availableResults.map((student) => (
            <button
              type="button"
              key={student.id}
              className="student-target-result"
              onClick={() => handleAddStudent(student)}
            >
              <span className="student-target-result-name">
                {student.displayName}
              </span>

              {student.classNames.length > 0 && (
                <span className="student-target-result-classes">
                  {student.classNames.join(", ")}
                </span>
              )}
            </button>
          ))}
        </div>
      )}

      {selectedStudents.length > 0 && (
        <div className="selected-student-targets">
          <p className="selected-student-targets-title">
            Geselecteerde leerlingen
          </p>

          <div className="selected-student-target-list">
            {selectedStudents.map((student) => (
              <span key={student.id} className="selected-student-target-pill">
                <span>
                  {student.displayName}

                  {student.classNames.length > 0 && (
                    <small>{student.classNames.join(", ")}</small>
                  )}
                </span>

                <button
                  type="button"
                  onClick={() => onRemoveStudent(student.id)}
                  aria-label={`${student.displayName} verwijderen`}
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
