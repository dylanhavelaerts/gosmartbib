"use client";

import { useEffect } from "react";
import { useSearchParams } from "next/navigation";
import "./login.css";

export default function LoginPage() {
  const apiBaseUrl = process.env.NEXT_PUBLIC_API_URL;
  const searchParams = useSearchParams();
  const hasError = searchParams.get("error") === "true";

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
            Meld je aan met je Smartschool-account om verder te gaan.
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

          {hasError && (
            <p className="loginError" role="alert">
              Er is een fout opgetreden tijdens het inloggen. Probeer het
              opnieuw.
            </p>
          )}
        </section>
      </main>
    </>
  );
}
