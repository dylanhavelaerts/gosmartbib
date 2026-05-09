"use client";

import ProtectedRoute from "@/app/components/ProtectedRoute";
import type { MeResponse } from "@/app/interfaces/user";
import "./librarySettings.css";
import { useEffect, useState } from "react";
import { formatSchoolLabel } from "../reviews/components/ReviewCard";

const API_URL = process.env.NEXT_PUBLIC_API_URL;
const DEFAULT_LOAN_PERIOD = 14;
const DEFAULT_EXTENSION_PERIOD = 3;

export default function LibrarySettings() {
  const [loanPeriod, setLoanPeriod] = useState<number>(DEFAULT_LOAN_PERIOD);
  const [extensionPeriod, setExtensionPeriod] = useState<number>(
    DEFAULT_EXTENSION_PERIOD,
  );
  const [savedLoanPeriod, setSavedLoanPeriod] =
    useState<number>(DEFAULT_LOAN_PERIOD);
  const [savedExtensionPeriod, setSavedExtensionPeriod] = useState<number>(
    DEFAULT_EXTENSION_PERIOD,
  );

  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const schoolId = me?.school?.id ?? null;
  const schoolName = me?.school?.name ?? null;
  const schoolDomain = me?.school?.domain ?? null;

  useEffect(() => {
    const loadSchoolData = async () => {
      if (!API_URL) {
        setError("API URL is niet ingesteld");
        setLoading(false);
        return;
      }

      try {
        setError("");
        setSuccess("");

        const meResponse = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          setError("Je bent niet ingelogd");
          setLoading(false);
          return;
        }

        const meData: MeResponse = await meResponse.json();
        setMe(meData);

        if (!meData.school?.id) {
          setError("Geen school gevonden voor deze gebruiker");
          setLoading(false);
          return;
        }

        const policyResponse = await fetch(
          `${API_URL}/admin/schools/${meData.school.id}/loan-policy`,
          { credentials: "include" },
        );

        if (policyResponse.status === 404) {
          setLoanPeriod(DEFAULT_LOAN_PERIOD);
          setExtensionPeriod(DEFAULT_EXTENSION_PERIOD);
          setSavedLoanPeriod(DEFAULT_LOAN_PERIOD);
          setSavedExtensionPeriod(DEFAULT_EXTENSION_PERIOD);
        } else if (policyResponse.ok) {
          const data = await policyResponse.json();
          setLoanPeriod(data.defaultLoanPeriodDays);
          setExtensionPeriod(data.defaultExtensionPeriodDays);
          setSavedLoanPeriod(data.defaultLoanPeriodDays);
          setSavedExtensionPeriod(data.defaultExtensionPeriodDays);
        } else {
          throw new Error(`HTTP ${policyResponse.status}`);
        }
      } catch {
        setError("Er is een fout opgetreden bij het laden van de gegevens");
      } finally {
        setLoading(false);
      }
    };

    loadSchoolData();
  }, []);

  const handleSave = async () => {
    if (!API_URL || !schoolId) return;
    if (loanPeriod < 1 || extensionPeriod < 1) {
      setError("Periodes moeten minimaal 1 dag zijn");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/loan-policy`,
        {
          method: "PUT",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            defaultLoanPeriodDays: loanPeriod,
            defaultExtensionPeriodDays: extensionPeriod,
          }),
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Opslaan mislukt");
      }

      const saved = await response.json();
      setLoanPeriod(saved.defaultLoanPeriodDays);
      setExtensionPeriod(saved.defaultExtensionPeriodDays);
      setSavedLoanPeriod(saved.defaultLoanPeriodDays);
      setSavedExtensionPeriod(saved.defaultExtensionPeriodDays);
      setSuccess("Instellingen succesvol opgeslagen");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Er ging iets mis bij het opslaan",
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <ProtectedRoute allowedRoles={["BIBLIOTHEEKBEHEERDER"]}>
      <main className="page">
        <div className="pageHeader">
          <div>
            <h1>Bibliotheekinstellingen</h1>
            {schoolName && (
              <p className="pageSubtitle">
                {formatSchoolLabel(schoolName)}
                {schoolDomain && schoolDomain !== schoolName
                  ? ` — ${schoolDomain}`
                  : ""}
              </p>
            )}
          </div>
        </div>

        {error && <p className="message error">{error}</p>}
        {success && <p className="message success">{success}</p>}

        {loading ? (
          <p className="loadingText">Gegevens laden...</p>
        ) : (
          <div className="pageColumns">
            <section className="card">
              <h2>Leenbeleid</h2>
              <p className="help">
                Stel de standaard termijnen in voor uitleningen en verlengingen.
                Deze waarden worden gebruikt voor alle nieuwe uitleningen tenzij
                anders opgegeven.
              </p>

              <div className="form">
                <label className="field">
                  <span>Uitleenperiode</span>
                  <div className="inputWithUnit">
                    <input
                      type="number"
                      min={1}
                      max={365}
                      value={loanPeriod}
                      onChange={(e) => setLoanPeriod(Number(e.target.value))}
                      disabled={saving}
                    />
                    <span className="unitLabel">dagen</span>
                  </div>
                </label>

                <label className="field">
                  <span>Verlengingsperiode</span>
                  <div className="inputWithUnit">
                    <input
                      type="number"
                      min={1}
                      max={365}
                      value={extensionPeriod}
                      onChange={(e) =>
                        setExtensionPeriod(Number(e.target.value))
                      }
                      disabled={saving}
                    />
                    <span className="unitLabel">dagen</span>
                  </div>
                </label>
              </div>

              <div className="actions">
                <button
                  className="button primaryButton"
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving ? "Opslaan..." : "Opslaan"}
                </button>
              </div>
            </section>

            <section className="card">
              <h2>Schooloverzicht</h2>
              <p className="help">Huidige opgeslagen instellingen</p>

              <div className="infoPanel" style={{ marginBottom: "0.75rem" }}>
                <p className="infoPanelTitle">School</p>
                <div className="infoRow">
                  <span className="infoLabel">Naam</span>
                  <strong className="infoValue">
                    {schoolName ? formatSchoolLabel(schoolName) : "—"}
                  </strong>
                </div>
                <div className="infoRow">
                  <span className="infoLabel">Domein</span>
                  <strong className="infoValue">{schoolDomain ?? "—"}</strong>
                </div>
              </div>

              <div className="infoPanel">
                <p className="infoPanelTitle">Leenbeleid</p>
                <div className="infoRow">
                  <span className="infoLabel">Uitleenperiode</span>
                  <strong className="infoValue">{savedLoanPeriod} dagen</strong>
                </div>
                <div className="infoRow">
                  <span className="infoLabel">Verlengingsperiode</span>
                  <strong className="infoValue">
                    {savedExtensionPeriod} dagen
                  </strong>
                </div>
              </div>
            </section>
          </div>
        )}
      </main>
    </ProtectedRoute>
  );
}
