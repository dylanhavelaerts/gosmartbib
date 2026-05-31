package edu.ap.gosmartlib.util;

public enum UserRoles {
    STUDENT, // "Leerling"
    TEACHER, // "Leerkracht"
    LIBRARIAN, // Custom Role
    ADMIN, // "Overkoepelende platform-administrator"
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
            case "directie" -> LIBRARIAN; // in geval dat "Directie" bestaat, automatisch toewijzen aan LIBRARIAN
            case "librarian" -> LIBRARIAN;
            default -> OTHER;
        };
    }

    /**
     * Converteert een OneRoster rol naar een UserRoles enum
     * @param role de rol van OneRoster (bijv. "student", "teacher")
     * @return de bijbehorende UserRoles enum, of OTHER als de rol onbekend is
     */
    public static UserRoles fromOneRoster(String role) {
        if (role == null) return OTHER;
        return switch (role.toLowerCase()) {
            case "student" -> STUDENT;
            case "teacher" -> TEACHER;
            default -> OTHER;
        };
    }

}
