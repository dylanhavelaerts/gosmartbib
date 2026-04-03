"use client";

import { useState } from "react";

interface StarRatingProps {
  value: number;
  onChange?: (v: number) => void;
}

export default function StarRating({ value, onChange }: StarRatingProps) {
  const [hovered, setHovered] = useState(0);
  const interactive = !!onChange;

  return (
    <div className="stars">
      {[1, 2, 3, 4, 5].map((star) => (
        <span
          key={star}
          className={`star ${star <= (hovered || value) ? "filled" : ""} ${interactive ? "interactive" : ""}`}
          onMouseEnter={() => interactive && setHovered(star)}
          onMouseLeave={() => interactive && setHovered(0)}
          onClick={() => onChange?.(star)}
        >
          ★
        </span>
      ))}
    </div>
  );
}
