"use client";

import { createContext, useContext, useEffect, useState } from "react";

/**
 * Rollen die de frontend gebruikt om navigatie en client-side routes te sturen.
 *
 * De backend blijft de beslissende beveiligingslaag. Deze rollen worden alleen
 * gebruikt voor UI-keuzes en gebruiksgemak.
 */

type UserRole =
  | "STUDENT"
  | "TEACHER"
  | "BIBLIOTHEEKBEHEERDER"
  | "ADMIN"
  | "OTHER";

/**
 * Frontendrepresentatie van de huidige gebruiker.
 *
 * Deze structuur komt overeen met de UserDTO die de backend via /auth/me
 * teruggeeft. De frontend leest geen sessiecookies rechtstreeks, omdat die
 * HttpOnly zijn.
 */

interface AuthUser {
  id: number;
  smartschoolUid?: string;
  role: UserRole;
  school?: { id: number; name: string; domain: string } | null;
  classes?: {
    id?: number;
    name?: string | null;
    grade?: string | null;
    schoolYear?: string | null;
  }[];
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
});

/**
 * AuthProvider controleert bij het laden van de applicatie of er een actieve
 * backendsessie bestaat.
 *
 * De call naar /auth/me gebruikt credentials: "include", zodat de browser de
 * HttpOnly sessiecookies meestuurt. De response bepaalt de frontend user state.
 * Buiten localhost en publieke routes wordt een gebruiker zonder sessie naar
 * /login gestuurd.
 */

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const isLocalHost =
      window.location.hostname === "localhost" ||
      window.location.hostname === "127.0.0.1";
    const pathname = window.location.pathname;
    const isPublicRoute =
      pathname === "/login" ||
      pathname.startsWith("/smartschool") ||
      pathname.startsWith("/public");

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/me`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        setUser(data);

        if (!data && !isLocalHost && !isPublicRoute) {
          window.location.replace("/login");
        }
      })
      .catch(() => {
        setUser(null);

        if (!isLocalHost && !isPublicRoute) {
          window.location.replace("/login");
        }
      })
      .finally(() => setLoading(false));
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * Hook om de huidige authenticatiestatus op te vragen.
 *
 * Components gebruiken deze hook om de ingelogde gebruiker, rol en loading state
 * uit AuthContext te lezen.
 */

export const useAuth = () => useContext(AuthContext);
