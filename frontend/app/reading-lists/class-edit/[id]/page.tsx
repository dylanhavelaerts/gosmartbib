"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { useAuth } from "../../../context/AuthContext";
import { Book } from "../../../interfaces/Book";
import type {
  ReadingListAssignmentTargets,
  ReadingListStudentTarget,
  ReadingListTargetType,
} from "../../../interfaces/ReadingList";
import ProtectedRoute from "../../../components/ProtectedRoute";
import StudentTargetSearch from "../../components/StudentTargetSearch";
import {
  cleanTargetPayloadForType,
  gradeLabel,
  scopeLabel,
  toggleNumberInList,
  yearLabel,
} from "../../../utils/readingListTargets";
import "../../create/createReadingList.css";
import "./editReadList.css";

interface ReadingListDetailResponse {
  id: number;
  title: string;
  taskDescription?: string | null;
  deadline?: string | null;
  listType: "CLASS" | "PERSONAL";
  ownList: boolean;
  creatorName?: string | null;
  targetType?: ReadingListTargetType | null;
  targetStudentIds?: number[];
  targetStudentDisplayNames?: string[];
  targetStudents?: ReadingListStudentTarget[];
  targetClassIds?: number[];
  targetClassNames?: string[];
  targetYears?: number[];
  targetGrades?: number[];
  targetAllSchools?: boolean;
  books: Array<{ id: number }>;
}

