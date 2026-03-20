import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (pathname.startsWith("/login") || 
      pathname.startsWith("/_next") || 
      pathname.startsWith("/api")) {
    return NextResponse.next();
  }

  // Stap 1. checkt voor de gebruiker session cookie
  const hasSession = request.cookies.has("JSESSIONID");

  // Stap 2.1 geen session cookie, redirect naar login pagina
  if (!hasSession) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  // Stap 2.2 session cookie gevonden, toegang tot pagina toestaan
  return NextResponse.next();
}

// Proxy toepassen op alle routes behalve de login pagina, statische bestanden en API routes
export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico).*)",    // exclude Next.js internals
    "/((?!.*\\.png$).*)",                               // exclude .png files
  ],
};