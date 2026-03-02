"use client";

import { useState } from "react";

type TabId = "spotlight" | "new";

export default function Home() {
  const [selected, setSelected] = useState<TabId>("spotlight");
  const cls = (id: TabId) => `tabBtn ${selected === id ? "selectedCategory" : ""}`;
  return (
    <>
      <main>
        <div id="searchBox">
          <div className="searchbar">
            <input type="text" placeholder="Titel, auteur, genre, onderwerp" />
            <button id="searchButton">🔎︎</button>
          </div>
          <button className="semitransparentButton">Bekijk Catalogus →</button>
        </div>
        <div id="dashboard">
          <nav>
            <button className={cls("spotlight")} onClick={() => setSelected("spotlight")}>in de kijker</button>
            <button className={cls("new")} onClick={() => setSelected("new")}>Nieuw in bibliotheek</button>
          </nav>
        </div>
      </main></>
  )
}
