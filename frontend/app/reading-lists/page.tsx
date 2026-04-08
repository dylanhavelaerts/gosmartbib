"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../context/AuthContext";
import ProtectedRoute from "../components/ProtectedRoute";
import "./create/createReadingList.css"; // We hergebruiken slim jouw bestaande CSS!

export default function MyReadingListsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

  const [lists, setLists] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // @ts-ignore
    const userId = user?.id || user?.userID;
    if (!userId) return;

    fetch(`${apiUrl}/reading-lists/user/${userId}`, { credentials: "include" })
      .then((res) => (res.ok ? res.json() : []))
      .then((data) => {
        setLists(data);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Fout bij ophalen lijsten", err);
        setLoading(false);
      });
  }, [user, apiUrl]);

  return (
    <ProtectedRoute allowedRoles={["TEACHER", "ADMIN", "BIBLIOTHEEKBEHEERDER"]}>
      <div className="readingListContainer">
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "2rem" }}>
          <h1 className="pageTitle" style={{ margin: 0 }}>Mijn Leeslijsten</h1>
          <button 
            className="titleSubmitBtn" 
            onClick={() => router.push("/reading-lists/create")}
          >
            + Nieuwe Lijst
          </button>
        </div>

        {loading ? (
          <p>Lijsten laden...</p>
        ) : lists.length === 0 ? (
          <div className="info-island" style={{ alignItems: "center", padding: "3rem" }}>
            <p>Je hebt nog geen leeslijsten aangemaakt.</p>
          </div>
        ) : (
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))", gap: "1.5rem" }}>
            {lists.map((list) => (
              <div 
                key={list.id} 
                className="info-island" 
                style={{ cursor: "pointer", transition: "transform 0.2s" }}
                onClick={() => router.push(`/reading-lists/${list.id}`)}
                onMouseOver={(e) => (e.currentTarget.style.transform = "translateY(-5px)")}
                onMouseOut={(e) => (e.currentTarget.style.transform = "translateY(0)")}
              >
                <h3 style={{ color: "#8e2446", margin: "0 0 0.5rem 0" }}>{list.title}</h3>
                <p style={{ margin: 0, fontSize: "0.9rem", color: "#666" }}>
                  Deadline: {new Date(list.deadline).toLocaleDateString()}
                </p>
                <p style={{ margin: "0.5rem 0 0 0", fontWeight: "bold" }}>
                  {list.books ? list.books.length : 0} boeken
                </p>
              </div>
            ))}
          </div>
        )}
      </div>
    </ProtectedRoute>
  );
}