package edu.ap.gosmartlib.util;

public enum UserRoles {
    STUDENT, // "Leerling"
    TEACHER, // "Leerkracht"
    BIBLIOTHEEKBEHEERDER, // Custom Role
    ADMIN, // "Directie"
    OTHER; // alle andere rollen

    /**
     * Converteert een Smartschool basisrol naar een UserRoles enum.
     * @param basisrol de basisrol van Smartschool (bijv. "Leerling", "Leerkracht",
     *                 "Directie")
     * @return de bijbehorende UserRoles enum, of OTHER als de basisrol onbekend is
     */
    public static UserRoles fromSmartschool(String basisrol) {
        if (basisrol == null)
            return OTHER;
        return switch (basisrol.toLowerCase()) {
            case "leerling" -> STUDENT;
            case "leerkracht" -> TEACHER;
            case "directie" -> ADMIN;
            case "bibliotheekbeheerder" -> BIBLIOTHEEKBEHEERDER;
            default -> OTHER;
        };
    }
}
