"use client";

import { createContext, useContext, useEffect, useState } from "react";

type UserRole =
  | "STUDENT"
  | "TEACHER"
  | "BIBLIOTHEEKBEHEERDER"
  | "ADMIN"
  | "OTHER";

interface AuthUser {
  id: number;
  smartschoolUid: string;
  role: UserRole;
}

interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  loading: true,
});

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

export const useAuth = () => useContext(AuthContext);
