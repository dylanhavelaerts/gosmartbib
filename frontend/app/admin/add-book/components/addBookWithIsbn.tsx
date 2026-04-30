"use client";

import { useEffect, useState } from "react";
import type { Book } from "../../../interfaces/Book";
import type { MeResponse } from "@/app/interfaces/user";
import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";
import { fetchSchoolCampuses } from "@/app/utils/schoolCampuses";
import "./addBookForm.css";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export default function AddBookWithIsbn() {
  const [isbn, setIsbn] = useState("");
  const [message, setMessage] = useState("");
  const [campusLoadError, setCampusLoadError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [previewBook, setPreviewBook] = useState<Book | null>(null);
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png");
  const [campus, setCampus] = useState("");
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);

  useEffect(() => {
    const loadCampusesForCurrentSchool = async () => {
      if (!API_URL) {
        setCampusLoadError("NEXT_PUBLIC_API_URL ontbreekt");
        return;
      }

      try {
        setLoadingCampuses(true);
        setCampusLoadError("");

        const meResponse = await fetch(`${API_URL}/auth/me`, {
          credentials: "include",
        });

        if (!meResponse.ok) {
          throw new Error("Kon de ingelogde gebruiker niet ophalen");
        }

        const meData: MeResponse = await meResponse.json();

        if (!meData.school?.id) {
          throw new Error("Geen school gevonden voor de ingelogde gebruiker");
        }

        const campusData = await fetchSchoolCampuses(API_URL, meData.school.id);
        setCampuses(campusData);
      } catch (error) {
        console.error(error);
        setCampusLoadError(
          error instanceof Error
            ? error.message
            : "Kon de campussen niet ophalen",
        );
      } finally {
        setLoadingCampuses(false);
      }
    };

    loadCampusesForCurrentSchool();
  }, []);

  const handleSearchBook = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isbn.trim()) return;

    setLoading(true);
    setMessage("");
    setPreviewBook(null);

    try {
      const response = await fetch(
        `${API_URL}/books/search/${isbn}`,{credentials: "include"}
      );

      if (response.ok) {
        const data = await response.json();
        setPreviewBook(data);
        setMessage("Boek gevonden! Controleer de gegevens hieronder.");
      } else if (response.status === 404) {
        setMessage("Geen boek gevonden met dit ISBN-nummer bij Google Books.");
      } else {
        setMessage("Er is een onverwachte serverfout opgetreden.");
      }
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken. Controleer of de backend draait.");
    } finally {
      setLoading(false);
    }
  };

  const handleConfirmAdd = async () => {
    if (!isbn.trim()) return;

    setLoading(true);
    setMessage("");

    try {
      const params = new URLSearchParams();
        if (campus.trim()) {
          params.set("campus", campus.trim());
        }

        const response = await fetch(
          `${API_URL}/books/add/${isbn}${params.toString() ? `?${params.toString()}` : ""}`,
          {
            method: "POST",
            credentials: "include",
          },
        );

      if (response.ok) {
        const data = await response.json();
        setMessage(`Boek succesvol aan de database toegevoegd: "${data.title}"`);
        setIsbn("");
        setCampus("");
        setPreviewBook(null);
      } else {
        setMessage("Er ging iets mis bij het opslaan van het boek.");
      }
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken.");
    } finally {
      setLoading(false);
    }
  };

  const handleCancelPreview = () => {
    setPreviewBook(null);
    setMessage("");
    setIsbn("");
  };

  useEffect(() => {
    setImgSrc(previewBook?.thumbnail || "/No-Image-Available-Placeholder.png");
  }, [previewBook]);

  const searchButtonClass = `submitButton ${loading ? "submitButtonLoading" : ""}`.trim();
  const messageClass = `message ${
    message.includes("succesvol")
      ? "messageSuccess"
      : message.includes("gevonden!")
        ? "messageInfo"
        : "messageError"
  }`.trim();

  return (
    <>
      <h1 className="title">Nieuw boek toevoegen</h1>

      <p className="description">
        Scan of typ het ISBN-nummer in. We halen eerst een voorbeeld op voordat
        we het opslaan.
      </p>

      <form onSubmit={handleSearchBook} className="form">
        <div className="fieldGroup">
          <label htmlFor="isbn" className="label">
            ISBN Nummer:
          </label>

          <input
            id="isbn"
            type="text"
            value={isbn}
            onChange={(e) => setIsbn(e.target.value)}
            placeholder="Bijv. 9781473227989"
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label htmlFor="campus" className="label">
            Campus
          </label>

          <select
            id="campus"
            value={campus}
            onChange={(e) => setCampus(e.target.value)}
            className="select"
            disabled={previewBook !== null || loadingCampuses}
          >
            <option value="">
              {loadingCampuses ? "Campussen laden..." : "Geen campus"}
            </option>

            {campuses.map((campusOption) => (
              <option key={campusOption.id} value={campusOption.name}>
                {campusOption.name}
              </option>
            ))}
          </select>

          {campusLoadError && <p className="fieldError">{campusLoadError}</p>}
        </div>

        {!previewBook && (
          <button type="submit" disabled={loading} className={searchButtonClass}>
            {loading ? "Bezig met zoeken..." : "Zoek Boek"}
          </button>
        )}
      </form>

      {previewBook && (
        <div className="previewCard">
          <h2 className="previewTitle">Preview van het boek:</h2>

          <div className="previewContent">
            <img
              src={imgSrc}
              alt="Cover"
              onError={() => setImgSrc("/No-Image-Available-Placeholder.png")}
              className="previewImage"
            />

            <div>
              <p>
                <strong>Titel:</strong> {previewBook.title}
              </p>
              <p>
                <strong>Auteur(s):</strong> {previewBook.authors?.join(", ")}
              </p>
              <p>
                <strong>Uitgeverij:</strong> {previewBook.publisher}
              </p>
              <p>
                <strong>Jaar van uitgave:</strong>{" "}
                {previewBook.publishedYear || "Onbekend"}
              </p>
              <p>
                <strong>ISBN:</strong> {previewBook.isbn}
              </p>
              <p>
                <strong>Campus:</strong> {campus || "Geen campus"}
              </p>
              <p>
                <strong>Pagina's:</strong> {previewBook.pageCount}
              </p>
            </div>
          </div>

          <div className="actionRow">
            <button
              type="button"
              onClick={handleConfirmAdd}
              disabled={loading}
              className="confirmButton"
            >
              {loading ? "Bezig..." : "Ja, Voeg toe aan Catalogus"}
            </button>

            <button
              type="button"
              onClick={handleCancelPreview}
              disabled={loading}
              className="cancelButton"
            >
              Annuleren
            </button>
          </div>
        </div>
      )}

      {message && <div className={messageClass}>{message}</div>}
    </>
  );
}
