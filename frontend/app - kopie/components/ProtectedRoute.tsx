"use client";

import { useAuth } from "@/app/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

type UserRole =
  | "STUDENT"
  | "TEACHER"
  | "BIBLIOTHEEKBEHEERDER"
  | "ADMIN"
  | "OTHER";

interface Props {
  allowedRoles: UserRole[];
  children: React.ReactNode;
}

export default function ProtectedRoute({ allowedRoles, children }: Props) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!loading && (!user || !allowedRoles.includes(user.role))) {
      router.replace("/catalog");
    }
  }, [user, loading]);

  if (loading) return null;
  if (!user || !allowedRoles.includes(user.role)) return null;

  return <>{children}</>;
}
