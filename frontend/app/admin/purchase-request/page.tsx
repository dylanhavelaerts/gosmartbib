"use client";

import "./purchase.css";
import SuggestionCard from "./component/card";
import ActionModal from "./component/ActionModal";
import ProtectedRoute from "@/app/components/ProtectedRoute";
import Pagination from "@/app/catalog/pagination";
import { useAuth } from "@/app/context/AuthContext";
import { useMemo, useState } from "react";
import { usePurchaseRequests } from "./usePurchaseRequests";

const API_URL = process.env.NEXT_PUBLIC_API_URL;
const REQUESTS_PER_PAGE = 10;

type FilterStatus = "all" | "PENDING" | "APPROVED" | "REJECTED";
type SortOption = "newest" | "oldest";
type ActiveTab = "form" | "overview";

export default function PurchaseRequestPage() {
  const { user } = useAuth();
  const isBeheerder = user?.role === "BIBLIOTHEEKBEHEERDER";

  const [activeTab, setActiveTab] = useState<ActiveTab>("form");

  const [formTitle, setFormTitle] = useState("");
  const [formAuthors, setFormAuthors] = useState<string[]>([""]);
  const [formIsbn, setFormIsbn] = useState("");
  const [formError, setFormError] = useState("");
  const [formSuccess, setFormSuccess] = useState(false);

  const [filterStatus, setFilterStatus] = useState<FilterStatus>("all");
  const [sortOption, setSortOption] = useState<SortOption>("newest");
  const [currentPage, setCurrentPage] = useState(1);

  const [modalAction, setModalAction] = useState<"approve" | "reject" | null>(
    null,
  );
  const [modalRequestId, setModalRequestId] = useState<number | null>(null);
  const [modalNote, setModalNote] = useState("");

  const {
    requests,
    loading,
    error,
    actionError,
    isSubmitting,
    approvingId,
    rejectingId,
    setActionError,
    createRequest,
    approveRequest,
    rejectRequest,
  } = usePurchaseRequests(API_URL);

  function addAuthor() {
    setFormAuthors((prev) => [...prev, ""]);
  }

  function removeAuthor(index: number) {
    setFormAuthors((prev) => prev.filter((_, i) => i !== index));
  }

  function updateAuthor(index: number, value: string) {
    setFormAuthors((prev) => prev.map((a, i) => (i === index ? value : a)));
  }

  async function handleSubmitForm(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError("");
    setFormSuccess(false);

    if (!formTitle.trim()) {
      setFormError("Vul een titel in.");
      return;
    }

    const success = await createRequest({
      title: formTitle,
      authors: formAuthors,
      isbn: formIsbn,
    });

    if (success) {
      setFormTitle("");
      setFormAuthors([""]);
      setFormIsbn("");
      setFormSuccess(true);
    } else {
      setFormError(actionError || "Er ging iets mis.");
    }
  }

  function openModal(action: "approve" | "reject", id: number) {
    setModalAction(action);
    setModalRequestId(id);
    setModalNote("");
    setActionError("");
  }

  function closeModal() {
    if (approvingId !== null || rejectingId !== null) return;
    setModalAction(null);
    setModalRequestId(null);
    setModalNote("");
  }

  async function submitModal() {
    if (modalRequestId === null || modalAction === null) return;
    const success =
      modalAction === "approve"
        ? await approveRequest(modalRequestId, modalNote)
        : await rejectRequest(modalRequestId, modalNote);
    if (success) closeModal();
  }

  const filteredAndSorted = useMemo(() => {
    const filtered = requests.filter((r) =>
      filterStatus === "all" ? true : r.status === filterStatus,
    );
    return [...filtered].sort((a, b) => {
      const left = new Date(a.requestDate).getTime();
      const right = new Date(b.requestDate).getTime();
      return sortOption === "newest" ? right - left : left - right;
    });
  }, [requests, filterStatus, sortOption]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredAndSorted.length / REQUESTS_PER_PAGE),
  );
  const paginatedRequests = filteredAndSorted.slice(
    (currentPage - 1) * REQUESTS_PER_PAGE,
    currentPage * REQUESTS_PER_PAGE,
  );

  const filters: { key: FilterStatus; label: string }[] = [
    { key: "all", label: "Alle" },
    { key: "PENDING", label: "In behandeling" },
    { key: "APPROVED", label: "Goedgekeurd" },
    { key: "REJECTED", label: "Afgekeurd" },
  ];

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "BIBLIOTHEEKBEHEERDER"]}>
      <main className="purchase-page">
        <h1 className="purchase-title">Aankoopsuggesties</h1>
        <p className="purchase-subtitle">
          Stel een aankoopsuggestie in voor een nieuw boek of bekijk de
          ingediende suggesties.
        </p>

        <div className="tab-bar">
          <button
            className={`tab-btn ${activeTab === "form" ? "active" : ""}`}
            onClick={() => setActiveTab("form")}
          >
            Suggestie indienen
          </button>
          <button
            className={`tab-btn ${activeTab === "overview" ? "active" : ""}`}
            onClick={() => setActiveTab("overview")}
          >
            Overzicht
          </button>
        </div>

        {activeTab === "form" && (
          <section className="form-section">
            <form className="purchase-form" onSubmit={handleSubmitForm}>
              <div className="form-group">
                <label className="form-label" htmlFor="pr-title">
                  Titel *
                </label>
                <input
                  id="pr-title"
                  className="form-input"
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="Bijv. De ontdekking van de hemel"
                  disabled={isSubmitting}
                />
              </div>

              <div className="form-group">
                <label className="form-label">Auteurs</label>
                {formAuthors.map((author, index) => (
                  <div key={index} className="author-row">
                    <input
                      className="form-input"
                      type="text"
                      value={author}
                      onChange={(e) => updateAuthor(index, e.target.value)}
                      placeholder={`Auteur ${index + 1}`}
                      disabled={isSubmitting}
                    />
                    {formAuthors.length > 1 && (
                      <button
                        type="button"
                        className="author-remove-btn"
                        onClick={() => removeAuthor(index)}
                        disabled={isSubmitting}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                ))}
                <button
                  type="button"
                  className="author-add-btn"
                  onClick={addAuthor}
                  disabled={isSubmitting}
                >
                  + Auteur toevoegen
                </button>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="pr-isbn">
                  ISBN (optioneel)
                </label>
                <input
                  id="pr-isbn"
                  className="form-input"
                  type="text"
                  value={formIsbn}
                  onChange={(e) => setFormIsbn(e.target.value)}
                  placeholder="Bijv. 978-90-254-3567-7"
                  disabled={isSubmitting}
                />
              </div>

              {formError && <p className="form-error">{formError}</p>}
              {formSuccess && (
                <p className="form-success">
                  Je aankoopsuggestie is succesvol ingediend!
                </p>
              )}

              <button
                type="submit"
                className="form-submit-btn"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Bezig..." : "Suggestie indienen"}
              </button>
            </form>
          </section>
        )}

        {activeTab === "overview" && (
          <section className="overview-section">
            <div className="overview-toolbar">
              <div className="overview-filters">
                <span className="filter-label">Filter:</span>
                {filters.map((f) => (
                  <button
                    key={f.key}
                    className={`filter-chip ${filterStatus === f.key ? "active" : ""}`}
                    onClick={() => {
                      setFilterStatus(f.key);
                      setCurrentPage(1);
                    }}
                  >
                    {f.label}
                  </button>
                ))}
              </div>
              <div className="sort-bar">
                <label htmlFor="pr-sort">Sorteer:</label>
                <select
                  id="pr-sort"
                  value={sortOption}
                  onChange={(e) => {
                    setSortOption(e.target.value as SortOption);
                    setCurrentPage(1);
                  }}
                >
                  <option value="newest">Nieuwste eerst</option>
                  <option value="oldest">Oudste eerst</option>
                </select>
              </div>
            </div>

            {loading && (
              <p className="overview-info">Aankoopverzoeken laden...</p>
            )}
            {!loading && error && <p className="overview-error">{error}</p>}
            {!loading && !error && actionError && (
              <p className="overview-error">{actionError}</p>
            )}
            {!loading && !error && filteredAndSorted.length === 0 && (
              <p className="overview-info">Geen aankoopverzoeken gevonden.</p>
            )}

            {!loading && !error && filteredAndSorted.length > 0 && (
              <>
                <div className="card-list">
                  {paginatedRequests.map((request) => (
                    <SuggestionCard
                      key={request.id}
                      request={request}
                      isBeheerder={isBeheerder}
                      approving={approvingId === request.id}
                      rejecting={rejectingId === request.id}
                      onApprove={(id) => openModal("approve", id)}
                      onReject={(id) => openModal("reject", id)}
                    />
                  ))}
                </div>
                {filteredAndSorted.length > REQUESTS_PER_PAGE && (
                  <div className="overview-pagination">
                    <Pagination
                      currentPage={currentPage}
                      totalPages={totalPages}
                      onPageChange={setCurrentPage}
                    />
                  </div>
                )}
              </>
            )}
          </section>
        )}

        <ActionModal
          isOpen={modalAction !== null}
          action={modalAction ?? "approve"}
          note={modalNote}
          isSubmitting={approvingId !== null || rejectingId !== null}
          onNoteChange={setModalNote}
          onCancel={closeModal}
          onSubmit={submitModal}
        />
      </main>
    </ProtectedRoute>
  );
}
