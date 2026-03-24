"use client";

import { useEffect, useRef, useState } from "react";
import { AGE_RANGE, Book, BOOK_CATEGORIES, BOOK_LABELS } from "../../interfaces/Book";
import styles from "./addBookForm.module.css";

export default function AddBookWithoutIsbn() {
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
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
      didacticTag: false,
      readingLevel: "",
      labels: [],
      totalCopies: 0,
      availableCopies: 0,
      ageRange
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

    setLoading(true);
    setMessage("");

    try {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/books/add`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
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
          totalCopies: 1,
          availableCopies: 1,
          ageRange
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

  const submitButtonClass = `${styles.submitButton} ${loading ? styles.submitButtonLoading : ""}`.trim();
  const messageClass = `${styles.message} ${
    message.includes("succesvol")
      ? styles.messageSuccess
      : message.includes("Controleer")
        ? styles.messageInfo
        : styles.messageError
  }`.trim();

  return (
    <>
      <h1 className={styles.title}>Nieuw boek toevoegen zonder ISBN nummer</h1>

      <p className={styles.description}>
        Geef hier de nodige info om het boek aan te maken.
      </p>

      <form onSubmit={handlePreviewBook} className={styles.form}>
        <div className={styles.fieldGroup}>
          <label className={styles.label}>Titel</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titel van het boek"
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Auteur(s)</label>

          {authors.map((author, index) => (
            <input
              key={index}
              type="text"
              value={author}
              onChange={(e) => handleAuthorChange(index, e.target.value)}
              placeholder={`Auteur ${index + 1}`}
              className={`${styles.input} ${styles.authorInput}`}
              disabled={previewBook !== null}
            />
          ))}

          <div className={styles.authorButtons}>
            <button
              type="button"
              onClick={handleAddAuthorField}
              disabled={previewBook !== null}
              className={styles.smallButton}
            >
              Auteur toevoegen
            </button>

            <button
              type="button"
              onClick={handleRemoveAuthorField}
              disabled={previewBook !== null}
              className={styles.smallButton}
            >
              Auteur verwijderen
            </button>
          </div>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Uitgever</label>
          <input
            type="text"
            value={publisher}
            onChange={(e) => setPublisher(e.target.value)}
            placeholder="Uitgever van het boek"
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Omschrijving</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Omschrijving van het boek"
            rows={5}
            className={styles.textarea}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Aantal pagina's</label>
          <input
            type="number"
            value={pageCount}
            onChange={(e) => setPageCount(Number(e.target.value) || 0)}
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Categorieën</label>
          <div ref={dropdownRef} className={styles.dropdownWrapper}>
            <button
              type="button"
              onClick={() => setOpenDropdown(!openDropdown)}
              disabled={previewBook !== null}
              className={styles.dropdownToggle}
            >
              {categories.length !== 0
                ? categories.join(", ")
                : "Selecteer categorieën"}
            </button>

            {openDropdown && (
              <div className={styles.dropdownPanel}>
                {BOOK_CATEGORIES.map((category) => (
                  <label key={category} className={styles.checkboxLabel}>
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

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Leefwereldlabels</label>
          <div ref={dropdownRefLabel} className={styles.dropdownWrapper}>
            <button
              type="button"
              onClick={() => setOpenLabelDropdown(!openLabelDropdown)}
              disabled={previewBook !== null}
              className={styles.dropdownToggle}
            >
              {labels.length !== 0 ? labels.join(", ") : "Selecteer labels"}
            </button>

            {openLabelDropdown && (
              <div className={styles.dropdownPanel}>
                {BOOK_LABELS.map((label) => (
                  <label key={label} className={styles.checkboxLabel}>
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

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Foto</label>
          <input
            type="text"
            value={thumbnail}
            onChange={(e) => setThumbnail(e.target.value)}
            placeholder="Url voor een foto van de voorpagina"
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Taal</label>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            className={styles.select}
            disabled={previewBook !== null}
          >
            <option value="">Alle talen</option>
            <option value="en">EN</option>
            <option value="ne">NE</option>
            <option value="fr">FR</option>
          </select>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Rating</label>
          <input
            type="number"
            min="0"
            max="5"
            step="0.1"
            value={rating}
            onChange={(e) => setRating(Number(e.target.value) || 0)}
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Jaar van uitgave</label>
          <input
            type="number"
            value={publishedYear}
            onChange={(e) => setPublishedYear(Number(e.target.value) || 0)}
            className={styles.input}
            disabled={previewBook !== null}
          />
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Leesniveau</label>
          <select
            value={readingLevel}
            onChange={(e) => setReadingLevel(e.target.value)}
            className={styles.select}
            disabled={previewBook !== null}
          >
            <option value="">leesniveau</option>
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="C">C</option>
            <option value="D">D</option>
          </select>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Leeftijd</label>
          <select
            value={ageRange}
            onChange={(e) => setAgeRange(e.target.value)}
            className={styles.select}
            disabled={previewBook !== null}
          >
            <option value="">leeftijd</option>
            <option value="Eerste graad">Eerste graad</option>
            <option value="Tweede graad">Tweede graad</option>
            <option value="Derde graad">Derde graad</option>
          </select>
        </div>

        <div className={styles.fieldGroup}>
          <label className={styles.label}>Didactisch boek</label>
          <select
            value={String(didacticTag)}
            onChange={(e) => setDidacticTag(e.target.value === "true")}
            className={styles.select}
            disabled={previewBook !== null}
          >
            <option value="true">Ja</option>
            <option value="false">Nee</option>
          </select>
        </div>

        {!previewBook && (
          <button type="submit" disabled={loading} className={submitButtonClass}>
            {loading ? "Bezig..." : "Toon boek"}
          </button>
        )}
      </form>

      {previewBook && (
        <div className={styles.previewCard}>
          <h2 className={styles.previewTitle}>Preview van het boek:</h2>

          <div className={styles.previewContent}>
            <img
              src={imgSrc}
              alt="Cover"
              onError={() => setImgSrc("/No-Image-Available-Placeholder.png")}
              className={styles.previewImage}
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

          <div className={styles.actionRow}>
            <button
              type="button"
              onClick={handleConfirmAdd}
              disabled={loading}
              className={styles.confirmButton}
            >
              {loading ? "Bezig..." : "Ja, Voeg toe aan Catalogus"}
            </button>

            <button
              type="button"
              onClick={handleCancelPreview}
              disabled={loading}
              className={styles.cancelButton}
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
