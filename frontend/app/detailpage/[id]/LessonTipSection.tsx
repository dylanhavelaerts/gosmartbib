"use client";

import { useState, useEffect } from "react";

interface LessonTipDTO {
  id: number;
  text: string;
  anonymous: boolean;
  authorName: string | null;
  ownTip: boolean;
  createdDate: string;
}

export default function LessonTipSection({ bookId }: { bookId: number }) {
  const [tips, setTips] = useState<LessonTipDTO[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [newText, setNewText] = useState("");
  const [isAnonymous, setIsAnonymous] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editText, setEditText] = useState("");
  const [editAnonymous, setEditAnonymous] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<number | null>(null);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "";

  useEffect(() => {
    const loadTips = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const res = await fetch(`${apiUrl}/books/${bookId}/lesson-tips`, {
          credentials: "include",
        });
        if (!res.ok) throw new Error();
        setTips(await res.json());
      } catch {
        setError("Kon de lestips niet laden.");
      } finally {
        setIsLoading(false);
      }
    };
    loadTips();
  }, [bookId, apiUrl]);

  const handleSubmit = async () => {
    if (!newText.trim()) return;
    setIsSubmitting(true);
    try {
      const res = await fetch(`${apiUrl}/books/${bookId}/lesson-tips`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ text: newText, anonymous: isAnonymous }),
      });
      if (!res.ok) throw new Error();
      const created: LessonTipDTO = await res.json();
      setTips((prev) => [created, ...prev]);
      setNewText("");
      setIsAnonymous(false);
    } catch {
      // gebruiker kan het opnieuw proberen
    } finally {
      setIsSubmitting(false);
    }
  };

  const startEdit = (tip: LessonTipDTO) => {
    setEditingId(tip.id);
    setEditText(tip.text);
    setEditAnonymous(tip.anonymous);
  };

  const handleUpdate = async (id: number) => {
    setIsSubmitting(true);
    try {
      const res = await fetch(`${apiUrl}/lesson-tips/${id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ text: editText, anonymous: editAnonymous }),
      });
      if (!res.ok) throw new Error();
      const updated: LessonTipDTO = await res.json();
      setTips((prev) => prev.map((t) => (t.id === id ? updated : t)));
      setEditingId(null);
    } catch {
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    setDeletingId(id);
    try {
      const res = await fetch(`${apiUrl}/lesson-tips/${id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!res.ok) throw new Error();
      setTips((prev) => prev.filter((t) => t.id !== id));
      setConfirmDeleteId(null);
    } catch {
    } finally {
      setDeletingId(null);
    }
  };

  const formatDate = (dateStr: string) =>
    new Date(dateStr).toLocaleDateString("nl-BE", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });

  if (isLoading) return <p className="lessonTipsLoading">Laden...</p>;
  if (error) return <p className="lessonTipsError">{error}</p>;

  return (
    <div className="lessonTipsSection">
      <div className="lessonTipForm">
        <textarea
          className="lessonTipTextarea"
          placeholder="Schrijf hier je lestip voor collega's..."
          value={newText}
          onChange={(e) => setNewText(e.target.value)}
          rows={4}
        />
        <div className="lessonTipFormFooter">
          <label className="lessonTipAnonLabel">
            <input
              type="checkbox"
              checked={isAnonymous}
              onChange={(e) => setIsAnonymous(e.target.checked)}
            />
            Anoniem plaatsen
          </label>
          <button
            className="lessonTipSubmitBtn"
            onClick={handleSubmit}
            disabled={isSubmitting || !newText.trim()}
          >
            {isSubmitting ? "Plaatsen..." : "Tip plaatsen"}
          </button>
        </div>
      </div>

      {tips.length === 0 ? (
        <p className="lessonTipsEmpty">
          Nog geen lestips voor dit boek. Wees de eerste!
        </p>
      ) : (
        <div className="lessonTipsList">
          {tips.map((tip) =>
            editingId === tip.id ? (
              <div
                key={tip.id}
                className="lessonTipCard lessonTipCard--editing"
              >
                <textarea
                  className="lessonTipTextarea"
                  value={editText}
                  onChange={(e) => setEditText(e.target.value)}
                  rows={4}
                />
                <div className="lessonTipFormFooter">
                  <label className="lessonTipAnonLabel">
                    <input
                      type="checkbox"
                      checked={editAnonymous}
                      onChange={(e) => setEditAnonymous(e.target.checked)}
                    />
                    Anoniem
                  </label>
                  <div style={{ display: "flex", gap: "0.5rem" }}>
                    <button
                      className="lessonTipCancelBtn"
                      onClick={() => setEditingId(null)}
                    >
                      Annuleren
                    </button>
                    <button
                      className="lessonTipSubmitBtn"
                      onClick={() => handleUpdate(tip.id)}
                      disabled={isSubmitting || !editText.trim()}
                    >
                      {isSubmitting ? "Opslaan..." : "Opslaan"}
                    </button>
                  </div>
                </div>
              </div>
            ) : (
              <div key={tip.id} className="lessonTipCard">
                <div className="lessonTipMeta">
                  <span className="lessonTipAuthor">
                    {tip.anonymous
                      ? "Anoniem"
                      : (tip.authorName ?? "Leerkracht")}
                  </span>
                  <span className="lessonTipDate">
                    {formatDate(tip.createdDate)}
                  </span>
                </div>
                <p className="lessonTipText">{tip.text}</p>
                {tip.ownTip && (
                  <div className="lessonTipActions">
                    <button
                      className="lessonTipEditBtn"
                      onClick={() => startEdit(tip)}
                    >
                      Bewerken
                    </button>
                    {confirmDeleteId === tip.id ? (
                      <div className="lessonTipConfirmDelete">
                        <span>Verwijderen?</span>
                        <button
                          className="lessonTipCancelBtn"
                          onClick={() => setConfirmDeleteId(null)}
                        >
                          Annuleren
                        </button>
                        <button
                          className="lessonTipDeleteBtn"
                          onClick={() => handleDelete(tip.id)}
                          disabled={deletingId === tip.id}
                        >
                          {deletingId === tip.id ? "..." : "Verwijder"}
                        </button>
                      </div>
                    ) : (
                      <button
                        className="lessonTipDeleteBtn"
                        onClick={() => setConfirmDeleteId(tip.id)}
                      >
                        Verwijderen
                      </button>
                    )}
                  </div>
                )}
              </div>
            ),
          )}
        </div>
      )}
    </div>
  );
}
