"use client";

import { useAuth } from "@/app/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

type UserRole =
  | "STUDENT"
  | "TEACHER"
  | "LIBRARIAN"
  | "ADMIN"
  | "OTHER";

interface Props {
  allowedRoles: UserRole | UserRole[];
  children: React.ReactNode;
}

export default function ProtectedRoute({ allowedRoles, children }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();
  const roles = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];

  useEffect(() => {
    if (!loading && (!user || !roles.includes(user.role))) {
      router.replace("/catalog");
    }
  }, [user, loading]);

  if (loading) return null;
  if (!user || !roles.includes(user.role)) return null;

  return <>{children}</>;
}
