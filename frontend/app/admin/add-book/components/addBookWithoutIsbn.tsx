"use client";

import { useEffect, useRef, useState } from "react";
import {
  BOOK_CATEGORIES,
  BOOK_LABELS,
  BOOK_LANGUAGE_PRESETS,
} from "../../../interfaces/Book";
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
  const [availableCategories, setAvailableCategories] =
    useState<string[]>(BOOK_CATEGORIES);
  const [newCategory, setNewCategory] = useState("");
  const [thumbnail, setThumbnail] = useState("");
  const [language, setLanguage] = useState("");
  const [languageInputMode, setLanguageInputMode] = useState("");
  const [publishedYear, setPublishedYear] = useState(0);
  const [openDropdown, setOpenDropdown] = useState(false);
  const [openLabelDropdown, setOpenLabelDropdown] = useState(false);
  const [didacticTag, setDidacticTag] = useState(false);
  const [labels, setLabels] = useState<string[]>([]);
  const [availableLabels, setAvailableLabels] = useState<string[]>(BOOK_LABELS);
  const [newLabel, setNewLabel] = useState("");
  const [readingLevel, setReadingLevel] = useState("");
  const [imgSrc, setImgSrc] = useState("/No-Image-Available-Placeholder.png");
  const [ageRange, setAgeRange] = useState("");
  const [me, setMe] = useState<MeResponse | null>(null);
  const [campuses, setCampuses] = useState<SchoolCampusDTO[]>([]);
  const [inventories, setInventories] = useState<BookInventory[]>([]);

  const API_URL = process.env.NEXT_PUBLIC_API_URL;

  const CUSTOM_LANGUAGE_VALUE = "__custom_language__";

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

  /**
   * Voorbeeld van een boek genereren via de huidige invoer van de gebruiker
   * Doet dit door een boekobject samen te stellen met de huidige staat van de invoervelden, en dit in te stellen als het previewBook
   */
  const handlePreviewBook = (e: React.FormEvent) => {
    e.preventDefault();

    if (
      campuses.length > 1 &&
      inventories.some((inventory) => !inventory.campus.trim())
    ) {
      setMessage("Kies voor elke inventarisregel een campus.");
      return;
    }
    if (hasDuplicateInventoryCampuses()) {
      setMessage("Elke campus mag maar één keer voorkomen.");
      return;
    }

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
      rating: 0,
      publishedYear,
      spotlight: false,
      didacticTag,
      readingLevel,
      labels,
      totalCopies: totalCopiesFromInventories,
      availableCopies: availableCopiesFromInventories,
      ageRange,
      inventories,
    };

    setPreviewBook(book);
    setMessage("Controleer de gegevens hieronder");
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

  const languageSelectValue =
    languageInputMode === CUSTOM_LANGUAGE_VALUE
      ? CUSTOM_LANGUAGE_VALUE
      : language.toLowerCase();

  const handleLanguageSelectChange = (value: string) => {
    setLanguageInputMode(value);

    if (value === CUSTOM_LANGUAGE_VALUE) {
      setLanguage("");
      return;
    }

    setLanguage(value);
  };

  function mergeOptions(baseOptions: string[], databaseOptions: string[]) {
    const mergedOptions: string[] = [];
    const seenOptions = new Set<string>();

    [...baseOptions, ...databaseOptions].forEach((option) => {
      const trimmedOption = option.trim();

      if (!trimmedOption) {
        return;
      }

      const normalizedOption = trimmedOption.toLowerCase();

      if (!seenOptions.has(normalizedOption)) {
        seenOptions.add(normalizedOption);
        mergedOptions.push(trimmedOption);
      }
    });

    return mergedOptions.sort((a, b) => a.localeCompare(b));
  }

  function isOptionSelected(values: string[], option: string) {
    return values.some((value) => value.toLowerCase() === option.toLowerCase());
  }

  function toggleOption(
    setSelectedValues: React.Dispatch<React.SetStateAction<string[]>>,
    option: string,
  ) {
    setSelectedValues((prev) =>
      isOptionSelected(prev, option)
        ? prev.filter((value) => value.toLowerCase() !== option.toLowerCase())
        : [...prev, option],
    );
  }

  /**
   * Voegt een aangepaste optie toe aan de lijst van beschikbare opties
   * Doet dit door de waarde te trimmen, te controleren of deze al bestaat (case-insensitive),
   * en indien niet, toe te voegen aan de beschikbare opties en te selecteren
   * @param value - De waarde van de nieuwe optie
   * @param setValue - Functie om de waarde van de input te updaten
   * @param setSelectedValues - Functie om de geselecteerde waarden te updaten
   * @param setAvailableOptions - Functie om de beschikbare opties te updaten
   */
  function addCustomOption(
    value: string,
    setValue: React.Dispatch<React.SetStateAction<string>>,
    setSelectedValues: React.Dispatch<React.SetStateAction<string[]>>,
    setAvailableOptions: React.Dispatch<React.SetStateAction<string[]>>,
  ) {
    const trimmedValue = value.trim();

    if (!trimmedValue) {
      return;
    }

    setAvailableOptions((prev) =>
      prev.some((option) => option.toLowerCase() === trimmedValue.toLowerCase())
        ? prev
        : [...prev, trimmedValue].sort((a, b) => a.localeCompare(b)),
    );

    setSelectedValues((prev) =>
      isOptionSelected(prev, trimmedValue) ? prev : [...prev, trimmedValue],
    );

    setValue("");
  }

  /**
   * Verwerkt het bevestigen van het toevoegen van een nieuw boek
   * Doet dit via een API-aanroep en geeft feedback aan de gebruiker over het resultaat
   * <ul>
   *   <li>Bij een succesvolle toevoeging wordt de gebruiker doorgestuurd naar de bewerkingspagina van het nieuwe boek</li>
   *   <li>Bij een fout wordt er een foutmelding weergegeven</li>
   * </ul>
   */
  const handleConfirmAdd = async () => {
    if (!previewBook) return;

    if (inventories.length === 0) {
      setMessage("Voeg minstens één inventarisregel toe");
      return;
    }

    for (const inventory of inventories) {
      if (!inventory.schoolId) {
        setMessage("Elke inventarisregel moet een school hebben");
        return;
      }

      if (campuses.length > 1 && !inventory.campus.trim()) {
        setMessage("Kies voor elke inventarisregel een campus.");
        return;
      }

      if (inventory.totalCopies < 0 || inventory.availableCopies < 0) {
        setMessage("Aantallen mogen niet negatief zijn");
        return;
      }

      if (inventory.availableCopies > inventory.totalCopies) {
        setMessage("Beschikbare exemplaren mogen niet groter zijn dan totaal");
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
        },
        credentials: "include",
        body: JSON.stringify({
          title,
          authors: authors.filter((author) => author.trim() !== ""),
          publisher,
          description,
          pageCount,
          categories,
          thumbnail,
          language,
          rating: 0,
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
            campus: campuses.length === 1 ? campuses[0].name : inventory.campus,
            totalCopies: inventory.totalCopies,
            availableCopies: inventory.availableCopies,
          })),
        }),
      });

      const data = await response.json().catch(() => null);

      if (!response.ok) {
        setMessage(
          data?.message || "Er ging iets mis bij het opslaan van het boek",
        );
        return;
      }

      setMessage(`Boek succesvol aan de database toegevoegd: "${data.title}"`);
      setPreviewBook(null);

      setTitle("");
      setAuthors([""]);
      setPublisher("");
      setDescription("");
      setPageCount(0);
      setCategories([]);
      setNewCategory("");
      setThumbnail("");
      setLanguage("");
      setLanguageInputMode("");
      setPublishedYear(0);
      setOpenDropdown(false);
      setOpenLabelDropdown(false);
      setDidacticTag(false);
      setLabels([]);
      setNewLabel("");
      setReadingLevel("");
      setAgeRange("");
      setInventories([
        createEmptyInventory(
          me?.school ?? null,
          campuses.length === 1 ? campuses[0].name : "",
        ),
      ]);
    } catch (error) {
      console.error(error);
      setMessage("Kan de server niet bereiken");
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

  const submitButtonClass =
    `submitButton ${loading ? "submitButtonLoading" : ""}`.trim();
  const messageClass = `message ${
    message.includes("succesvol")
      ? "messageSuccess"
      : message.includes("Controleer")
        ? "messageInfo"
        : "messageError"
  }`.trim();

  const createEmptyInventory = (
    school: MeResponse["school"] | null,
    campusName = "",
  ): BookInventory => ({
    id: null,
    schoolId: school?.id ?? null,
    schoolName: school?.name ?? "",
    campus: campusName,
    totalCopies: 1,
    availableCopies: 1,
  });

  const shouldShowCampusSelect = campuses.length > 1;

  const canAddInventoryRow =
    campuses.length > 0 && inventories.length < campuses.length;

  const shouldShowAddCampusButton =
    shouldShowCampusSelect && canAddInventoryRow;

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
    // Add extra campus row
    setInventories((prev) => [
      ...prev,
      createEmptyInventory(me?.school ?? null, getNextAvailableCampusName()),
    ]);
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
          const campusData = await fetchSchoolCampuses(API_URL, data.school.id);
          setCampuses(campusData);
          setInventories([
            createEmptyInventory(
              data.school,
              campusData.length === 1 ? campusData[0].name : "",
            ),
          ]);
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

  useEffect(() => {
    if (!API_URL) return;

    const fetchOptions = async (endpoint: string) => {
      const response = await fetch(`${API_URL}/books/${endpoint}`, {
        credentials: "include",
      });

      if (!response.ok) {
        return [];
      }

      const data = await response.json();
      return Array.isArray(data) ? data : [];
    };

    Promise.all([fetchOptions("categories"), fetchOptions("labels")])
      .then(([databaseCategories, databaseLabels]) => {
        setAvailableCategories(
          mergeOptions(BOOK_CATEGORIES, databaseCategories),
        );
        setAvailableLabels(mergeOptions(BOOK_LABELS, databaseLabels));
      })
      .catch(() => {
        setAvailableCategories(BOOK_CATEGORIES);
        setAvailableLabels(BOOK_LABELS);
      });
  }, [API_URL]);

  function getUsedCampusNames() {
    return inventories
      .map((inventory) => inventory.campus?.trim().toLowerCase() ?? "")
      .filter((campusName) => campusName !== "");
  }

  /**
   * Bepaalt de volgende beschikbare campusnaam
   * @returns de volgende beschikbare campusnaam
   */
  function getNextAvailableCampusName() {
    const usedCampusNames = getUsedCampusNames();

    return (
      campuses.find(
        (campusOption) =>
          !usedCampusNames.includes(campusOption.name.trim().toLowerCase()),
      )?.name ?? ""
    );
  }

  /**
   * Controleert of er dubbele campussen zijn geselecteerd in de inventaris
   * @returns true als er dubbele campussen zijn, anders false
   */
  const hasDuplicateInventoryCampuses = () => {
    const selectedCampuses = inventories
      .map((inventory) => inventory.campus.trim().toLowerCase())
      .filter(Boolean);

    return new Set(selectedCampuses).size !== selectedCampuses.length;
  };

  return (
    <>
      <h1 className="title">Nieuw boek toevoegen zonder ISBN nummer</h1>

      <p className="description">
        Geef hier de nodige info om het boek aan te maken
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
                {availableCategories.map((category) => (
                  <label key={category} className="checkboxLabel">
                    <input
                      type="checkbox"
                      checked={isOptionSelected(categories, category)}
                      onChange={() => toggleOption(setCategories, category)}
                      disabled={previewBook !== null}
                    />
                    <span>{category}</span>
                  </label>
                ))}
                <div className="customOptionRow">
                  <input
                    type="text"
                    className="customOptionInput"
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        addCustomOption(
                          newCategory,
                          setNewCategory,
                          setCategories,
                          setAvailableCategories,
                        );
                      }
                    }}
                    placeholder="Nieuwe categorie"
                    disabled={previewBook !== null}
                  />

                  <button
                    type="button"
                    className="customOptionButton"
                    onClick={() =>
                      addCustomOption(
                        newCategory,
                        setNewCategory,
                        setCategories,
                        setAvailableCategories,
                      )
                    }
                    disabled={previewBook !== null}
                  >
                    Toevoegen
                  </button>
                </div>
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
                {availableLabels.map((label) => (
                  <label key={label} className="checkboxLabel">
                    <input
                      type="checkbox"
                      checked={isOptionSelected(labels, label)}
                      onChange={() => toggleOption(setLabels, label)}
                      disabled={previewBook !== null}
                    />
                    <span>{label}</span>
                  </label>
                ))}

                <div className="customOptionRow">
                  <input
                    type="text"
                    className="customOptionInput"
                    value={newLabel}
                    onChange={(e) => setNewLabel(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        addCustomOption(
                          newLabel,
                          setNewLabel,
                          setLabels,
                          setAvailableLabels,
                        );
                      }
                    }}
                    placeholder="Nieuw leefwereldlabel"
                    disabled={previewBook !== null}
                  />

                  <button
                    type="button"
                    className="customOptionButton"
                    onClick={() =>
                      addCustomOption(
                        newLabel,
                        setNewLabel,
                        setLabels,
                        setAvailableLabels,
                      )
                    }
                    disabled={previewBook !== null}
                  >
                    Toevoegen
                  </button>
                </div>
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
            value={languageSelectValue}
            onChange={(e) => handleLanguageSelectChange(e.target.value)}
            className="select"
            disabled={previewBook !== null}
          >
            <option value="">Kies een taal</option>
            {BOOK_LANGUAGE_PRESETS.map((languagePreset) => (
              <option key={languagePreset} value={languagePreset}>
                {languagePreset.toUpperCase()}
              </option>
            ))}
            <option value={CUSTOM_LANGUAGE_VALUE}>Andere taal...</option>
          </select>

          {languageInputMode === CUSTOM_LANGUAGE_VALUE && (
            <input
              type="text"
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              placeholder="Geef een afkorting van een taal in"
              className="input"
              disabled={previewBook !== null}
            />
          )}
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
          <label className="label">
            {shouldShowCampusSelect
              ? "Inventaris per school/campus"
              : "Inventaris"}
          </label>

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

                {shouldShowCampusSelect && (
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
                        {loadingCampuses
                          ? "Campussen laden..."
                          : "Kies een campus"}
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
                )}

                <div className="inventoryField">
                  <label className="label">Totaal</label>
                  <input
                    type="number"
                    min="0"
                    value={inventory.totalCopies}
                    onChange={(e) =>
                      updateInventory(
                        index,
                        "totalCopies",
                        Number(e.target.value) || 0,
                      )
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
            </div>
          ))}

          {!previewBook && shouldShowAddCampusButton && (
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
          <button
            type="submit"
            disabled={
              loading ||
              loadingCampuses ||
              Boolean(campusLoadError) ||
              (shouldShowCampusSelect &&
                inventories.some((inventory) => !inventory.campus.trim()))
            }
            className={submitButtonClass}
          >
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
