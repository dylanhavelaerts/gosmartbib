"use client";
import Image from "next/image";
import { useEffect, useState } from "react";

export default function Home() {
  return (
    <>
      <main>
        <div id="searchBox">
          <div className="searchbar" >         
            <input type="text" placeholder="Titel, auteur, genre, onderwerp"/>
            <button id="searchButton">🔎︎</button>
          </div>
          <button className="semitransparentButton">Bekijk Catalogus →</button>
        </div>
      </main>
      <footer>
      </footer></>
  )
}
