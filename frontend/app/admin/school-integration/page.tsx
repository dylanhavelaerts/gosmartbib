"use client";

import type { MeResponse } from "@/app/interfaces/user";
import type {
  SchoolIntegrationDTO,
  UpsertSchoolIntegrationRequest,
  SchoolIntegrationTestResponse,
  SchoolIntegrationLiveUsersResponse,
  SchoolIntegrationLiveClassesResponse,
  SchoolCampusDTO,
} from "@/app/interfaces/schoolIntegration";
import { fetchSchoolCampuses } from "@/app/utils/schoolCampuses";
import { useEffect, useMemo, useState } from "react";
import "./schoolIntegration.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

function formatDate(value?: string | null) {
  if (!value) return "-";

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;

  return parsed.toLocaleString("nl-BE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function SchoolIntegrationPage() {
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [loadingClasses, setLoadingClasses] = useState(false);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [savingCampus, setSavingCampus] = useState(false);
  const [deletingCampusId, setDeletingCampusId] = useState<number | null>(null);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [me, setMe] = useState<MeResponse | null>(null);
  const [integration, setIntegration] = useState<SchoolIntegrationDTO | null>(
    null,
  );

  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [newCampusName, setNewCampusName] = useState("");

  const [baseUrl, setBaseUrl] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [enabled, setEnabled] = useState(false);

  const [testResult, setTestResult] =
    useState<SchoolIntegrationTestResponse | null>(null);
  const [liveUsers, setLiveUsers] =
    useState<SchoolIntegrationLiveUsersResponse | null>(null);
  const [liveClasses, setLiveClasses] =
    useState<SchoolIntegrationLiveClassesResponse | null>(null);

  const [showAllUsers, setShowAllUsers] = useState(false);
  const [showAllClasses, setShowAllClasses] = useState(false);

  const schoolId = me?.school?.id ?? null;
  const schoolName = me?.school?.name ?? "";
  const schoolDomain = me?.school?.domain ?? "";

  const [smartschoolAccesscode, setSmartschoolAccesscode] = useState("");
  const [senderIdentifier, setSenderIdentifier] = useState("");

  const canUsePage = useMemo(() => {
    return me?.role === "ADMIN" && !!schoolId;
  }, [me, schoolId]);

  useEffect(() => {
    const load = async () => {
      if (!API_URL) {
        setError("NEXT_PUBLIC_API_URL ontbreekt");
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

        if (meData.role !== "ADMIN") {
          setError("Je hebt geen toegang tot deze pagina");
          setLoading(false);
          return;
        }

        if (!meData.school?.id) {
          setError("Geen school gevonden voor de ingelogde gebruiker");
          setLoading(false);
          return;
        }

        await Promise.all([
          loadIntegration(meData.school.id),
          loadCampuses(meData.school.id),
        ]);
      } catch (err) {
        console.error(err);
        setError("Er ging iets mis bij het laden van de integratie");
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  const loadIntegration = async (targetSchoolId: number) => {
    if (!API_URL) return;

    try {
      const response = await fetch(
        `${API_URL}/admin/schools/${targetSchoolId}/integration`,
        {
          credentials: "include",
        },
      );

      if (response.status === 404) {
        setIntegration(null);
        setBaseUrl("");
        setClientId("");
        setClientSecret("");
        setSmartschoolAccesscode(""); //altijd leeg na het laden als successvol
        setSenderIdentifier("");
        setEnabled(false);
        return;
      }

      if (!response.ok) {
        const body = await response.text();
        throw new Error(`HTTP ${response.status}: ${body}`);
      }

      const data: SchoolIntegrationDTO = await response.json();
      setIntegration(data);
      setBaseUrl(data.onerosterBaseUrl ?? "");
      setClientId(data.onerosterClientId ?? "");
      setClientSecret("");
      setSmartschoolAccesscode(""); //altijd leeg na het laden als successvol
      setSenderIdentifier(data.smartschoolSenderIdentifier ?? ""); //?
      setEnabled(Boolean(data.onerosterEnabled));
    } catch (err) {
      console.error(err);
      setError("Kon de schoolintegratie niet ophalen");
    }
  };

  const loadCampuses = async (targetSchoolId: number) => {
    if (!API_URL) return;

    try {
      setLoadingCampuses(true);
      const data = await fetchSchoolCampuses(API_URL, targetSchoolId);
      setCampuses(data);
    } catch (err) {
      console.error(err);
      setError("Kon de campussen niet ophalen");
    } finally {
      setLoadingCampuses(false);
    }
  };

  const handleCreateCampus = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!API_URL || !schoolId) return;

    const trimmedName = newCampusName.trim();
    if (!trimmedName) {
      setError("Campusnaam mag niet leeg zijn");
      return;
    }

    try {
      setSavingCampus(true);
      setError("");
      setSuccess("");

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/campuses`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ name: trimmedName }),
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Campus toevoegen mislukt");
      }

      const createdCampus: SchoolCampusDTO = await response.json();
      setCampuses((prev) => [...prev, createdCampus]);
      setNewCampusName("");
      setSuccess("Campus succesvol toegevoegd");
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : "Er ging iets mis bij het toevoegen van de campus",
      );
    } finally {
      setSavingCampus(false);
    }
  };

  const handleDeleteCampus = async (campusId: number) => {
    if (!API_URL || !schoolId) return;

    try {
      setDeletingCampusId(campusId);
      setError("");
      setSuccess("");

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/campuses/${campusId}`,
        {
          method: "DELETE",
          credentials: "include",
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Campus verwijderen mislukt");
      }

      setCampuses((prev) => prev.filter((campus) => campus.id !== campusId));
      setSuccess("Campus succesvol verwijderd");
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : "Er ging iets mis bij het verwijderen van de campus",
      );
    } finally {
      setDeletingCampusId(null);
    }
  };

  const handleSave = async () => {
    if (!API_URL || !schoolId) return;

    try {
      setSaving(true);
      setError("");
      setSuccess("");

      const payload: UpsertSchoolIntegrationRequest = {
        onerosterBaseUrl: baseUrl.trim(),
        onerosterClientId: clientId.trim(),
        onerosterClientSecret: clientSecret,
        onerosterEnabled: enabled,
        smartschoolAccesscode: smartschoolAccesscode,
        smartschoolSenderIdentifier: senderIdentifier,
      };

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/integration`,
        {
          method: "PUT",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Opslaan mislukt");
      }

      const data: SchoolIntegrationDTO = await response.json();
      setIntegration(data);
      setBaseUrl(data.onerosterBaseUrl ?? "");
      setClientId(data.onerosterClientId ?? "");
      setEnabled(Boolean(data.onerosterEnabled));
      setClientSecret("");
      setSmartschoolAccesscode("");
      setSuccess("Integratie succesvol opgeslagen");
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error ? err.message : "Er ging iets mis bij het opslaan",
      );
    } finally {
      setSaving(false);
    }
  };

  const handleTest = async () => {
    if (!API_URL || !schoolId) return;

    try {
      setTesting(true);
      setError("");
      setSuccess("");
      setTestResult(null);

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/integration/test`,
        {
          method: "POST",
          credentials: "include",
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Test mislukt");
      }

      const data: SchoolIntegrationTestResponse = await response.json();
      setTestResult(data);

      if (data.success) {
        setSuccess("Test succesvol uitgevoerd");
      } else {
        setError(data.message || "De test is mislukt");
      }

      await loadIntegration(schoolId);
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error ? err.message : "Er ging iets mis bij het testen",
      );
    } finally {
      setTesting(false);
    }
  };

  const handleLoadLiveUsers = async () => {
    if (!API_URL || !schoolId) return;

    try {
      setLoadingUsers(true);
      setError("");
      setShowAllUsers(false);
      setLiveUsers(null);

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/integration/live/users`,
        {
          credentials: "include",
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Live users ophalen mislukt");
      }

      const data: SchoolIntegrationLiveUsersResponse = await response.json();
      setLiveUsers(data);
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : "Er ging iets mis bij het ophalen van live users",
      );
    } finally {
      setLoadingUsers(false);
    }
  };

  const handleLoadLiveClasses = async () => {
    if (!API_URL || !schoolId) return;

    try {
      setLoadingClasses(true);
      setError("");
      setShowAllClasses(false);
      setLiveClasses(null);

      const response = await fetch(
        `${API_URL}/admin/schools/${schoolId}/integration/live/classes`,
        {
          credentials: "include",
        },
      );

      if (!response.ok) {
        const body = await response.text();
        throw new Error(body || "Live classes ophalen mislukt");
      }

      const data: SchoolIntegrationLiveClassesResponse = await response.json();
      setLiveClasses(data);
    } catch (err) {
      console.error(err);
      setError(
        err instanceof Error
          ? err.message
          : "Er ging iets mis bij het ophalen van live classes",
      );
    } finally {
      setLoadingClasses(false);
    }
  };

  if (loading) {
    return (
      <main className="page">
        <p>Integratie laden...</p>
      </main>
    );
  }

  return (
    <main className="page">
      <h1>Schoolintegratie</h1>

      {error && <p className="message error">{error}</p>}
      {success && <p className="message success">{success}</p>}

      {canUsePage && (
        <>
          <section className="card">
            <h2>School</h2>

            <div className="grid">
              <div className="stat">
                <span className="label">Naam</span>
                <strong>{schoolName || "-"}</strong>
              </div>

              <div className="stat">
                <span className="label">Domein</span>
                <strong>{schoolDomain || "-"}</strong>
              </div>

              <div className="stat">
                <span className="label">Actief</span>
                <strong>{integration?.onerosterEnabled ? "Ja" : "Nee"}</strong>
              </div>

              <div className="stat">
                <span className="label">Secret opgeslagen</span>
                <strong>
                  {integration?.clientSecretConfigured ? "Ja" : "Nee"}
                </strong>
              </div>

              <div className="stat">
                <span className="label">Laatste test</span>
                <strong>{formatDate(integration?.lastTestSuccessfulAt)}</strong>
              </div>

              <div className="stat">
                <span className="label">Laatste sync</span>
                <strong>{formatDate(integration?.lastSyncAt)}</strong>
              </div>
            </div>

            {integration?.lastError && (
              <div className="alert">
                <span className="label">Laatste fout</span>
                <p>{integration.lastError}</p>
              </div>
            )}
          </section>

          <section className="card">
            <h2>Campussen</h2>
            <p className="help">
              Voeg hier de campussen toe die later in boekinventaris als keuze
              verschijnen.
            </p>

            <form className="campusForm" onSubmit={handleCreateCampus}>
              <label className="field campusNameField">
                <span>Nieuwe campus</span>
                <input
                  type="text"
                  value={newCampusName}
                  onChange={(e) => setNewCampusName(e.target.value)}
                  placeholder="Bijv. Campus Zuid"
                  disabled={savingCampus}
                />
              </label>

              <button
                type="submit"
                className="button primaryButton"
                disabled={savingCampus || !newCampusName.trim()}
              >
                {savingCampus ? "Toevoegen..." : "Campus toevoegen"}
              </button>
            </form>

            {loadingCampuses ? (
              <p className="help">Campussen laden...</p>
            ) : campuses.length === 0 ? (
              <p className="emptyState">Nog geen campussen toegevoegd.</p>
            ) : (
              <ul className="campusList">
                {campuses.map((campus) => (
                  <li key={campus.id} className="campusListItem">
                    <span>{campus.name}</span>
                    <button
                      type="button"
                      className="button dangerButton"
                      onClick={() => handleDeleteCampus(campus.id)}
                      disabled={deletingCampusId === campus.id}
                    >
                      {deletingCampusId === campus.id
                        ? "Verwijderen..."
                        : "Verwijderen"}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="card">
            <h2>Configuratie</h2>

            <div className="form">
              <label className="field">
                <span>OneRoster base URL</span>
                <input
                  type="text"
                  value={baseUrl}
                  onChange={(e) => setBaseUrl(e.target.value)}
                  placeholder="https://jouwschool.smartschool.be"
                />
              </label>

              <label className="field">
                <span>Client ID</span>
                <input
                  type="text"
                  value={clientId}
                  onChange={(e) => setClientId(e.target.value)}
                  placeholder="Client ID"
                />
              </label>

              <label className="field">
                <span>Client secret</span>
                <input
                  type="password"
                  value={clientSecret}
                  onChange={(e) => setClientSecret(e.target.value)}
                  placeholder={
                    integration?.clientSecretConfigured
                      ? "Laat leeg om het huidige secret te behouden"
                      : "Client secret"
                  }
                />
              </label>

              <label className="field">
                <span>Smartschool accesscode</span>
                <input
                  type="password"
                  value={smartschoolAccesscode}
                  onChange={(e) => setSmartschoolAccesscode(e.target.value)}
                  placeholder={
                    integration?.smartschoolAccesscodeConfigured
                      ? "Laat leeg om de huidige accesscode te behouden"
                      : "Webservices Accesscode"
                  }
                />
              </label>

              <label className="field">
                <span>
                  Smartschool afzender vanaf dit smartschool account zullen de
                  berichten verstuurd worden
                </span>
                <input
                  type="text"
                  value={senderIdentifier}
                  onChange={(e) => setSenderIdentifier(e.target.value)}
                  placeholder="bv. jan.janssen"
                />
              </label>

              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={enabled}
                  onChange={(e) => setEnabled(e.target.checked)}
                />
                <span>Integratie inschakelen</span>
              </label>
            </div>

            <div className="actions">
              <button
                className="button primaryButton"
                onClick={handleSave}
                disabled={saving || testing}
              >
                {saving ? "Opslaan..." : "Opslaan"}
              </button>

              <button
                className="button"
                onClick={handleTest}
                disabled={saving || testing}
              >
                {testing ? "Testen..." : "Verbinding testen"}
              </button>
            </div>
          </section>

          <section className="card">
            <h2>Debug</h2>
            <p className="help">Test of de live koppeling echt werkt.</p>

            <div className="actions">
              <button
                className="button"
                onClick={handleLoadLiveUsers}
                disabled={loadingUsers}
              >
                {loadingUsers ? "Users laden..." : "Live users ophalen"}
              </button>

              <button
                className="button"
                onClick={handleLoadLiveClasses}
                disabled={loadingClasses}
              >
                {loadingClasses ? "Klassen laden..." : "Live classes ophalen"}
              </button>
            </div>

            {testResult && (
              <div className="debugBlock">
                <h3>Testresultaat</h3>
                <ul className="list">
                  <li>Succes: {testResult.success ? "Ja" : "Nee"}</li>
                  <li>
                    Token ontvangen: {testResult.tokenReceived ? "Ja" : "Nee"}
                  </li>
                  <li>
                    Schools endpoint bereikbaar:{" "}
                    {testResult.schoolsEndpointReachable ? "Ja" : "Nee"}
                  </li>
                  <li>Aantal scholen: {testResult.schoolCount}</li>
                  <li>Bericht: {testResult.message}</li>
                </ul>
              </div>
            )}

            {liveUsers && (
              <div className="debugBlock">
                <h3>Live users</h3>
                <p>
                  {liveUsers.message} ({liveUsers.userCount} users)
                </p>

                {liveUsers.users.length > 5 && (
                  <button
                    type="button"
                    className="button"
                    onClick={() => setShowAllUsers((prev) => !prev)}
                  >
                    {showAllUsers ? "Toon minder" : "Toon alles"}
                  </button>
                )}

                <pre>
                  {JSON.stringify(
                    showAllUsers
                      ? liveUsers.users
                      : liveUsers.users.slice(0, 5),
                    null,
                    2,
                  )}
                </pre>
              </div>
            )}

            {liveClasses && (
              <div className="debugBlock">
                <h3>Live classes</h3>
                <p>
                  {liveClasses.message} ({liveClasses.classCount} klassen)
                </p>

                {liveClasses.classes.length > 5 && (
                  <button
                    type="button"
                    className="button"
                    onClick={() => setShowAllClasses((prev) => !prev)}
                  >
                    {showAllClasses ? "Toon minder" : "Toon alles"}
                  </button>
                )}

                <pre>
                  {JSON.stringify(
                    showAllClasses
                      ? liveClasses.classes
                      : liveClasses.classes.slice(0, 5),
                    null,
                    2,
                  )}
                </pre>
              </div>
            )}
          </section>
        </>
      )}
    </main>
  );
}
