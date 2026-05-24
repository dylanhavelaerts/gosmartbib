package edu.ap.gosmartlib.util;

public enum UserRoles {
    STUDENT, // "Leerling"
    TEACHER, // "Leerkracht"
    BIBLIOTHEEKBEHEERDER, // Custom Role
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
            case "directie" -> BIBLIOTHEEKBEHEERDER; // moet mss weg als er geen directie word meegegeven
            case "bibliotheekbeheerder" -> BIBLIOTHEEKBEHEERDER;
            default -> OTHER;
        };
    }

    public static UserRoles fromOneRoster(String role) {
        if (role == null) return OTHER;
        return switch (role.toLowerCase()) {
            case "student" -> STUDENT;
            case "teacher" -> TEACHER;
//            case "administrator" -> ADMIN;
            default -> OTHER;
        };
    }

}
