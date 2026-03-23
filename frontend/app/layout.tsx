import type { Metadata } from "next";
import "./globals.css";
import Link from "next/link";
import MockRoleSwitcher from "./mocking/mockRoleSwitcher";
import { AuthProvider } from "./context/AuthContext";

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
          <header>
            <Link href={"/"}>
              <img
                src="https://helpdesk.go-antwerpen.be/logo.php"
                className="goLogo"
              />
            </Link>
          </header>
          <nav id="headerNav">
            <Link href="/">
              <button>Startpagina</button>
            </Link>
            <Link href="/catalog">
              <button>Catalogus</button>
            </Link>
            <Link href="/spotlight">
              <button>In de kijker</button>
            </Link>
            <Link href="/">
              <button>Mijn uitleningen</button>
            </Link>
            <Link href="/">
              <button>Mijn leeslijst & favorieten</button>
            </Link>
            <MockRoleSwitcher />
          </nav>
          <div id="backgroundImage"></div>
          {children}
          <footer>
            <div id="socialMediaBlock">
              <h4>Sociale media</h4>
              <a
                href="https://www.instagram.com/go_scholengroep_antwerpen/"
                id="instagram"
                className="socialMediaLogo"
              ></a>
              <a
                href="https://www.facebook.com/GOAntwerpen/"
                id="facebook"
                className="socialMediaLogo"
              ></a>
              <a
                href="https://x.com/GOinAntwerpen"
                id="twitter"
                className="socialMediaLogo"
              ></a>
              <a
                href="https://www.linkedin.com/company/go-scholengroep-antwerpen/"
                id="linkedIn"
                className="socialMediaLogo"
              ></a>
              <a
                href="https://www.youtube.com/channel/UCDdFd-gH1DbonyHgBqyBUWQ"
                id="youtube"
                className="socialMediaLogo"
              ></a>
            </div>
            <img
              src="https://helpdesk.go-antwerpen.be/logo.php"
              className="goLogoFooter"
            />
            <div id="contactBlock">
              <h4>Contact</h4>
              <p>GO! Scholengroep Antwerpen</p>
              <p>Thonetlaan 106A</p>
              <p>2050 Antwerpen</p>
              <p>secretariaat@go-antwerpen.be</p>
              <p>03 360 82 90</p>
            </div>
            <div id="linksBlock">
              <h4>Nuttige links</h4>
              <ul>
                <li>
                  <a href="https://g-o.be/">GO! onderwijs</a>
                </li>
                <li>
                  <a href="https://meldjeaan.antwerpen.be/">
                    Meld je aan Antwerpen
                  </a>
                </li>
                <li>
                  <a href="https://go-antwerpen.smartschool.be/login">
                    Smartschool
                  </a>
                </li>
                <li>
                  <a href="https://onderwijs.vlaanderen.be/">
                    Onderwijs Vlaanderen
                  </a>
                </li>
                <li>
                  <a href="https://www.onderwijskiezer.be/v2/index.php">
                    Onderwijskiezer
                  </a>
                </li>
                <li>
                  <a href="https://www.taalkrachtgoantwerpen.be/">
                    Taalkracht GO! Antwerpen
                  </a>
                </li>
              </ul>
            </div>
          </footer>
        </AuthProvider>
      </body>
    </html>
  );
}