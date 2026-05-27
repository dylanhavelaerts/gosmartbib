"use client";

import ProtectedRoute from "@/app/components/ProtectedRoute";
import type { MeResponse } from "@/app/interfaces/user";
import "./librarySettings.css";
import { useEffect, useState } from "react";
import { formatSchoolLabel } from "../reviews/components/ReviewCard";

const API_URL = process.env.NEXT_PUBLIC_API_URL;
const DEFAULT_LOAN_PERIOD = 14;
const DEFAULT_EXTENSION_PERIOD = 3;
const DEFAULT_REMINDER_DAYS = 3;

export default function LibrarySettings() {
  // --- Leenbeleid States ---
  const [loanPeriod, setLoanPeriod] = useState<number>(DEFAULT_LOAN_PERIOD);
  const [extensionPeriod, setExtensionPeriod] = useState<number>(
    DEFAULT_EXTENSION_PERIOD,
  );
  const [reminderDays, setReminderDays] = useState<number>(
    DEFAULT_REMINDER_DAYS,
  );
  const [savedLoanPeriod, setSavedLoanPeriod] =
    useState<number>(DEFAULT_LOAN_PERIOD);
  const [savedExtensionPeriod, setSavedExtensionPeriod] = useState<number>(
    DEFAULT_EXTENSION_PERIOD,
  );
  const [savedReminderDays, setSavedReminderDays] = useState<number>(
    DEFAULT_REMINDER_DAYS,
  );
  // --- Homepage Weergave States ---
  const [showSpotlight, setShowSpotlight] = useState<boolean>(true);
  const [showNewInLibrary, setShowNewInLibrary] = useState<boolean>(true);
  const [showReadingLists, setShowReadingLists] = useState<boolean>(true);
  const [showUrgentLoans, setShowUrgentLoans] = useState<boolean>(true);
  const [savedShowSpotlight, setSavedShowSpotlight] = useState<boolean>(true);
  const [savedShowNewInLibrary, setSavedShowNewInLibrary] =
    useState<boolean>(true);
  const [savedShowReadingLists, setSavedShowReadingLists] =
    useState<boolean>(true);
  const [savedShowUrgentLoans, setSavedShowUrgentLoans] =
    useState<boolean>(true);

  // --- Bibliotheekfuncties States ---
  const [barcodesEnabled, setBarcodesEnabled] = useState<boolean>(false);
  const [savedBarcodesEnabled, setSavedBarcodesEnabled] =
    useState<boolean>(false);

  // --- Algemene States ---
  const [me, setMe] = useState<MeResponse | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const schoolId = me?.school?.id ?? null;
  const schoolName = me?.school?.name ?? null;
  const schoolDomain = me?.school?.domain ?? null;

  // --- berichten ---
  const [senderIdentifier, setSenderIdentifier] = useState("");
  const [savedSenderIdentifier, setSavedSenderIdentifier] = useState("");

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

        const [policyResponse, settingsResponse, librarySettingsResponse] =
          await Promise.all([
            fetch(`${API_URL}/admin/schools/${meData.school.id}/loan-policy`, {
              credentials: "include",
            }),
            fetch(
              `${API_URL}/admin/schools/${meData.school.id}/homepage-settings`,
              { credentials: "include" },
            ),
            fetch(
              `${API_URL}/admin/schools/${meData.school.id}/library-settings`,
              { credentials: "include" },
            ),
          ]);

        // Verwerk Leenbeleid
        if (policyResponse.status === 404) {
          setLoanPeriod(DEFAULT_LOAN_PERIOD);
          setExtensionPeriod(DEFAULT_EXTENSION_PERIOD);
          setReminderDays(DEFAULT_REMINDER_DAYS);
          setSavedLoanPeriod(DEFAULT_LOAN_PERIOD);
          setSavedExtensionPeriod(DEFAULT_EXTENSION_PERIOD);
          setSavedReminderDays(DEFAULT_REMINDER_DAYS);
        } else if (policyResponse.ok) {
          const data = await policyResponse.json();
          setLoanPeriod(data.defaultLoanPeriodDays);
          setExtensionPeriod(data.defaultExtensionPeriodDays);
          setReminderDays(data.dueDateReminderDays);
          setSavedLoanPeriod(data.defaultLoanPeriodDays);
          setSavedExtensionPeriod(data.defaultExtensionPeriodDays);
          setSavedReminderDays(data.dueDateReminderDays);
        } else {
          throw new Error(
            `Fout bij ophalen leenbeleid: HTTP ${policyResponse.status}`,
          );
        }

        // Verwerk Homepage Instellingen
        if (settingsResponse.ok) {
          const settingsData = await settingsResponse.json();
          setShowSpotlight(settingsData.showSpotlight ?? true);
          setShowNewInLibrary(settingsData.showNewInLibrary ?? true);
          setShowReadingLists(settingsData.showReadingLists ?? true);
          setShowUrgentLoans(settingsData.showUrgentLoans ?? true);
          setSavedShowSpotlight(settingsData.showSpotlight ?? true);
          setSavedShowNewInLibrary(settingsData.showNewInLibrary ?? true);
          setSavedShowReadingLists(settingsData.showReadingLists ?? true);
          setSavedShowUrgentLoans(settingsData.showUrgentLoans ?? true);
          setSenderIdentifier(settingsData.smartschoolSenderIdentifier ?? "");
          setSavedSenderIdentifier(
            settingsData.smartschoolSenderIdentifier ?? "",
          );
        }

        // Verwerk Bibliotheekfuncties
        if (librarySettingsResponse.ok) {
          const libData = await librarySettingsResponse.json();
          setBarcodesEnabled(libData.barcodesEnabled ?? false);
          setSavedBarcodesEnabled(libData.barcodesEnabled ?? false);
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
    if (loanPeriod < 1 || extensionPeriod < 1 || reminderDays < 1) {
      setError("Periodes moeten minimaal 1 dag zijn");
      return;
    }

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      // Start beide opslag-acties tegelijkertijd
      const policyPromise = fetch(
        `${API_URL}/admin/schools/${schoolId}/loan-policy`,
        {
          method: "PUT",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            defaultLoanPeriodDays: loanPeriod,
            defaultExtensionPeriodDays: extensionPeriod,
            dueDateReminderDays: reminderDays,
          }),
        },
      );

      const settingsPromise = fetch(
        `${API_URL}/admin/schools/${schoolId}/homepage-settings`,
        {
          method: "PUT",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            showSpotlight,
            showNewInLibrary,
            showReadingLists,
            showUrgentLoans,
            smartschoolSenderIdentifier: senderIdentifier,
          }),
        },
      );

      const librarySettingsPromise = fetch(
        `${API_URL}/admin/schools/${schoolId}/library-settings`,
        {
          method: "PUT",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ barcodesEnabled }),
        },
      );

      const [policyRes, settingsRes, librarySettingsRes] = await Promise.all([
        policyPromise,
        settingsPromise,
        librarySettingsPromise,
      ]);

      if (!policyRes.ok) {
        const body = await policyRes.text();
        throw new Error(body || "Opslaan van leenbeleid mislukt");
      }
      if (!settingsRes.ok) {
        const body = await settingsRes.text();
        throw new Error(body || "Opslaan van weergave-instellingen mislukt");
      }
      if (!librarySettingsRes.ok) {
        const body = await librarySettingsRes.text();
        throw new Error(body || "Opslaan van bibliotheekfuncties mislukt");
      }

      const savedPolicy = await policyRes.json();
      setLoanPeriod(savedPolicy.defaultLoanPeriodDays);
      setExtensionPeriod(savedPolicy.defaultExtensionPeriodDays);
      setReminderDays(savedPolicy.dueDateReminderDays);
      setSavedLoanPeriod(savedPolicy.defaultLoanPeriodDays);
      setSavedExtensionPeriod(savedPolicy.defaultExtensionPeriodDays);
      setSavedReminderDays(savedPolicy.dueDateReminderDays);

      const savedSettings = await settingsRes.json();
      setShowSpotlight(savedSettings.showSpotlight);
      setShowNewInLibrary(savedSettings.showNewInLibrary);
      setShowReadingLists(savedSettings.showReadingLists);
      setShowUrgentLoans(savedSettings.showUrgentLoans);
      setSavedShowSpotlight(savedSettings.showSpotlight);
      setSavedShowNewInLibrary(savedSettings.showNewInLibrary);
      setSavedShowReadingLists(savedSettings.showReadingLists);
      setSavedShowUrgentLoans(savedSettings.showUrgentLoans);

      const savedLibSettings = await librarySettingsRes.json();
      setBarcodesEnabled(savedLibSettings.barcodesEnabled);
      setSavedBarcodesEnabled(savedLibSettings.barcodesEnabled);
      setSenderIdentifier(savedSettings.smartschoolSenderIdentifier ?? "");
      setSavedSenderIdentifier(savedSettings.smartschoolSenderIdentifier ?? "");

      setSuccess("Instellingen succesvol opgeslagen");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Er ging iets mis bij het opslaan",
      );
    } finally {
      setSaving(false);
    }
  };

  // Een hulpmiddel voor de styling van de mooie checkbox rijen
  const checkboxRowStyle: React.CSSProperties = {
    display: "flex",
    alignItems: "center",
    gap: "1rem",
    cursor: "pointer",
    padding: "0.85rem 1rem",
    backgroundColor: "#f9fafb",
    border: "1px solid #e5e7eb",
    borderRadius: "0.5rem",
    transition: "background-color 0.2s ease",
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
            <div
              className="leftColumnSettings"
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "1.5rem",
                flex: 1,
              }}
            >
              {/* --- LEENBELEID CARD --- */}
              <section className="card">
                <h2>Leenbeleid</h2>
                <p className="help">
                  Stel de standaard termijnen in voor uitleningen en
                  verlengingen. Deze waarden worden gebruikt voor alle nieuwe
                  uitleningen tenzij anders opgegeven.
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

                  <label className="field">
                    <span>Herinneringsperiode</span>
                    <div className="inputWithUnit">
                      <input
                        type="number"
                        value={reminderDays}
                        onChange={(e) =>
                          setReminderDays(Number(e.target.value))
                        }
                        disabled={saving}
                      />
                      <span className="unitLabel">dagen</span>
                    </div>
                  </label>
                </div>
              </section>
              {/* --- BERICHTEN CARD --- */}
              <section className="card">
                <h2>Berichten</h2>
                <p className="help">
                  De naam die getoond wordt als afzender bij automatische
                  Smartschool-berichten. Laat leeg om als je dit niet wilt
                  gebruiken.
                </p>
                <div className="form">
                  <label className="field">
                    <span>Afzendernaam</span>
                    <input
                      type="text"
                      value={senderIdentifier}
                      onChange={(e) => setSenderIdentifier(e.target.value)}
                      placeholder="bv. jan.janssens"
                      disabled={saving}
                    />
                  </label>
                </div>
              </section>
              {/* --- HOMEPAGE WEERGAVE CARD --- */}
              <section className="card">
                <h2>Weergave Homepage</h2>
                <p className="help">
                  Kies welke elementen standaard zichtbaar zijn voor leerlingen
                  op de homepage van jouw school.
                </p>

                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.75rem",
                    marginTop: "1rem",
                  }}
                >
                  <label
                    style={checkboxRowStyle}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f3f4f6")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f9fafb")
                    }
                  >
                    <input
                      type="checkbox"
                      style={{
                        width: "1.25rem",
                        height: "1.25rem",
                        margin: 0,
                        cursor: "pointer",
                        accentColor: "var(--accent-color, #2563eb)",
                      }}
                      checked={showSpotlight}
                      onChange={(e) => setShowSpotlight(e.target.checked)}
                      disabled={saving}
                    />
                    <span
                      style={{
                        fontWeight: 500,
                        color: "#374151",
                        userSelect: "none",
                      }}
                    >
                      "In de kijker" tabblad tonen
                    </span>
                  </label>

                  <label
                    style={checkboxRowStyle}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f3f4f6")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f9fafb")
                    }
                  >
                    <input
                      type="checkbox"
                      style={{
                        width: "1.25rem",
                        height: "1.25rem",
                        margin: 0,
                        cursor: "pointer",
                        accentColor: "var(--accent-color, #2563eb)",
                      }}
                      checked={showNewInLibrary}
                      onChange={(e) => setShowNewInLibrary(e.target.checked)}
                      disabled={saving}
                    />
                    <span
                      style={{
                        fontWeight: 500,
                        color: "#374151",
                        userSelect: "none",
                      }}
                    >
                      "Nieuw in bibliotheek" tabblad tonen
                    </span>
                  </label>

                  <label
                    style={checkboxRowStyle}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f3f4f6")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f9fafb")
                    }
                  >
                    <input
                      type="checkbox"
                      style={{
                        width: "1.25rem",
                        height: "1.25rem",
                        margin: 0,
                        cursor: "pointer",
                        accentColor: "var(--accent-color, #2563eb)",
                      }}
                      checked={showReadingLists}
                      onChange={(e) => setShowReadingLists(e.target.checked)}
                      disabled={saving}
                    />
                    <span
                      style={{
                        fontWeight: 500,
                        color: "#374151",
                        userSelect: "none",
                      }}
                    >
                      Zijbalk met "Jouw leeslijsten" tonen
                    </span>
                  </label>

                  <label
                    style={checkboxRowStyle}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f3f4f6")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f9fafb")
                    }
                  >
                    <input
                      type="checkbox"
                      style={{
                        width: "1.25rem",
                        height: "1.25rem",
                        margin: 0,
                        cursor: "pointer",
                        accentColor: "var(--accent-color, #2563eb)",
                      }}
                      checked={showUrgentLoans}
                      onChange={(e) => setShowUrgentLoans(e.target.checked)}
                      disabled={saving}
                    />
                    <span
                      style={{
                        fontWeight: 500,
                        color: "#374151",
                        userSelect: "none",
                      }}
                    >
                      "Terug te brengen" tabblad tonen (indien items te laat
                      zijn)
                    </span>
                  </label>
                </div>
              </section>

              {/* --- BIBLIOTHEEKFUNCTIES CARD --- */}
              <section className="card">
                <h2>Bibliotheekfuncties</h2>
                <p className="help">
                  Schakel extra functionaliteiten in of uit voor jouw
                  bibliotheek.
                </p>
                <div
                  style={{
                    display: "flex",
                    flexDirection: "column",
                    gap: "0.75rem",
                    marginTop: "1rem",
                  }}
                >
                  <label
                    style={checkboxRowStyle}
                    onMouseOver={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f3f4f6")
                    }
                    onMouseOut={(e) =>
                      (e.currentTarget.style.backgroundColor = "#f9fafb")
                    }
                  >
                    <input
                      type="checkbox"
                      style={{
                        width: "1.25rem",
                        height: "1.25rem",
                        margin: 0,
                        cursor: "pointer",
                        accentColor: "var(--accent-color, #2563eb)",
                      }}
                      checked={barcodesEnabled}
                      onChange={(e) => setBarcodesEnabled(e.target.checked)}
                      disabled={saving}
                    />
                    <span
                      style={{
                        fontWeight: 500,
                        color: "#374151",
                        userSelect: "none",
                      }}
                    >
                      Streepjescodes inschakelen voor exemplaren
                    </span>
                  </label>
                </div>
              </section>

              {/* --- OPSLAAN KNOP --- */}
              <div className="actions" style={{ marginTop: "0.5rem" }}>
                <button
                  className="button primaryButton"
                  onClick={handleSave}
                  disabled={saving}
                >
                  {saving ? "Opslaan..." : "Opslaan"}
                </button>
              </div>
            </div>

            <section
              className="card"
              style={{ alignSelf: "flex-start", flex: "0 0 350px" }}
            >
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

              <div className="infoPanel" style={{ marginBottom: "0.75rem" }}>
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
                <div className="infoRow">
                  <span className="infoLabel">Herinneringsperiode</span>
                  <strong className="infoValue">
                    {savedReminderDays} dagen
                  </strong>
                </div>
                <div className="infoPanel">
                  <p className="infoPanelTitle">Berichten</p>
                  <div className="infoRow">
                    <span className="infoLabel">Afzender</span>
                    <strong className="infoValue">
                      {savedSenderIdentifier || "—"}
                    </strong>
                  </div>
                </div>
              </div>

              <div className="infoPanel" style={{ marginBottom: "0.75rem" }}>
                <p className="infoPanelTitle">Bibliotheekfuncties</p>
                <div className="infoRow">
                  <span className="infoLabel">Streepjescodes</span>
                  <strong className="infoValue">
                    {savedBarcodesEnabled ? "Ingeschakeld" : "Uitgeschakeld"}
                  </strong>
                </div>
              </div>

              <div className="infoPanel">
                <p className="infoPanelTitle">Weergave Homepage</p>
                <div className="infoRow">
                  <span className="infoLabel">In de kijker</span>
                  <strong className="infoValue">
                    {savedShowSpotlight ? "Zichtbaar" : "Verborgen"}
                  </strong>
                </div>
                <div className="infoRow">
                  <span className="infoLabel">Nieuw in bibliotheek</span>
                  <strong className="infoValue">
                    {savedShowNewInLibrary ? "Zichtbaar" : "Verborgen"}
                  </strong>
                </div>
                <div className="infoRow">
                  <span className="infoLabel">Leeslijsten</span>
                  <strong className="infoValue">
                    {savedShowReadingLists ? "Zichtbaar" : "Verborgen"}
                  </strong>
                </div>
                <div className="infoRow">
                  <span className="infoLabel">Terug te brengen</span>
                  <strong className="infoValue">
                    {savedShowUrgentLoans ? "Zichtbaar" : "Verborgen"}
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
