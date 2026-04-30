"use client";

import { useEffect, useState } from "react";
import "./Notification.css";

interface Props {
  bookId: number;
  className?: string;
}

export default function NotificationBell({ bookId, className = "" }: Props) {
  const [enabled, setEnabled] = useState(false);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL;

  useEffect(() => {
    fetch(`${apiUrl}/books/${bookId}/notification`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((on: boolean) => setEnabled(on))
      .catch(() => setEnabled(false));
  }, [bookId]);

  const toggle = async () => {
    const method = enabled ? "DELETE" : "POST";
    try {
      await fetch(`${apiUrl}/books/${bookId}/notification`, {
        method,
        credentials: "include",
      });
      setEnabled((prev) => !prev);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <button
      className={`notifBtn ${enabled ? "notifBtn--active" : ""} ${className}`}
      onClick={toggle}
      title={
        enabled
          ? "Notificatie uitschakelen"
          : "Notificeer mij als dit boek beschikbaar is"
      }
    >
      <img
        src={
          enabled
            ? "/notification/bell-notification-social-media_full_black.png"
            : "/notification/bell-notification-social-media.png"
        }
        alt={enabled ? "Notificatie aan" : "Notificatie uit"}
        width={24}
        height={24}
      />
    </button>
  );
}
