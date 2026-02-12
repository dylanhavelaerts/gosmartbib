"use client";
import Image from "next/image";
import { useEffect, useState } from "react";

export default function Home() {
  const [message, setMessage] = useState("loading...");
  const [error, setError] = useState(null);

  useEffect(() => {
    fetch("/api/hello")
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then((d) => setMessage(d.message))
      .catch((e) => setError(e.message));
  }, []);

  if (error) {
    return <p>error : {error}</p>;
  }
  return <p>Message from DB via backend: {message}</p>;
}
