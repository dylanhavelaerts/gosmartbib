"use client"

import { UserRole } from "@/app/interfaces/user";
import type { MeResponse, AdminUser } from "@/app/interfaces/user";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

const ROLE_OPTIONS: UserRole[] = [
    "STUDENT",
    "LEERKRACHT",
    "BIBLIOTHEEKBEHEERDER"
]

export default function AdminUserPage() {
    
}