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
}: ClassTargetSearchProps) {
  const [filter, setFilter] = useState("");
  const [allClasses, setAllClasses] = useState<ReadingListClassTarget[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const selectedIds = useMemo(
    () => new Set(selectedClassIds),
    [selectedClassIds],
  );

  useEffect(() => {
    let cancelled = false;

    const fetchClasses = async () => {
      setLoading(true);
      setError("");

      try {
        const response = await fetch(
          `${apiUrl}/reading-lists/assignment-targets/classes`,
          { credentials: "include" },
        );

        if (!response.ok) {
          throw new Error("Kon klassen niet laden");
        }

        const data: ReadingListClassTarget[] = await response.json();

        if (!cancelled) {
          setAllClasses(Array.isArray(data) ? data : []);
        }
      } catch (err) {
        if (!cancelled) {
          console.error(err);
          setError("Kon klassen niet laden.");
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    };

    fetchClasses();

    return () => {
      cancelled = true;
    };
  }, [apiUrl]);

  const filteredClasses = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return allClasses;
    return allClasses.filter((c) => c.name.toLowerCase().includes(q));
  }, [allClasses, filter]);

  const classMetaLabel = (schoolClass: ReadingListClassTarget) => {
    const parts = [];
    if (schoolClass.year) parts.push(yearLabel(schoolClass.year));
    if (schoolClass.grade) parts.push(gradeLabel(schoolClass.grade));
    return parts.join(" · ");
  };

  return (
    <div className="student-target-search">
      <input
        className="student-target-search-input"
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        placeholder="Filter op klasnaam..."
      />

      {loading && (
        <p className="student-target-search-status">Klassen laden...</p>
      )}

      {error && <p className="student-target-search-error">{error}</p>}

      {!loading && !error && filteredClasses.length === 0 && (
        <p className="student-target-search-status">Geen klassen gevonden</p>
      )}

      {!loading && !error && filteredClasses.length > 0 && (
        <div className="assignment-chip-grid">
          {filteredClasses.map((schoolClass) => {
            const metaLabel = classMetaLabel(schoolClass);
            const isSelected = selectedIds.has(schoolClass.id);

            return (
              <label key={schoolClass.id} className="assignment-chip">
                <input
                  type="checkbox"
                  checked={isSelected}
                  onChange={() => onToggleClass(schoolClass)}
                />
                <span>
                  {schoolClass.name}
                  {metaLabel && <small>{metaLabel}</small>}
                </span>
              </label>
            );
          })}
        </div>
      )}

      {selectedClasses.length > 0 && (
        <div className="selected-student-targets">
          <p className="selected-student-targets-title">
            Geselecteerde klassen ({selectedClasses.length})
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
                    onClick={() => onToggleClass(schoolClass)}
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
