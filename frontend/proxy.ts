import { NextRequest, NextResponse } from "next/server";

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  if (
    pathname === "/login" ||
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname.startsWith("/smartschool") ||
    pathname.startsWith("/public")
  ) {
    return NextResponse.next();
  }

  const hasSession = request.cookies.has("JSESSIONID");
  const isLocal = request.nextUrl.hostname === "localhost";
  if (isLocal) {
    return NextResponse.next();
  }

  if (!hasSession) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

// Voeg de proxy toe overal behalve voor statische bestanden en de login pagina
export const config = {
  matcher: [
    "/((?!_next/static|_next/image|favicon.ico|.*\\.png$|.*\\.jpg$|.*\\.jpeg$|.*\\.svg$|.*\\.ico$).*)",
  ],
};
