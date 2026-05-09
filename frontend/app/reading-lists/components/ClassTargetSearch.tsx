"use client";

import { useEffect, useMemo, useState } from "react";
import type { ReadingListClassTarget } from "@/app/interfaces/ReadingList";
import { gradeLabel, yearLabel } from "@/app/utils/readingListTargets";

interface ClassTargetSearchProps {
  apiUrl: string;
  selectedClassIds: number[];
  selectedClasses: ReadingListClassTarget[];
  onToggleClass: (schoolClass: ReadingListClassTarget) => void;
  onRemoveClass: (classId: number) => void;
}

export default function ClassTargetSearch({
  apiUrl,
  selectedClassIds,
  selectedClasses,
  onToggleClass,
  onRemoveClass,
}: ClassTargetSearchProps) {
  const [query, setQuery] = useState("");
  const [results, setResults] = useState<ReadingListClassTarget[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const selectedIds = useMemo(
    () => new Set(selectedClassIds),
    [selectedClassIds],
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
          `${apiUrl}/reading-lists/assignment-targets/classes?query=${encodeURIComponent(
            trimmedQuery,
          )}`,
          {
            credentials: "include",
          },
        );

        if (!response.ok) {
          throw new Error("Kon klassen niet zoeken");
        }

        const data: ReadingListClassTarget[] = await response.json();

        if (!cancelled) {
          setResults(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setResults([]);
          setError("Kon klassen niet zoeken.");
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
    (schoolClass) => !selectedIds.has(schoolClass.id),
  );

  const classMetaLabel = (schoolClass: ReadingListClassTarget) => {
    const parts = [];

    if (schoolClass.year) {
      parts.push(yearLabel(schoolClass.year));
    }

    if (schoolClass.grade) {
      parts.push(gradeLabel(schoolClass.grade));
    }

    return parts.join(" · ");
  };

  const handleAddClass = (schoolClass: ReadingListClassTarget) => {
    onToggleClass(schoolClass);
    setQuery("");
    setResults([]);
    setError("");
  };

  return (
    <div className="student-target-search">
      <label className="field-label" htmlFor="class-target-search-input">
        Klassen zoeken
      </label>

      <input
        id="class-target-search-input"
        className="student-target-search-input"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Zoek op klasnaam"
      />

      <p className="student-target-search-help">
        Typ minstens 2 tekens. Je ziet alleen klassen van je eigen school
      </p>

      {loading && (
        <p className="student-target-search-status">Klassen zoeken...</p>
      )}

      {error && <p className="student-target-search-error">{error}</p>}

      {!loading &&
        query.trim().length >= 2 &&
        availableResults.length === 0 &&
        !error && (
          <p className="student-target-search-status">Geen klassen gevonden</p>
        )}

      {availableResults.length > 0 && (
        <div className="student-target-search-results">
          {availableResults.map((schoolClass) => {
            const metaLabel = classMetaLabel(schoolClass);

            return (
              <button
                type="button"
                key={schoolClass.id}
                className="student-target-result"
                onClick={() => handleAddClass(schoolClass)}
              >
                <span className="student-target-result-name">
                  {schoolClass.name}
                </span>

                {metaLabel && (
                  <span className="student-target-result-classes">
                    {metaLabel}
                  </span>
                )}
              </button>
            );
          })}
        </div>
      )}

      {selectedClasses.length > 0 && (
        <div className="selected-student-targets">
          <p className="selected-student-targets-title">
            Geselecteerde klassen
          </p>

          <div className="selected-student-target-list">
            {selectedClasses.map((schoolClass) => {
              const metaLabel = classMetaLabel(schoolClass);

              return (
                <span
                  key={schoolClass.id}
                  className="selected-student-target-pill"
                >
                  <span>
                    {schoolClass.name}

                    {metaLabel && <small>{metaLabel}</small>}
                  </span>

                  <button
                    type="button"
                    onClick={() => onRemoveClass(schoolClass.id)}
                    aria-label={`${schoolClass.name} verwijderen`}
                  >
                    ×
                  </button>
                </span>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
