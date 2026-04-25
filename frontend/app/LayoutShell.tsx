"use client";

import MockRoleSwitcher from "./mocking/mockRoleSwitcher";
import Navbar from "./components/Navbar";

export default function LayoutShell({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="app-container">
      <Navbar />

      <div className="dev-overlay">
        <MockRoleSwitcher />
      </div>

      <main>{children}</main>

      <footer></footer>
    </div>
  );
}
