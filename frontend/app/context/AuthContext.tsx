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

    fetch(`${process.env.NEXT_PUBLIC_API_URL}/auth/me`, {
      credentials: "include",
    })
      .then((res) => (res.ok ? res.json() : null))
      .then((data) => {
        setUser(data);

        if (!data && !isLocalHost) {
          window.location.replace("/login");
        }
      })
      .catch(() => {
        setUser(null);

        if (!isLocalHost) {
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
