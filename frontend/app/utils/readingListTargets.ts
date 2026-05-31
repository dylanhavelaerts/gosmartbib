import type { ReadingListTargetType } from "../interfaces/ReadingList";

export interface ReadingListTargetFields {
  targetType?: ReadingListTargetType | null;

  targetStudentIds?: number[] | null;
  targetStudentDisplayNames?: string[] | null;

  targetClassIds?: number[] | null;
  targetClassNames?: string[] | null;

  targetYears?: number[] | null;
  targetGrades?: number[] | null;

  targetAllSchools?: boolean | null;
}

export const targetTypeLabel = (targetType?: ReadingListTargetType | null) => {
  switch (targetType) {
    case "STUDENTS":
      return "Specifieke leerling(en)";
    case "CLASSES":
      return "Specifieke klas(sen)";
    case "YEARS":
      return "Specifiek jaar";
    case "GRADES":
      return "Specifieke graad";
    default:
      return "Geen doelgroep";
  }
};

export const gradeLabel = (grade: number) => {
  switch (grade) {
    case 1:
      return "1ste graad";
    case 2:
      return "2de graad";
    case 3:
      return "3de graad";
    default:
      return `${grade}de graad`;
  }
};

export const yearLabel = (year: number) => `${year}de jaar`;

export const scopeLabel = (allSchools?: boolean | null) =>
  allSchools ? "alle scholen" : "eigen school";

/**
 * Bepaalt of er doelgroepen zijn ingesteld voor de leeslijst.
 * @param targets - de doelgroepvelden
 * @returns true als er doelgroepen zijn ingesteld, anders false
 */
export const hasReadingListTargets = (targets: ReadingListTargetFields) => {
  switch (targets.targetType) {
    case "STUDENTS":
      return Boolean(
        targets.targetStudentIds?.length ||
        targets.targetStudentDisplayNames?.length,
      );

    case "CLASSES":
      return Boolean(
        targets.targetClassIds?.length || targets.targetClassNames?.length,
      );

    case "YEARS":
      return Boolean(targets.targetYears?.length);

    case "GRADES":
      return Boolean(targets.targetGrades?.length);

    default:
      return Boolean(
        targets.targetStudentIds?.length ||
        targets.targetStudentDisplayNames?.length ||
        targets.targetClassIds?.length ||
        targets.targetClassNames?.length ||
        targets.targetYears?.length ||
        targets.targetGrades?.length,
      );
  }
};

/**
 * Formatteert de doelgroepen voor de leeslijst
 * Doet dit via een leesbare tekst die de ingestelde doelgroepen beschrijft, afhankelijk van het type doelgroep dat is ingesteld
 * @param targets - de doelgroepvelden
 * @returns de geformatteerde doelgroepen
 */
export const formatReadingListTargets = (targets: ReadingListTargetFields) => {
  switch (targets.targetType) {
    case "STUDENTS": {
      if (targets.targetStudentDisplayNames?.length) {
        return `Leerlingen: ${targets.targetStudentDisplayNames.join(", ")}`;
      }

      if (targets.targetStudentIds?.length) {
        return `Leerlingen: ${targets.targetStudentIds.length} geselecteerd`;
      }

      return "Geen leerlingen ingesteld";
    }

    case "CLASSES": {
      if (targets.targetClassNames?.length) {
        return `Klassen: ${targets.targetClassNames.join(", ")}`;
      }

      if (targets.targetClassIds?.length) {
        return `Klassen: ${targets.targetClassIds.length} geselecteerd`;
      }

      return "Geen klassen ingesteld";
    }

    case "YEARS": {
      if (!targets.targetYears?.length) {
        return "Geen jaren ingesteld";
      }

      return `Jaren: ${targets.targetYears
        .map(yearLabel)
        .join(", ")} (${scopeLabel(targets.targetAllSchools)})`;
    }

    case "GRADES": {
      if (!targets.targetGrades?.length) {
        return "Geen graden ingesteld";
      }

      return `Graden: ${targets.targetGrades
        .map(gradeLabel)
        .join(", ")} (${scopeLabel(targets.targetAllSchools)})`;
    }

    default:
      return "Geen doelgroep ingesteld";
  }
};

/**
 * Wisselt een nummer in een lijst in of uit
 * @param value het nummer dat moet worden gewijzigd
 * @param current de huidige lijst van nummers
 * @returns de bijgewerkte lijst van nummers
 */
export const toggleNumberInList = (value: number, current: number[]) => {
  return current.includes(value)
    ? current.filter((item) => item !== value)
    : [...current, value].sort((a, b) => a - b);
};

/**
 * Verwijdert alle waarden uit het doelgroep-payload, behalve het type doelgroep zelf
 * Dit is handig bij het wisselen van type doelgroep,
 * zodat er geen verouderde waarden in het payload blijven staan die mogelijk voor verwarring kunnen zorgen
 * @returns het lege doelgroep-payload
 */
export const clearTargetPayload = () => ({
  targetType: null,
  targetStudentIds: [],
  targetClassIds: [],
  targetYears: [],
  targetGrades: [],
  targetAllSchools: false,
});

/**
 * Houdt alleen de doelgroepvelden over die bij het gekozen doelgroeptype horen.
 *
 * Dit voorkomt dat oude formulierselecties per ongeluk meegestuurd worden wanneer
 * de gebruiker tussen doelgroepmodi wisselt.
 */
export const cleanTargetPayloadForType = (
  targetType: ReadingListTargetType,
  values: {
    targetStudentIds: number[];
    targetClassIds: number[];
    targetYears: number[];
    targetGrades: number[];
    targetAllSchools: boolean;
  },
) => {
  switch (targetType) {
    case "STUDENTS":
      return {
        targetType,
        targetStudentIds: values.targetStudentIds,
        targetClassIds: [],
        targetYears: [],
        targetGrades: [],
        targetAllSchools: false,
      };

    case "CLASSES":
      return {
        targetType,
        targetStudentIds: [],
        targetClassIds: values.targetClassIds,
        targetYears: [],
        targetGrades: [],
        targetAllSchools: false,
      };

    case "YEARS":
      return {
        targetType,
        targetStudentIds: [],
        targetClassIds: [],
        targetYears: values.targetYears,
        targetGrades: [],
        targetAllSchools: values.targetAllSchools,
      };

    case "GRADES":
      return {
        targetType,
        targetStudentIds: [],
        targetClassIds: [],
        targetYears: [],
        targetGrades: values.targetGrades,
        targetAllSchools: values.targetAllSchools,
      };
  }
};
