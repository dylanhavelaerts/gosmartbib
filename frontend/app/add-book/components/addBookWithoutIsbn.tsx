"use client";

import { useEffect, useRef, useState } from "react";
import type { Book } from "../../interfaces/Book";

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
  const [labels, setLabels] = useState<String[]>([]);
  const [readingLevel, setReadingLevel] = useState("")
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png")

  const categoryChoice = [
    "Fictie algemeen",
    "Literaire roman",
    "Spanning / thriller",
    "Detective / misdaad",
    "Fantasy",
    "Science fiction",
    "Dystopie",
    "Historische roman",
    "Romantiek",
    "Coming-of-age",
    "Avontuur",
    "Oorlog & conflict",
    "Horror",
    "Humor",
    "Graphic Novel / strip",
    "Poëzie",
    "Non-fictie algemeen"
  ];

  const labelChoice = [
    "Liefde & relatie",
    "Vriendschap",
    "Identiteit & zelfbeeld",
    "Gender & seksualiteit",
    "Diversiteit & inclusie",
    "Mentale gezondheid",
    "Rouw & verlies",
    "Familie",
    "School & prestatiedruk",
    "Sociale media",
    "Migratie & afkomst",
    "Armoede & ongelijkheid",
    "Macht & onrecht",
    "Avontuur & ontdekking",
    "Overleven",
    "Toekomst & technologie"
  ]

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
  }, [previewBook])

  return (
    <>
      <h1 style={{ fontSize: "2rem", marginBottom: "1rem" }}>
        Nieuw boek toevoegen zonder ISBN nummer
      </h1>

      <p style={{ marginBottom: "2rem", color: "#555" }}>
        Geef hier de nodige info om het boek aan te maken.
      </p>

      <form
        onSubmit={handlePreviewBook}
        style={{ display: "flex", flexDirection: "column", gap: "1rem" }}
      >
        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Titel
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Titel van het boek"
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Auteur(s)
          </label>

          {authors.map((author, index) => (
            <input
              key={index}
              type="text"
              value={author}
              onChange={(e) => handleAuthorChange(index, e.target.value)}
              placeholder={`Auteur ${index + 1}`}
              style={{
                width: "100%",
                padding: "0.75rem",
                fontSize: "1rem",
                borderRadius: "4px",
                border: "1px solid #8e2446",
                background: "white",
                color: "#8e2446",
                marginBottom: "0.1rem",
                boxSizing: "border-box",
              }}
              disabled={previewBook !== null}
            />
          ))}

          <button
            type="button"
            onClick={handleAddAuthorField}
            disabled={previewBook !== null}
            style={{
              padding: "0.75rem 1rem",
              backgroundColor: "#8e2446",
              color: "white",
              border: "none",
              borderRadius: "4px",
              fontWeight: "bold",
              cursor: "pointer",
              marginTop:"0.8rem"
            }}
          >
            Auteur toevoegen
          </button>

          <button
            type="button"
            onClick={handleRemoveAuthorField}
            disabled={previewBook !== null}
            style={{
              padding: "0.75rem 1rem",
              backgroundColor: "#8e2446",
              color: "white",
              border: "none",
              borderRadius: "4px",
              fontWeight: "bold",
              cursor: "pointer",
              marginLeft: "1rem",
            }}
          >
            Auteur verwijderen
          </button>
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Uitgever
          </label>
          <input
            type="text"
            value={publisher}
            onChange={(e) => setPublisher(e.target.value)}
            placeholder="Uitgever van het boek"
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Omschrijving
          </label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Omschrijving van het boek"
            rows={5}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              resize: "none",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Aantal pagina's
          </label>
          <input
            type="number"
            value={pageCount}
            onChange={(e) => setPageCount(Number(e.target.value) || 0)}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Categorieën
          </label>
          <div ref={dropdownRef} style={{width: "100%"}}>
            <button
              type="button"
              onClick={() => setOpenDropdown(!openDropdown)}
              disabled={previewBook !== null}
              style={{
                width: "100%",
                padding: "0.75rem",
                fontSize: "1rem",
                borderRadius: "4px",
                border: "1px solid #8e2446",
                background: "white",
                color: "#8e2446",
                minHeight: "20px",
                textAlign: "left",
                cursor: "pointer",
                boxSizing: "border-box",
              }}
            >
              {categories.length !== 0
                ? categories.join(", ")
                : "Selecteer categorieën"}
            </button>

            {openDropdown && (
              <div
                style={{
                  borderRadius: "4px",
                  border: "1px solid #8e2446",
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  backgroundColor: "white",
                  marginTop: "0.5rem",
                  boxSizing: "border-box",
                }}
              >
                {categoryChoice.map((category) => (
                  <label
                    key={category}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                      padding: "0.5rem 0.75rem",
                      color: "black",
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={categories.includes(category)}
                      onChange={() => {
                        setCategories((prev) =>
                          prev.includes(category)
                            ? prev.filter((c) => c !== category)
                            : [...prev, category]
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

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Leefwereldlabels
          </label>
          <div ref={dropdownRefLabel} style={{width: "100%"}}>
            <button
              type="button"
              onClick={() => setOpenLabelDropdown(!openLabelDropdown)}
              disabled={previewBook !== null}
              style={{
                width: "100%",
                padding: "0.75rem",
                fontSize: "1rem",
                borderRadius: "4px",
                border: "1px solid #8e2446",
                background: "white",
                color: "#8e2446",
                minHeight: "20px",
                textAlign: "left",
                cursor: "pointer",
                boxSizing: "border-box",
              }}
            >
              {labels.length !== 0
                ? labels.join(", ")
                : "Selecteer labels"}
            </button>

            {openLabelDropdown && (
              <div
                style={{
                  borderRadius: "4px",
                  border: "1px solid #8e2446",
                  display: "flex",
                  flexDirection: "column",
                  width: "100%",
                  backgroundColor: "white",
                  marginTop: "0.5rem",
                  boxSizing: "border-box",
                }}
              >
                {labelChoice.map((label) => (
                  <label
                    key={label}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.5rem",
                      padding: "0.5rem 0.75rem",
                      color: "black",
                    }}
                  >
                    <input
                      type="checkbox"
                      checked={labels.includes(label)}
                      onChange={() => {
                        setLabels((prev) =>
                          prev.includes(label)
                            ? prev.filter((c) => c !== label)
                            : [...prev, label]
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

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Foto
          </label>
          <input
            type="text"
            value={thumbnail}
            onChange={(e) => setThumbnail(e.target.value)}
            placeholder="Url voor een foto van de voorpagina"
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Taal
          </label>
          <select
            value={language}
            onChange={(e) => setLanguage(e.target.value)}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          >
            <option value="">Alle talen</option>
            <option value="en">EN</option>
            <option value="ne">NE</option>
            <option value="fr">FR</option>
          </select>
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Rating
          </label>
          <input
            type="number"
            min="0"
            max="5"
            step="0.1"
            value={rating}
            onChange={(e) => setRating(Number(e.target.value) || 0)}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>

        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Jaar van uitgave
          </label>
          <input
            type="number"
            value={publishedYear}
            onChange={(e) => setPublishedYear(Number(e.target.value) || 0)}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          />
        </div>
        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Leesniveau
          </label>
         <select
            value={readingLevel}
            onChange={(e) => setReadingLevel(e.target.value)}
            style={{
              width: "100%",
              padding: "0.75rem",
              fontSize: "1rem",
              borderRadius: "4px",
              border: "1px solid #8e2446",
              background: "white",
              color: "#8e2446",
              boxSizing: "border-box",
            }}
            disabled={previewBook !== null}
          >
            <option value="">leesniveau</option>
            <option value="A">A</option>
            <option value="B">B</option>
            <option value="C">C</option>
            <option value="D">D</option>
          </select>
        </div>
        
        <div>
          <label
            style={{
              display: "block",
              marginBottom: "0.5rem",
              fontWeight: "bold",
            }}
          >
            Didactisch boek
          </label>
            <select
              value={String(didacticTag)}
              onChange={(e) => setDidacticTag(e.target.value === "true")}
              style={{
                width: "100%",
                padding: "0.75rem",
                fontSize: "1rem",
                borderRadius: "4px",
                border: "1px solid #8e2446",
                background: "white",
                color: "#8e2446",
                boxSizing: "border-box",
              }}
              disabled={previewBook !== null}
            >
              <option value="true">Ja</option>
              <option value="false">Nee</option>
            </select>
          </div>

        {!previewBook && (
          <button
            type="submit"
            disabled={loading}
            style={{
              cursor: loading ? "not-allowed" : "pointer",
              backgroundColor: loading ? "#ccc" : "#8e2446",
              color: "white",
              border: "none",
              borderRadius: "4px",
              fontWeight: "bold",
              flex: 1,
              padding: "0.75rem",
              maxWidth: "14rem",
            }}
          >
            {loading ? "Bezig..." : "Toon boek"}
          </button>
        )}
      </form>

      {previewBook && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1.5rem",
            border: "2px solid #8e2446",
            borderRadius: "8px",
            backgroundColor: "#f9f9f9",
            color: "black",
          }}
        >
          <h2 style={{ marginTop: 0 }}>Preview van het boek:</h2>

          <div style={{ display: "flex", gap: "1rem", marginTop: "1rem" }}>
              <img
                src={imgSrc}
                alt="Cover"
                onError={() => setImgSrc("/No-Image-Available-Placeholder.png")}
                style={{
                  width: "100px",
                  height: "150px",
                  objectFit: "cover",
                  borderRadius: "4px",
                }}
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

          <div style={{ display: "flex", gap: "1rem", marginTop: "1.5rem" }}>
            <button
              type="button"
              onClick={handleConfirmAdd}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#28a745",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              {loading ? "Bezig..." : "Ja, Voeg toe aan Catalogus"}
            </button>

            <button
              type="button"
              onClick={handleCancelPreview}
              disabled={loading}
              style={{
                flex: 1,
                padding: "0.75rem",
                backgroundColor: "#dc3545",
                color: "white",
                border: "none",
                borderRadius: "4px",
                fontWeight: "bold",
                cursor: "pointer",
              }}
            >
              Annuleren
            </button>
          </div>
        </div>
      )}

      {message && (
        <div
          style={{
            marginTop: "2rem",
            padding: "1rem",
            backgroundColor: message.includes("succesvol")
              ? "#d4edda"
              : message.includes("Controleer")
                ? "#cce5ff"
                : "#f8d7da",
            color: message.includes("succesvol")
              ? "#155724"
              : message.includes("Controleer")
                ? "#004085"
                : "#721c24",
            borderRadius: "4px",
          }}
        >
          {message}
        </div>
      )}
    </>
  );
}