export default function EditClassReadingListPage() {
  const { user } = useAuth();
  const router = useRouter();
  const params = useParams();
  const id = String(params?.id ?? "");
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const [title, setTitle] = useState("");
  const [deadline, setDeadline] = useState("");
  const [taskDescription, setTaskDescription] = useState("");

  const [targetType, setTargetType] =
    useState<ReadingListTargetType>("CLASSES");
  const [assignmentTargets, setAssignmentTargets] =
    useState<ReadingListAssignmentTargets | null>(null);

  const [selectedTargetStudentIds, setSelectedTargetStudentIds] = useState<
    number[]
  >([]);
  const [selectedTargetStudents, setSelectedTargetStudents] = useState<
  ReadingListStudentTarget[]
>([]);
  const [selectedTargetClassIds, setSelectedTargetClassIds] = useState<
    number[]
  >([]);
  const [selectedTargetYears, setSelectedTargetYears] = useState<number[]>([]);
  const [selectedTargetGrades, setSelectedTargetGrades] = useState<number[]>(
    [],
  );
  const [targetAllSchools, setTargetAllSchools] = useState(false);

  const [selectedBooks, setSelectedBooks] = useState<Book[]>([]);
  const [selectedBookIds, setSelectedBookIds] = useState<number[]>([]);
  const [allBooks, setAllBooks] = useState<Book[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  const [initialLoading, setInitialLoading] = useState(true);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);

  const toDatetimeLocal = (value?: string | null) => {
    if (!value) return "";
    return value.slice(0, 16);
  };

  useEffect(() => {
    fetch(`${apiUrl}/books/all/unpaged`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data: Book[]) => setAllBooks(Array.isArray(data) ? data : []))
      .catch((err) => console.error("Fout bij ophalen boeken:", err));
  }, [apiUrl]);

  useEffect(() => {
    fetch(`${apiUrl}/reading-lists/assignment-targets`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data: ReadingListAssignmentTargets | null) => {
        if (data) {
          setAssignmentTargets(data);
        }
      })
      .catch((err) => console.error("Fout bij ophalen doelgroepen:", err));
  }, [apiUrl]);

  useEffect(() => {
    if (!id || !user) return;

    setInitialLoading(true);
    setMessage(null);

    fetch(`${apiUrl}/reading-lists/${id}`, { credentials: "include" })
      .then(async (res) => {
        if (!res.ok) {
          const body = await res.text();
          throw new Error(body || "Kon leeslijst niet laden.");
        }
        return res.json();
      })
      .then((data: ReadingListDetailResponse) => {
        if (data.listType !== "CLASS") {
          throw new Error("Alleen klaslijsten kunnen hier bewerkt worden.");
        }

        setTitle(data.title || "");
        setTaskDescription(data.taskDescription || "");
        setDeadline(toDatetimeLocal(data.deadline));
        setSelectedBookIds((data.books || []).map((b) => b.id));
        setTargetType(data.targetType ?? "CLASSES");
        const initialStudents = data.targetStudents?.length
          ? data.targetStudents
          : (data.targetStudentIds ?? []).map((studentId, index) => ({
              id: studentId,
              displayName:
                data.targetStudentDisplayNames?.[index] ?? `Leerling ${studentId}`,
              classNames: [],
            }));

        setSelectedTargetStudentIds(initialStudents.map((student) => student.id));
        setSelectedTargetStudents(initialStudents);
        setSelectedTargetClassIds(data.targetClassIds ?? []);
        setSelectedTargetYears(data.targetYears ?? []);
        setSelectedTargetGrades(data.targetGrades ?? []);
        setTargetAllSchools(Boolean(data.targetAllSchools));
      })
      .catch((err) => {
        console.error(err);
        setMessage({
          type: "error",
          text:
            err instanceof Error && err.message
              ? err.message
              : "Kon klasleeslijst niet laden.",
        });
      })
      .finally(() => setInitialLoading(false));
  }, [apiUrl, id, user]);

  useEffect(() => {
    if (allBooks.length === 0) return;
    if (selectedBookIds.length === 0) {
      setSelectedBooks([]);
      return;
    }

    const preselected = allBooks.filter((b) => selectedBookIds.includes(b.id));
    setSelectedBooks(preselected);
  }, [allBooks, selectedBookIds]);

  const filteredBooks = useMemo(() => {
    const q = searchQuery.toLowerCase();
    return allBooks.filter((book) => {
      return (
        book.title?.toLowerCase().includes(q) ||
        book.authors?.some((a) => a.toLowerCase().includes(q)) ||
        book.isbn?.toLowerCase().includes(q)
      );
    });
  }, [allBooks, searchQuery]);

  const addBookToList = (book: Book) => {
    if (selectedBooks.some((b) => b.id === book.id)) return;
    setSelectedBooks((prev) => [...prev, book]);
  };

  const removeBookFromList = (bookId: number) => {
    setSelectedBooks((prev) => prev.filter((b) => b.id !== bookId));
  };

  const removeTargetStudent = (studentId: number) => {
    setSelectedTargetStudentIds((prev) =>
      prev.filter((currentStudentId) => currentStudentId !== studentId),
    );

    setSelectedTargetStudents((prev) =>
      prev.filter((student) => student.id !== studentId),
    );
  };

  const toggleTargetStudent = (student: ReadingListStudentTarget) => {
    if (selectedTargetStudentIds.includes(student.id)) {
      removeTargetStudent(student.id);
      return;
    }

    setSelectedTargetStudentIds((prev) =>
      [...prev, student.id].sort((a, b) => a - b),
    );

    setSelectedTargetStudents((prev) => {
      if (prev.some((selectedStudent) => selectedStudent.id === student.id)) {
        return prev;
      }

      return [...prev, student].sort((a, b) =>
        a.displayName.localeCompare(b.displayName, "nl", {
          sensitivity: "base",
        }),
      );
    });
  };

  const toggleTargetClass = (classId: number) => {
    setSelectedTargetClassIds((prev) => toggleNumberInList(classId, prev));
  };

  const selectTargetType = (nextTargetType: ReadingListTargetType) => {
    setTargetType(nextTargetType);

    if (nextTargetType === "STUDENTS" || nextTargetType === "CLASSES") {
      setTargetAllSchools(false);
    }
  };

  const validateTargetSelection = () => {
    switch (targetType) {
      case "STUDENTS":
        return selectedTargetStudentIds.length > 0;

      case "CLASSES":
        return selectedTargetClassIds.length > 0;

      case "YEARS":
        return selectedTargetYears.length > 0;

      case "GRADES":
        return selectedTargetGrades.length > 0;

      default:
        return false;
    }
  };

  const targetSelectionError = () => {
    switch (targetType) {
      case "STUDENTS":
        return "Kies minstens één leerling als doelgroep.";

      case "CLASSES":
        return "Kies minstens één klas als doelgroep.";

      case "YEARS":
        return "Kies minstens één jaar als doelgroep.";

      case "GRADES":
        return "Kies minstens één graad als doelgroep.";

      default:
        return "Kies een doelgroep.";
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!user) {
      setMessage({ type: "error", text: "Je bent niet correct ingelogd." });
      return;
    }

    if (!title.trim() || !deadline) {
      setMessage({ type: "error", text: "Titel en deadline zijn verplicht." });
      return;
    }

    if (!validateTargetSelection()) {
      setMessage({
        type: "error",
        text: targetSelectionError(),
      });
      return;
    }

    if (selectedBooks.length === 0) {
      setMessage({
        type: "error",
        text: "Voeg minstens één boek toe aan de leeslijst.",
      });
      return;
    }

    setLoading(true);
    setMessage(null);

    const cleanedTargetPayload = cleanTargetPayloadForType(targetType, {
      targetStudentIds: selectedTargetStudentIds,
      targetClassIds: selectedTargetClassIds,
      targetYears: selectedTargetYears,
      targetGrades: selectedTargetGrades,
      targetAllSchools:
        targetType === "YEARS" || targetType === "GRADES"
          ? targetAllSchools
          : false,
    });

    const payload = {
      title: title.trim(),
      taskDescription: taskDescription.trim() || null,
      deadline: deadline.length === 16 ? `${deadline}:00` : deadline,
      bookIds: selectedBooks.map((b) => b.id),
      ...cleanedTargetPayload
    };

    try {
      const res = await fetch(`${apiUrl}/reading-lists/class/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const body = await res.text();
        throw new Error(body || "Fout bij opslaan");
      }

      setMessage({
        type: "success",
        text: "Klasleeslijst succesvol bijgewerkt.",
      });

      setTimeout(() => router.push("/reading-lists/" + id), 700);
    } catch (error) {
      setMessage({
        type: "error",
        text:
          error instanceof Error && error.message
            ? error.message
            : "Kon de klasleeslijst niet bijwerken. Probeer opnieuw.",
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"]}>
      <div className="readingListContainer">
        <div className="crl-subheader">
          <button
            type="button"
            className="crl-back-link"
            onClick={() => router.push(`/reading-lists/${id}`)}
            aria-label="Ga terug naar leeslijst"
          >
            ← Terug naar leeslijst
          </button>

          <h1 className="pageTitle">Klasleeslijst bewerken</h1>
        </div>

        {initialLoading && <p className="loading-text">Leeslijst laden...</p>}

        {message && (
          <div
            className={message.type === "success" ? "msgSuccess" : "msgError"}
          >
            {message.text}
          </div>
        )}

        {!initialLoading && (
          <form onSubmit={handleSubmit} className="create-form-layout">
            <div className="info-island">
              <div className="info-row-top">
                <div className="inputGroup">
                  <label htmlFor="title">1. Titel van de leeslijst *</label>
                  <input
                    id="title"
                    type="text"
                    className="textInput"
                    placeholder="Bijv. Verplichte literatuur 5IT - Q2"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                  />
                </div>
                <div className="inputGroup">
                  <label htmlFor="deadline">2. Deadline *</label>
                  <input
                    id="deadline"
                    type="datetime-local"
                    className="textInput"
                    value={deadline}
                    onChange={(e) => setDeadline(e.target.value)}
                    required
                  />
                </div>
              </div>

              <div className="inputGroup">
                <label htmlFor="taskDescription">
                  3. Algemene opdrachtomschrijving
                </label>
                <textarea
                  id="taskDescription"
                  className="textAreaInput"
                  placeholder="Wat moeten de leerlingen doen met deze boeken?"
                  value={taskDescription}
                  onChange={(e) => setTaskDescription(e.target.value)}
                />
              </div>

              <div className="inputGroup assignment-group">
                <label>4. Doelgroep *</label>
                <p className="assignment-help">
                  Een klasleeslijst heeft exact één type doelgroep. Leerlingen
                  en klassen blijven altijd binnen je eigen school. Jaren en
                  graden kunnen ook met alle scholen gedeeld worden.
                </p>

                <div className="assignment-type-row">
                  <button
                    type="button"
                    className={`assignment-type-button ${
                      targetType === "STUDENTS" ? "active" : ""
                    }`}
                    onClick={() => selectTargetType("STUDENTS")}
                  >
                    Leerlingen
                  </button>

                  <button
                    type="button"
                    className={`assignment-type-button ${
                      targetType === "CLASSES" ? "active" : ""
                    }`}
                    onClick={() => selectTargetType("CLASSES")}
                  >
                    Klassen
                  </button>

                  <button
                    type="button"
                    className={`assignment-type-button ${
                      targetType === "YEARS" ? "active" : ""
                    }`}
                    onClick={() => selectTargetType("YEARS")}
                  >
                    Jaren
                  </button>

                  <button
                    type="button"
                    className={`assignment-type-button ${
                      targetType === "GRADES" ? "active" : ""
                    }`}
                    onClick={() => selectTargetType("GRADES")}
                  >
                    Graden
                  </button>
                </div>

                {targetType === "STUDENTS" && (
                  <div className="assignment-panel assignment-panel-wide">
                    <h3>Specifieke leerlingen</h3>

                    <StudentTargetSearch
                      apiUrl={apiUrl}
                      selectedStudentIds={selectedTargetStudentIds}
                      selectedStudents={selectedTargetStudents}
                      onToggleStudent={toggleTargetStudent}
                      onRemoveStudent={removeTargetStudent}
                    />
                  </div>
                )}

                {targetType === "CLASSES" && (
                  <div className="assignment-panel assignment-panel-wide">
                    <h3>Specifieke klassen</h3>

                    {assignmentTargets?.classes?.length ? (
                      <div className="assignment-class-grid">
                        {assignmentTargets.classes.map((schoolClass) => (
                          <label
                            key={schoolClass.id}
                            className="assignment-chip"
                          >
                            <input
                              type="checkbox"
                              checked={selectedTargetClassIds.includes(
                                schoolClass.id,
                              )}
                              onChange={() => toggleTargetClass(schoolClass.id)}
                            />

                            <span>
                              {schoolClass.name}
                              {schoolClass.year || schoolClass.grade ? (
                                <small>
                                  {schoolClass.year
                                    ? yearLabel(schoolClass.year)
                                    : ""}
                                  {schoolClass.year && schoolClass.grade
                                    ? " · "
                                    : ""}
                                  {schoolClass.grade
                                    ? gradeLabel(schoolClass.grade)
                                    : ""}
                                </small>
                              ) : null}
                            </span>
                          </label>
                        ))}
                      </div>
                    ) : (
                      <p className="assignment-empty">
                        Er zijn nog geen klassen gekend voor je school.
                      </p>
                    )}
                  </div>
                )}

                {targetType === "YEARS" && (
                  <div className="assignment-panel">
                    <div className="assignment-panel-heading">
                      <h3>Jaren</h3>
                      <span>{scopeLabel(targetAllSchools)}</span>
                    </div>

                    <div className="assignment-scope-row">
                      <label>
                        <input
                          type="radio"
                          name="target-scope"
                          checked={!targetAllSchools}
                          onChange={() => setTargetAllSchools(false)}
                        />
                        Eigen school
                      </label>

                      <label>
                        <input
                          type="radio"
                          name="target-scope"
                          checked={targetAllSchools}
                          onChange={() => setTargetAllSchools(true)}
                        />
                        Alle scholen
                      </label>
                    </div>

                    <div className="assignment-chip-grid">
                      {(assignmentTargets?.years ?? [1, 2, 3, 4, 5, 6, 7]).map(
                        (year) => (
                          <label key={year} className="assignment-chip">
                            <input
                              type="checkbox"
                              checked={selectedTargetYears.includes(year)}
                              onChange={() =>
                                setSelectedTargetYears((prev) =>
                                  toggleNumberInList(year, prev),
                                )
                              }
                            />
                            {yearLabel(year)}
                          </label>
                        ),
                      )}
                    </div>
                  </div>
                )}

                {targetType === "GRADES" && (
                  <div className="assignment-panel">
                    <div className="assignment-panel-heading">
                      <h3>Graden</h3>
                      <span>{scopeLabel(targetAllSchools)}</span>
                    </div>

                    <div className="assignment-scope-row">
                      <label>
                        <input
                          type="radio"
                          name="target-scope"
                          checked={!targetAllSchools}
                          onChange={() => setTargetAllSchools(false)}
                        />
                        Eigen school
                      </label>

                      <label>
                        <input
                          type="radio"
                          name="target-scope"
                          checked={targetAllSchools}
                          onChange={() => setTargetAllSchools(true)}
                        />
                        Alle scholen
                      </label>
                    </div>

                    <div className="assignment-chip-grid">
                      {(assignmentTargets?.grades ?? [1, 2, 3]).map((grade) => (
                        <label key={grade} className="assignment-chip">
                          <input
                            type="checkbox"
                            checked={selectedTargetGrades.includes(grade)}
                            onChange={() =>
                              setSelectedTargetGrades((prev) =>
                                toggleNumberInList(grade, prev),
                              )
                            }
                          />
                          {gradeLabel(grade)}
                        </label>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="manage-wrapper">
              <div className="eiland-common book-selector-island">
                <div className="search-container">
                  <label className="search-step-label">5. Zoek boeken</label>
                  <input
                    type="text"
                    className="search-input"
                    placeholder="Titel, auteur of ISBN..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </div>

                <div className="list-container">
                  {allBooks.length === 0 ? (
                    <p className="loading-text">Catalogus laden...</p>
                  ) : filteredBooks.length === 0 ? (
                    <p className="loading-text">Geen boeken gevonden.</p>
                  ) : (
                    <ul className="book-list">
                      {filteredBooks.map((book) => {
                        const isAdded = selectedBooks.some(
                          (b) => b.id === book.id,
                        );
                        return (
                          <li
                            key={book.id}
                            className={`book-list-item ${isAdded ? "added" : ""}`}
                          >
                            <div className="book-list-thumb">
                              {book.thumbnail &&
                              book.thumbnail.trim() !== "" ? (
                                <img src={book.thumbnail} alt={book.title} />
                              ) : (
                                <span>Geen cover</span>
                              )}
                            </div>

                            <div className="book-list-info">
                              <h3 className="book-list-title">{book.title}</h3>
                              <p className="book-list-authors">
                                {book.authors
                                  ? book.authors.join(", ")
                                  : "Onbekend"}
                              </p>
                            </div>

                            <button
                              type="button"
                              className="add-btn"
                              onClick={() => addBookToList(book)}
                              disabled={isAdded}
                            >
                              {isAdded ? "Toegevoegd" : "Voeg toe"}
                            </button>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                </div>
              </div>

              <div className="eiland-common form-details-island">
                <div className="form-details-content">
                  <div className="inputGroup selected-books-label-wrap">
                    <label>
                      6. Boeken op deze lijst ({selectedBooks.length})
                    </label>
                  </div>

                  <div className="selectedBooksContainer">
                    {selectedBooks.length === 0 ? (
                      <div className="selected-empty-state">
                        <p className="selected-empty-state-text">
                          Gebruik de linkerlijst om boeken aan deze leeslijst
                          toe te voegen.
                        </p>
                      </div>
                    ) : (
                      selectedBooks.map((book) => (
                        <div
                          key={book.id}
                          className="selectedBookCard selected-book-card"
                        >
                          <div className="selectedBookHeader selected-book-header">
                            <div className="selected-book-row">
                              <div className="book-list-thumb selected-book-thumb">
                                {book.thumbnail &&
                                book.thumbnail.trim() !== "" ? (
                                  <img src={book.thumbnail} alt={book.title} />
                                ) : (
                                  <span>Geen cover</span>
                                )}
                              </div>
                              <div className="book-list-info">
                                <h3 className="book-list-title selected-book-title">
                                  {book.title}
                                </h3>
                                <p className="book-list-authors selected-book-authors">
                                  door {book.authors?.join(", ") || "Onbekend"}
                                </p>
                              </div>
                            </div>

                            <button
                              type="button"
                              className="remove-btn"
                              onClick={() => removeBookFromList(book.id)}
                            >
                              Verwijder
                            </button>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="submit-container">
                  <div className="crl-form-actions">
                    <button
                      type="button"
                      className="crl-btn-cancel"
                      onClick={() => router.push(`/reading-lists/${id}`)}
                      disabled={loading}
                    >
                      Annuleer
                    </button>

                    <button
                      type="submit"
                      className="crl-btn-save"
                      disabled={loading}
                    >
                      {loading ? "Lijst opslaan..." : "Wijzigingen opslaan"}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </form>
        )}
      </div>
    </ProtectedRoute>
  );
}
