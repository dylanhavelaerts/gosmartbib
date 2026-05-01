"use client";

import { useEffect, useState } from "react";
import "./Notification.css";

interface Props {
  apiPath: string;
  className?: string;
  label?: string;
}

export default function NotificationBell({
  apiPath,
  className = "",
  label,
}: Props) {
  const [enabled, setEnabled] = useState(false);
  const apiUrl = process.env.NEXT_PUBLIC_API_URL;

  useEffect(() => {
    fetch(`${apiUrl}${apiPath}`, {
      credentials: "include",
    })
      .then((r) => r.json())
      .then((on: boolean) => setEnabled(on))
      .catch(() => setEnabled(false));
  }, [apiPath]);

  const toggle = async () => {
    const method = enabled ? "DELETE" : "POST";
    try {
      await fetch(`${apiUrl}${apiPath}`, {
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
      {label ? (
        <span>{enabled ? "Notificatie uitschakelen" : label}</span>
      ) : (
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
      )}
    </button>
  );
}
