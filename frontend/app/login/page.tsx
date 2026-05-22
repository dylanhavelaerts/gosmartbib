"use client";

import { useEffect, useState } from "react";
import "./login.css";

export default function LoginPage() {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;
  const [hasError] = useState(() =>
    typeof window !== "undefined"
      ? new URLSearchParams(window.location.search).get("error") === "true"
      : false,
  );
  const [schoolNotApproved] = useState(() =>
    typeof window !== "undefined"
      ? new URLSearchParams(window.location.search).get("error") ===
        "school_not_approved"
      : false,
  );

  useEffect(() => {
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = "";
    };
  }, []);

  const handleLogin = () => {
    if (!apiBaseUrl) {
      alert("NEXT_PUBLIC_API_URL is not set.");
      return;
    }
    window.location.href = `${apiBaseUrl}/auth/login`;
  };

  const [adminOpen, setAdminOpen] = useState(false);
  const [adminUsername, setAdminUsername] = useState("");
  const [adminPassword, setAdminPassword] = useState("");
  const [adminError, setAdminError] = useState(false);
  const [adminLoading, setAdminLoading] = useState(false);

  const handleAdminLogin = async () => {
    if (!apiBaseUrl) return;
    setAdminLoading(true);
    setAdminError(false);
    try {
      const res = await fetch(`${apiBaseUrl}/admin/login`, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: adminUsername,
          password: adminPassword,
        }),
      });
      if (res.ok) {
        window.location.href = "/admin/schools";
      } else if (res.status === 429) {
        setAdminError(true);
      } else {
        setAdminError(true);
      }
    } catch {
      setAdminError(true);
    } finally {
      setAdminLoading(false);
    }
  };

  return (
    <>
      <div className="loginBackground" />
      <main className="loginPage">
        <section className="loginCard">
          <div className="loginCardHeader">
            <img
              src="https://helpdesk.go-antwerpen.be/logo.php"
              className="loginGoLogo "
              alt="GO!"
            />
          </div>
          <p className="loginSubtitle">
            Meld je aan met je Smartschool-account om verder te gaan
          </p>
          <button
            type="button"
            className="smartschoolLoginButton"
            onClick={handleLogin}
            aria-label="Aanmelden met Smartschool"
          >
            <picture>
              <source
                media="(max-width: 420px)"
                srcSet="/smartschool/Smartschool%20aanmelden%20knop/btn_aanmelden_met_smartschool_234x32.png"
              />
              <img
                src="/smartschool/Smartschool%20aanmelden%20knop/btn_aanmelden_met_smartschool_290x40.png"
                alt="Aanmelden met Smartschool"
                width={290}
                height={40}
              />
            </picture>
          </button>

          {schoolNotApproved && (
            <p className="loginError" role="alert">
              Jouw school is nog niet toegevoegd aan het platform. Contacteer de
              schooladministratie.
            </p>
          )}
          {hasError && !schoolNotApproved && (
            <p className="loginError" role="alert">
              Er is een fout opgetreden tijdens het inloggen. Probeer het
              opnieuw.
            </p>
          )}
          <div className="adminToggle" onClick={() => setAdminOpen((o) => !o)}>
            <span>Beheerderslogin</span>
            <span
              className={`adminChevron ${adminOpen ? "adminChevronOpen" : ""}`}
            >
              ▾
            </span>
          </div>

          {adminOpen && (
            <div className="adminSection">
              <input
                className="adminInput"
                type="text"
                placeholder="Gebruikersnaam"
                value={adminUsername}
                onChange={(e) => setAdminUsername(e.target.value)}
                autoComplete="username"
              />
              <input
                className="adminInput"
                type="password"
                placeholder="Wachtwoord"
                value={adminPassword}
                onChange={(e) => setAdminPassword(e.target.value)}
                autoComplete="current-password"
              />
              {adminError && (
                <p className="loginError" role="alert">
                  Ongeldige gebruikersnaam of wachtwoord.
                </p>
              )}
              <button
                className="adminSubmitButton"
                onClick={handleAdminLogin}
                disabled={adminLoading}
                type="button"
              >
                {adminLoading ? "Bezig..." : "Aanmelden"}
              </button>
            </div>
          )}
        </section>
      </main>
    </>
  );
}
