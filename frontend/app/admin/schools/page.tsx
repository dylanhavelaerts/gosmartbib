"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import "./schools.css";
import ProtectedRoute from "@/app/components/ProtectedRoute";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

type School = {
  id: number;
  name: string;
  domain: string;
  adminApproved: boolean;
};

export default function SchoolsAdminPage() {
  const router = useRouter();
  const [schools, setSchools] = useState<School[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const [approveSchool, setApproveSchool] = useState<School | null>(null);
  const [approveName, setApproveName] = useState("");
  const [approveSaving, setApproveSaving] = useState(false);

  const [deleteSchool, setDeleteSchool] = useState<School | null>(null);
  const [deleteConfirming, setDeleteConfirming] = useState(false);

  const [renameSchool, setRenameSchool] = useState<School | null>(null);
  const [renameName, setRenameName] = useState("");
  const [renameSaving, setRenameSaving] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const meRes = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });
        if (!meRes.ok) {
          router.replace("/");
          return;
        }
        const me = await meRes.json();
        if (me.role !== "ADMIN") {
          router.replace("/");
          return;
        }

        const res = await fetch(`${API_URL}/admin/schools`, {
          credentials: "include",
        });
        if (!res.ok) {
          setError("Kon scholen niet ophalen");
          return;
        }
        setSchools(await res.json());
      } catch {
        setError("Er ging iets mis met het laden");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [router]);

  const handleApprove = async () => {
    if (!approveSchool) return;
    setApproveSaving(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(
        `${API_URL}/admin/schools/${approveSchool.id}/approve`,
        {
          method: "PATCH",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ name: approveName.trim() }),
        },
      );
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? "Goedkeuren mislukt");
        return;
      }
      const updated: School = await res.json();
      setSchools((prev) =>
        prev.map((s) => (s.id === updated.id ? updated : s)).sort(schoolSort),
      );
      setSuccess(`School "${updated.name}" goedgekeurd`);
      setApproveSchool(null);
      setApproveName("");
    } catch {
      setError("Er ging iets mis");
    } finally {
      setApproveSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteSchool) return;
    setDeleteConfirming(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${API_URL}/admin/schools/${deleteSchool.id}`, {
        method: "DELETE",
        credentials: "include",
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? "Verwijderen mislukt");
        return;
      }
      setSchools((prev) => prev.filter((s) => s.id !== deleteSchool.id));
      setSuccess(`School "${deleteSchool.name}" verwijderd`);
      setDeleteSchool(null);
    } catch {
      setError("Er ging iets mis");
    } finally {
      setDeleteConfirming(false);
    }
  };

  const handleRename = async () => {
    if (!renameSchool) return;
    setRenameSaving(true);
    setError("");
    setSuccess("");
    try {
      const res = await fetch(`${API_URL}/admin/schools/${renameSchool.id}`, {
        method: "PATCH",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: renameName.trim() }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        setError(data.message ?? "Hernoemen mislukt");
        return;
      }
      const updated: School = await res.json();
      setSchools((prev) =>
        prev.map((s) => (s.id === updated.id ? updated : s)).sort(schoolSort),
      );
      setSuccess(`School hernoemd naar "${updated.name}"`);
      setRenameSchool(null);
      setRenameName("");
    } catch {
      setError("Er ging iets mis");
    } finally {
      setRenameSaving(false);
    }
  };

  const schoolSort = (a: School, b: School) => {
    if (a.adminApproved !== b.adminApproved) return a.adminApproved ? 1 : -1;
    return a.name.localeCompare(b.name);
  };

  const pendingCount = schools.filter((s) => !s.adminApproved).length;

  if (loading) return <div>Scholen laden...</div>;

  return (
    <ProtectedRoute allowedRoles="ADMIN">
    <main>
      <div id="schoolsMain">
        {error && <p className="adminMessage adminMessageError">{error}</p>}
        {success && (
          <p className="adminMessage adminMessageSuccess">{success}</p>
        )}

        <div className="adminPageHeader">
          <div>
            <h1>Scholen</h1>
            {pendingCount > 0 && (
              <p
                className="adminMessage adminMessageError"
                style={{ marginTop: "0.4rem" }}
              >
                {pendingCount} school(en) wachten op goedkeuring
              </p>
            )}
          </div>
          <Link href="/admin/schools/new">
            <button className="adminPrimaryButton">School toevoegen</button>
          </Link>
        </div>

        <table className="adminTable">
          <thead>
            <tr>
              <th className="adminHeader" style={{ textAlign: "left" }}>
                School
              </th>
              <th className="adminHeader" style={{ textAlign: "left" }}>
                Domein
              </th>
              <th className="adminHeader">Status</th>
              <th className="adminHeader"></th>
            </tr>
          </thead>
          <tbody>
            {schools.map((school) => (
              <tr key={school.id}>
                <td className="adminCell nameCell">
                  <Link
                    href={`/admin/school-integration?schoolId=${school.id}`}
                  >
                    {school.name}
                  </Link>
                </td>
                <td className="adminCell domainCell">{school.domain}</td>
                <td className="adminCell">
                  {school.adminApproved ? (
                    <span className="statusBadge statusApproved">
                      Goedgekeurd
                    </span>
                  ) : (
                    <span className="statusBadge statusPending">Wachtend</span>
                  )}
                </td>
                <td
                  className="adminCell"
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    justifyContent: "flex-end",
                  }}
                >
                  {!school.adminApproved && (
                    <button
                      className="adminTableButton"
                      onClick={() => {
                        setApproveSchool(school);
                        setApproveName(school.name);
                      }}
                    >
                      Goedkeuren
                    </button>
                  )}
                  <button
                    className="adminTableButton adminTableButtonDanger"
                    onClick={() => setDeleteSchool(school)}
                  >
                    Verwijderen
                  </button>
                  <button
                    className="adminTableButton"
                    onClick={() => {
                      setRenameSchool(school);
                      setRenameName(school.name);
                    }}
                  >
                    Bewerken
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {deleteSchool && (
        <div className="schoolModalOverlay">
          <div className="schoolModalBox">
            <h2>School verwijderen</h2>
            <p style={{ marginBottom: "1rem" }}>
              Weet je zeker dat je <strong>{deleteSchool.name}</strong> wil
              verwijderen? Dit kan niet ongedaan worden gemaakt.
            </p>
            <div className="schoolModalFooter">
              <button
                className="schoolModalCancel"
                onClick={() => setDeleteSchool(null)}
                disabled={deleteConfirming}
              >
                Annuleren
              </button>
              <button
                className="adminDangerButton"
                onClick={handleDelete}
                disabled={deleteConfirming}
              >
                {deleteConfirming ? "Bezig..." : "Verwijderen"}
              </button>
            </div>
          </div>
        </div>
      )}

      {renameSchool && (
        <div className="schoolModalOverlay">
          <div className="schoolModalBox">
            <h2>School hernoemen</h2>
            <div className="schoolModalField">
              <label>Smartschool domein</label>
              <input type="text" value={renameSchool.domain} disabled />
            </div>
            <div className="schoolModalField">
              <label htmlFor="renameName">Schoolnaam</label>
              <input
                id="renameName"
                type="text"
                placeholder="bv. GO! Atheneum Antwerpen"
                value={renameName}
                onChange={(e) => setRenameName(e.target.value)}
              />
            </div>
            <div className="schoolModalFooter">
              <button
                className="schoolModalCancel"
                onClick={() => setRenameSchool(null)}
                disabled={renameSaving}
              >
                Annuleren
              </button>
              <button
                className="adminPrimaryButton"
                onClick={handleRename}
                disabled={renameSaving || !renameName.trim()}
              >
                {renameSaving ? "Bezig..." : "Opslaan"}
              </button>
            </div>
          </div>
        </div>
      )}

      {approveSchool && (
        <div className="schoolModalOverlay">
          <div className="schoolModalBox">
            <h2>School goedkeuren</h2>
            <div className="schoolModalField">
              <label>Smartschool domein</label>
              <input type="text" value={approveSchool.domain} disabled />
            </div>
            <div className="schoolModalField">
              <label htmlFor="approveName">Schoolnaam</label>
              <input
                id="approveName"
                type="text"
                placeholder="bv. GO! Atheneum Antwerpen"
                value={approveName}
                onChange={(e) => setApproveName(e.target.value)}
              />
            </div>
            <div className="schoolModalFooter">
              <button
                className="schoolModalCancel"
                onClick={() => setApproveSchool(null)}
              >
                Annuleren
              </button>
              <button
                className="adminPrimaryButton"
                onClick={handleApprove}
                disabled={approveSaving}
              >
                {approveSaving ? "Bezig..." : "Goedkeuren"}
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
    </ProtectedRoute>
  );
}
