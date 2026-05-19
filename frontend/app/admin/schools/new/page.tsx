"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import "../../school-integration/schoolIntegration.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

type PreviewUser = {
  sourcedId?: string;
  givenName?: string;
  familyName?: string;
  role?: string;
  email?: string;
  [key: string]: unknown;
};

export default function NewSchoolPage() {
  const router = useRouter();

  useEffect(() => {
    fetch(`${API_URL}/auth/me`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : null))
      .then((me) => {
        if (!me || me.role !== "ADMIN") router.replace("/");
      })
      .catch(() => router.replace("/"));
  }, [router]);

  const [name, setName] = useState("");
  const [domain, setDomain] = useState("");

  const [baseUrl, setBaseUrl] = useState("");
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [showSecret, setShowSecret] = useState(false);
  const [onerosterEnabled, setOnerosterEnabled] = useState(true);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [savedSchoolId, setSavedSchoolId] = useState<number | null>(null);
  const [previewing, setPreviewing] = useState(false);
  const [previewStudents, setPreviewStudents] = useState<PreviewUser[] | null>(
    null,
  );
  const [previewTeachers, setPreviewTeachers] = useState<PreviewUser[] | null>(
    null,
  );
  const [previewClasses, setPreviewClasses] = useState<string[] | null>(null);
  const [previewError, setPreviewError] = useState("");

  const handleSave = async () => {
    setError("");
    setSuccess("");
    if (!name.trim() || !domain.trim()) {
      setError("Schoolnaam en domein zijn verplicht.");
      return;
    }
    setSaving(true);
    try {
      const schoolRes = await fetch(`${API_URL}/admin/schools`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: name.trim(), domain: domain.trim() }),
      });
      let schoolId: number;
      let schoolName: string;
      if (schoolRes.status === 409) {
        const data = await schoolRes.json().catch(() => ({}));
        const match = String(data.message ?? "").match(/id=(\d+)/);
        if (!match) {
          setError(
            "School met dit domein bestaat al maar kon niet worden gevonden.",
          );
          return;
        }
        schoolId = Number(match[1]);
        schoolName = name.trim();
      } else if (!schoolRes.ok) {
        const data = await schoolRes.json().catch(() => ({}));
        setError(data.message ?? "School aanmaken mislukt.");
        return;
      } else {
        const created = await schoolRes.json();
        schoolId = created.id;
        schoolName = created.name;
      }

      if (baseUrl.trim() || clientId.trim() || clientSecret.trim()) {
        const intRes = await fetch(
          `${API_URL}/admin/schools/${schoolId}/integration`,
          {
            method: "PUT",
            credentials: "include",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
              schoolBaseUrl: baseUrl.trim(),
              onerosterClientId: clientId.trim(),
              onerosterClientSecret: clientSecret.trim(),
              onerosterEnabled,
              smartschoolAccesscode: "",
              smartschoolSenderIdentifier: "",
            }),
          },
        );
        if (!intRes.ok) {
          const data = await intRes.json().catch(() => ({}));
          setError(
            `School aangemaakt, maar integratie opslaan mislukt: ${data.message ?? "onbekende fout"}`,
          );
          setSavedSchoolId(schoolId);
          return;
        }
      }

      setSavedSchoolId(schoolId);
      setSuccess(`School "${schoolName}" succesvol aangemaakt.`);
    } catch {
      setError("Er ging iets mis bij het opslaan.");
    } finally {
      setSaving(false);
    }
  };

  const handlePreview = async () => {
    if (!savedSchoolId) return;
    setPreviewError("");
    setPreviewStudents(null);
    setPreviewTeachers(null);
    setPreviewClasses(null);
    setPreviewing(true);
    try {
      const [usersRes, classesRes] = await Promise.all([
        fetch(
          `${API_URL}/admin/schools/${savedSchoolId}/integration/live/users`,
          { credentials: "include" },
        ),
        fetch(
          `${API_URL}/admin/schools/${savedSchoolId}/integration/live/classes`,
          { credentials: "include" },
        ),
      ]);

      if (!usersRes.ok) {
        const data = await usersRes.json().catch(() => ({}));
        setPreviewError(data.message ?? "Preview ophalen mislukt.");
        return;
      }
      const userData = await usersRes.json();
      const users: PreviewUser[] = userData.users ?? [];
      setPreviewStudents(
        users.filter((u) => u.role === "student").slice(0, 10),
      );
      setPreviewTeachers(
        users.filter((u) => u.role === "teacher").slice(0, 10),
      );

      if (classesRes.ok) {
        const classData = await classesRes.json();
        const classes: Record<string, unknown>[] = classData.classes ?? [];
        setPreviewClasses(
          classes
            .slice(0, 10)
            .map((c) => (c.title as string) ?? (c.sourcedId as string) ?? "–"),
        );
      }
    } catch {
      setPreviewError("Er ging iets mis bij het ophalen van de preview.");
    } finally {
      setPreviewing(false);
    }
  };

  const displayName = (u: PreviewUser) =>
    [u.givenName, u.familyName].filter(Boolean).join(" ") || u.sourcedId || "–";

  return (
    <main className="page">
      <h1>School toevoegen</h1>

      {error && <p className="message error">{error}</p>}
      {success && <p className="message success">{success}</p>}

      {/* ── School info ── */}
      <div className="card">
        <h2>Schoolgegevens</h2>
        <div className="form">
          <div className="field">
            <span>Schoolnaam</span>
            <input
              type="text"
              placeholder="bv. GO! Atheneum Antwerpen"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={!!savedSchoolId}
            />
          </div>
          <div className="field">
            <span>Smartschool domein</span>
            <input
              type="text"
              placeholder="bv. https://mijnschool.smartschool.be"
              value={domain}
              onChange={(e) => setDomain(e.target.value)}
              disabled={!!savedSchoolId}
            />
          </div>
        </div>
      </div>

      {/* ── OneRoster integration ── */}
      <div className="card">
        <h2>OneRoster integratie</h2>
        <p className="help">
          Vul de OneRoster API-gegevens in om leerlingen en leerkrachten te
          synchroniseren. Dit is optioneel; je kan het later aanpassen via de
          school-integratiedetails.
        </p>
        <div className="form">
          <div className="field">
            <span>OneRoster basis-URL</span>
            <input
              type="text"
              placeholder="bv. https://mijnschool.smartschool.be"
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              disabled={!!savedSchoolId}
            />
          </div>
          <div className="field">
            <span>Client ID</span>
            <input
              type="text"
              placeholder="Client ID"
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              disabled={!!savedSchoolId}
            />
          </div>
          <div className="field">
            <span>Client Secret</span>
            <div className="passwordFieldWrapper">
              <input
                type={showSecret ? "text" : "password"}
                placeholder="Client Secret"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
                disabled={!!savedSchoolId}
              />
              <button
                type="button"
                className="togglePasswordBtn"
                onClick={() => setShowSecret((v) => !v)}
              >
                {showSecret ? "Verberg" : "Toon"}
              </button>
            </div>
          </div>
          <label className="checkbox">
            <input
              type="checkbox"
              checked={onerosterEnabled}
              onChange={(e) => setOnerosterEnabled(e.target.checked)}
              disabled={!!savedSchoolId}
            />
            OneRoster synchronisatie inschakelen
          </label>
        </div>
      </div>

      {/* ── Actions ── */}
      <div className="actions">
        <button
          className="button"
          onClick={() => router.push("/admin/schools")}
        >
          Annuleren
        </button>
        {!savedSchoolId ? (
          <button
            className="button primaryButton"
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? "Opslaan..." : "School aanmaken"}
          </button>
        ) : (
          <>
            <button
              className="button primaryButton"
              onClick={handlePreview}
              disabled={previewing}
            >
              {previewing ? "Laden..." : "Toon preview"}
            </button>
            <button
              className="button primaryButton"
              onClick={() => router.push("/admin/schools")}
            >
              Terug naar overzicht
            </button>
          </>
        )}
      </div>

      {/* ── Preview error ── */}
      {previewError && <p className="message error">{previewError}</p>}

      {/* ── Preview results ── */}
      {(previewStudents !== null ||
        previewTeachers !== null ||
        previewClasses !== null) && (
        <div className="grid" style={{ marginTop: "1.5rem" }}>
          <div className="card">
            <h2>Leerlingen (eerste 10)</h2>
            {previewStudents && previewStudents.length > 0 ? (
              <ul className="list">
                {previewStudents.map((u, i) => (
                  <li key={u.sourcedId ?? i}>{displayName(u)}</li>
                ))}
              </ul>
            ) : (
              <p className="emptyState">Geen leerlingen gevonden.</p>
            )}
          </div>

          <div className="card">
            <h2>Leerkrachten (eerste 10)</h2>
            {previewTeachers && previewTeachers.length > 0 ? (
              <ul className="list">
                {previewTeachers.map((u, i) => (
                  <li key={u.sourcedId ?? i}>{displayName(u)}</li>
                ))}
              </ul>
            ) : (
              <p className="emptyState">Geen leerkrachten gevonden.</p>
            )}
          </div>

          <div className="card">
            <h2>Klassen (eerste 10)</h2>
            {previewClasses && previewClasses.length > 0 ? (
              <ul className="list">
                {previewClasses.map((name, i) => (
                  <li key={i}>{name}</li>
                ))}
              </ul>
            ) : (
              <p className="emptyState">Geen klassen gevonden.</p>
            )}
          </div>
        </div>
      )}
    </main>
  );
}
