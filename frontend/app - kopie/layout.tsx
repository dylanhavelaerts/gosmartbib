import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "./context/AuthContext";
import LayoutShell from "./LayoutShell";

export const metadata: Metadata = {
  title: "GO! Antwerpen Smartbib",
  description: "Bibliotheek website GO! Antwerpen",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>
        <AuthProvider>
          <LayoutShell>{children}</LayoutShell>
        </AuthProvider>
      </body>
    </html>
  );
}