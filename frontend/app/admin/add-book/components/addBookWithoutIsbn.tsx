"use client";

import { useEffect, useRef, useState } from "react";
import {  BOOK_CATEGORIES, BOOK_LABELS } from "../../../interfaces/Book";
import type { Book, BookInventory } from "../../../interfaces/Book";
import "./addBookForm.css";
import type { MeResponse } from "@/app/interfaces/user";
import type { SchoolCampusDTO } from "@/app/interfaces/schoolIntegration";
import {
  fetchSchoolCampuses,
  getCampusSelectOptions,
} from "@/app/utils/schoolCampuses";

export default function AddBookWithoutIsbn() {
  const [message, setMessage] = useState("");
  const [campusLoadError, setCampusLoadError] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingCampuses, setLoadingCampuses] = useState(false);
  const [previewBook, setPreviewBook] = useState<Book | null>(null);

  const [title, setTitle] = useState("");
  const [authors, setAuthors] = useState<string[]>([""]);
  const [publisher, setPublisher] = useState("");
  const [description, setDescription] = useState("");
  const [pageCount, setPageCount] = useState(0);
  const [categories, setCategories] = useState<string[]>([]);
  const [thumbnail, setThumbnail] = useState("");
  const [language, setLanguage] = useState("");
  const [publishedYear, setPublishedYear] = useState(0);
  const [rating, setRating] = useState(0);
  const [openDropdown, setOpenDropdown] = useState(false);
  const [openLabelDropdown, setOpenLabelDropdown] = useState(false);
  const [didacticTag, setDidacticTag] = useState(false);
  const [labels, setLabels] = useState<string[]>([]);
  const [readingLevel, setReadingLevel] = useState("");
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png");
  const [ageRange, setAgeRange] = useState("");
  const [me, setMe] = useState<MeResponse | null>(null);
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [inventories, setInventories] = useState<BookInventory[]>([]);

  const API_URL = process.env.NEXT_PUBLIC_API_URL;

  const handleAuthorChange = (index: number, value: string) => {
    const updatedAuthors = [...authors];
    updatedAuthors[index] = value;
    setAuthors(updatedAuthors);
  };

  const handleAddAuthorField = () => {
    setAuthors([...authors, ""]);
  };

  const handleRemoveAuthorField = () => {
    if (authors.length <= 1) return;
    setAuthors(authors.slice(0, authors.length - 1));
  };

  const handlePreviewBook = (e: React.FormEvent) => {
    e.preventDefault();

    const book: Book = {
      id: 0,
      isbn: "",
      title,
      authors,
      publisher,
      description,
      pageCount,
      categories,
      thumbnail,
      language,
      rating,
      publishedYear,
      spotlight: false,
      didacticTag,
      readingLevel,
      labels,
      totalCopies: totalCopiesFromInventories,
      availableCopies: availableCopiesFromInventories,
      ageRange,
      inventories
    };

    setPreviewBook(book);
    setMessage("Controleer de gegevens hieronder.");
  };

  const dropdownRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node)
      ) {
        setOpenDropdown(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const dropdownRefLabel = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRefLabel.current &&
        !dropdownRefLabel.current.contains(event.target as Node)
      ) {
        setOpenLabelDropdown(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const handleConfirmAdd = async () => {
    if (!previewBook) return;

    if (inventories.length === 0) {
  setMessage("Voeg minstens één inventarisregel toe.");
  return;
}

for (const inventory of inventories) {
  if (!inventory.schoolId) {
    setMessage("Elke inventarisregel moet een school hebben.");
    return;
  }

  if (inventory.totalCopies < 0 || inventory.availableCopies < 0) {
    setMessage("Aantallen mogen niet negatief zijn.");
    return;
  }

  if (inventory.availableCopies > inventory.totalCopies) {
    setMessage("Beschikbare exemplaren mogen niet groter zijn dan totaal.");
    return;
  }
}

    setLoading(true);
    setMessage("");

    try {
      const response = await fetch(`${API_URL}/books/add`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        }, credentials: "include",
        body: JSON.stringify({
          title,
          authors: authors.filter((author) => author.trim() !== ""),
          publisher,
          description,
          pageCount,
          categories,
          thumbnail,
          language,
          rating,
          publishedYear,
          spotlight: false,
          didacticTag,
          labels,
          readingLevel,
          totalCopies: totalCopiesFromInventories,
              availableCopies: availableCopiesFromInventories,
              ageRange,
              inventories: inventories.map((inventory) => ({
                schoolId: inventory.schoolId,
                campus: inventory.campus,
                totalCopies: inventory.totalCopies,
                availableCopies: inventory.availableCopies,
              })),
        }),
      });

      if (response.ok) {
        const data = await response.json();
        setMessage(`Boek succesvol aan de database toegevoegd: "${data.title}"`);
        setPreviewBook(null);

        setTitle("");
        setAuthors([""]);
        setPublisher("");
        setDescription("");
        setPageCount(0);
        setCategories([]);
        setThumbnail("");
        setLanguage("");
        setPublishedYear(0);
        setRating(0);
        setOpenDropdown(false);
        setOpenLabelDropdown(false);
        setDidacticTag(false);
        setLabels([]);
        setReadingLevel("");
        setAgeRange("");
        setInventories(me?.school ? [createEmptyInventory(me.school)] : []);
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
  };

  useEffect(() => {
    setImgSrc(previewBook?.thumbnail || "/No-Image-Available-Placeholder.png");
  }, [previewBook]);

  const submitButtonClass = `submitButton ${loading ? "submitButtonLoading" : ""}`.trim();
  const messageClass = `message ${
    message.includes("succesvol")
      ? "messageSuccess"
      : message.includes("Controleer")
        ? "messageInfo"
        : "messageError"
  }`.trim();

  const createEmptyInventory = (
  school: MeResponse["school"] | null,
): BookInventory => ({
  id: null,
  schoolId: school?.id ?? null,
  schoolName: school?.name ?? "",
  campus: "",
  totalCopies: 1,
  availableCopies: 1,
});

const totalCopiesFromInventories = inventories.reduce(
  (sum, inventory) => sum + (inventory.totalCopies || 0),
  0,
);

const availableCopiesFromInventories = inventories.reduce(
  (sum, inventory) => sum + (inventory.availableCopies || 0),
  0,
);


function updateInventory(
  index: number,
  field: keyof BookInventory,
  value: string | number | null,
) {
  setInventories((prev) =>
    prev.map((inventory, i) =>
      i === index ? { ...inventory, [field]: value } : inventory,
    ),
  );
}

function addInventoryRow() {
  setInventories((prev) => [...prev, createEmptyInventory(me?.school ?? null)]);
}

function removeInventoryRow(index: number) {
  setInventories((prev) => prev.filter((_, i) => i !== index));
}

useEffect(() => {
  async function loadMeAndCampuses() {
    if (!API_URL) return;

    try {
      setLoadingCampuses(true);
      setCampusLoadError("");

      const response = await fetch(`${API_URL}/auth/me`, {
        credentials: "include",
      });

      if (!response.ok) return;

      const data: MeResponse = await response.json();
      setMe(data);

      if (data.school) {
        setInventories([createEmptyInventory(data.school)]);

        const campusData = await fetchSchoolCampuses(API_URL, data.school.id);
          setCampuses(campusData);
      }
    } catch (error) {
        console.error("Kon gebruiker of campussen niet ophalen:", error);
        setCampusLoadError(
          error instanceof Error
            ? error.message
            : "Kon de campussen niet ophalen",
        );
      } finally {
        setLoadingCampuses(false);
      }
    }

  loadMeAndCampuses();
}, [API_URL]);


  return (
    <>
      <h1 className="title">Nieuw boek toevoegen zonder ISBN nummer</h1>

      <p className="description">
        Geef hier de nodige info om het boek aan te maken.
      </p>

      <form onSubmit={handlePreviewBook} className="form">
        <div className="fieldGroup">
          <label className="label">Titel</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titel van het boek"
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Auteur(s)</label>

          {authors.map((author, index) => (
            <input
              key={index}
              type="text"
              value={author}
              onChange={(e) => handleAuthorChange(index, e.target.value)}
              placeholder={`Auteur ${index + 1}`}
              className="input authorInput"
              disabled={previewBook !== null}
            />
          ))}

          <div className="authorButtons">
            <button
              type="button"
              onClick={handleAddAuthorField}
              disabled={previewBook !== null}
              className="smallButton"
            >
              Auteur toevoegen
            </button>

            <button
              type="button"
              onClick={handleRemoveAuthorField}
              disabled={previewBook !== null}
              className="smallButton"
            >
              Auteur verwijderen
            </button>
          </div>
        </div>

        <div className="fieldGroup">
          <label className="label">Uitgever</label>
          <input
            type="text"
            value={publisher}
            onChange={(e) => setPublisher(e.target.value)}
            placeholder="Uitgever van het boek"
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Omschrijving</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Omschrijving van het boek"
            rows={5}
            className="textarea"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Aantal pagina's</label>
          <input
            type="number"
            value={pageCount}
            onChange={(e) => setPageCount(Number(e.target.value) || 0)}
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Categorieën</label>
          <div ref={dropdownRef} className="dropdownWrapper">
            <button
              type="button"
              onClick={() => setOpenDropdown(!openDropdown)}
              disabled={previewBook !== null}
              className="dropdownToggle"
            >
              {categories.length !== 0
                ? categories.join(", ")
                : "Selecteer categorieën"}
            </button>

            {openDropdown && (
              <div className="dropdownPanel">
                {BOOK_CATEGORIES.map((category) => (
                  <label key={category} className="checkboxLabel">
                    <input
                      type="checkbox"
                      checked={categories.includes(category)}
                      onChange={() => {
                        setCategories((prev) =>
                          prev.includes(category)
                            ? prev.filter((c) => c !== category)
                            : [...prev, category],
                        );
                      }}
                      disabled={previewBook !== null}
                    />
                    <span>{category}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="fieldGroup">
          <label className="label">Leefwereldlabels</label>
          <div ref={dropdownRefLabel} className="dropdownWrapper">
            <button
              type="button"
              onClick={() => setOpenLabelDropdown(!openLabelDropdown)}
              disabled={previewBook !== null}
              className="dropdownToggle"
            >
              {labels.length !== 0 ? labels.join(", ") : "Selecteer labels"}
            </button>

            {openLabelDropdown && (
              <div className="dropdownPanel">
                {BOOK_LABELS.map((label) => (
                  <label key={label} className="checkboxLabel">
                    <input
                      type="checkbox"
                      checked={labels.includes(label)}
                      onChange={() => {
                        setLabels((prev) =>
                          prev.includes(label)
                            ? prev.filter((c) => c !== label)
                            : [...prev, label],
                        );
                      }}
                      disabled={previewBook !== null}
                    />
                    <span>{label}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="fieldGroup">
          <label className="label">Foto</label>
          <input
            type="text"
            value={thumbnail}
            onChange={(e) => setThumbnail(e.target.value)}
            placeholder="Url voor een foto van de voorpagina"
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Taal</label>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className="select"
            disabled={previewBook !== null}
          >
            <option value="">Alle talen</option>
            <option value="en">EN</option>
            <option value="nl">NL</option>
            <option value="fr">FR</option>
          </select>
        </div>

        <div className="fieldGroup">
          <label className="label">Rating</label>
          <input
            type="number"
            min="0"
            max="5"
            step="0.1"
            value={rating}
            onChange={(e) => setRating(Number(e.target.value) || 0)}
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Jaar van uitgave</label>
          <input
            type="number"
            value={publishedYear}
            onChange={(e) => setPublishedYear(Number(e.target.value) || 0)}
            className="input"
            disabled={previewBook !== null}
          />
        </div>

        <div className="fieldGroup">
          <label className="label">Leesniveau</label>
          <select
            value={readingLevel}
            onChange={(e) => setReadingLevel(e.target.value)}
            className="select"
            disabled={previewBook !== null}
          >
            <option value="">leesniveau</option>
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="C">C</option>
            <option value="D">D</option>
          </select>
        </div>

        <div className="fieldGroup">
          <label className="label">Didactisch boek</label>
          <select
            value={String(didacticTag)}
            onChange={(e) => setDidacticTag(e.target.value === "true")}
            className="select"
            disabled={previewBook !== null}
          >
            <option value="true">Ja</option>
            <option value="false">Nee</option>
          </select>
        </div>
        <div className="fieldGroup">
          <label className="label">Inventaris per school/campus</label>

          {campusLoadError && <p className="fieldError">{campusLoadError}</p>}

          {inventories.map((inventory, index) => (
            <div key={index} className="inventoryCard">
              <div className="inventoryGrid">
                <div className="inventoryField">
                  <label className="label">School</label>
                  <input
                    type="text"
                    value={inventory.schoolName || ""}
                    className="input"
                    disabled
                  />
                </div>

                <div className="inventoryField">
                  <label className="label">Campus</label>
                  <select
                    value={inventory.campus}
                    onChange={(e) =>
                      updateInventory(index, "campus", e.target.value)
                    }
                    className="select"
                    disabled={previewBook !== null || loadingCampuses}
                  >
                    <option value="">
                      {loadingCampuses ? "Campussen laden..." : "Geen campus"}
                    </option>

                    {getCampusSelectOptions(campuses, inventory.campus).map(
                      (campusOption) => (
                        <option
                          key={`${campusOption.id}-${campusOption.name}`}
                          value={campusOption.name}
                        >
                          {campusOption.name}
                        </option>
                      ),
                    )}
                  </select>
                </div>

                <div className="inventoryField">
                  <label className="label">Totaal</label>
                  <input
                    type="number"
                    min="0"
                    value={inventory.totalCopies}
                    onChange={(e) =>
                      updateInventory(index, "totalCopies", Number(e.target.value) || 0)
                    }
                    className="input"
                    disabled={previewBook !== null}
                  />
                </div>

                <div className="inventoryField">
                  <label className="label">Beschikbaar</label>
                  <input
                    type="number"
                    min="0"
                    value={inventory.availableCopies}
                    onChange={(e) =>
                      updateInventory(
                        index,
                        "availableCopies",
                        Number(e.target.value) || 0,
                      )
                    }
                    className="input"
                    disabled={previewBook !== null}
                  />
                </div>
              </div>

              {!previewBook && inventories.length > 1 && (
                <div className="inventoryActions">
                  <button
                    type="button"
                    onClick={() => removeInventoryRow(index)}
                    className="smallButton"
                  >
                    Verwijder regel
                  </button>
                </div>
              )}
            </div>
          ))}

          {!previewBook && (
            <div className="inventoryActions">
              <button
                type="button"
                onClick={addInventoryRow}
                className="smallButton"
              >
                Campus toevoegen
              </button>
            </div>
          )}

          <div className="inventorySummary">
            <strong>Totaal:</strong> {totalCopiesFromInventories} |{" "}
            <strong>Beschikbaar:</strong> {availableCopiesFromInventories}
          </div>
        </div>

        {!previewBook && (
          <button type="submit" disabled={loading} className={submitButtonClass}>
            {loading ? "Bezig..." : "Toon boek"}
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
                <strong>ISBN:</strong> {previewBook.isbn || "Geen"}
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
