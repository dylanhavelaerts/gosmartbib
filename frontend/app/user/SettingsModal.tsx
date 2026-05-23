"use client";

import "./settingsModal.css";

export interface UserPreferences {
  anonymousLeaderboard: boolean;
}

interface SettingRowProps {
  label: string;
  description: string;
  options: { label: string; value: string }[];
  value: string;
  disabled?: boolean;
  onChange: (value: string) => void;
}

function SettingRow({ label, description, options, value, disabled, onChange }: SettingRowProps) {
  return (
    <div className="setting-row">
      <div className="setting-row-text">
        <p className="setting-row-label">{label}</p>
        <p className="setting-row-desc">{description}</p>
      </div>
      <select
        className="setting-select"
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
      >
        {options.map((opt) => (
          <option key={opt.value} value={opt.value}>
            {opt.label}
          </option>
        ))}
      </select>
    </div>
  );
}

interface SettingsModalProps {
  prefs: UserPreferences;
  saving: boolean;
  onToggle: (anonymousLeaderboard: boolean) => void;
  onClose: () => void;
}

export default function SettingsModal({ prefs, saving, onToggle, onClose }: SettingsModalProps) {
  return (
    <div className="settings-backdrop" onClick={onClose}>
      <div className="settings-modal" onClick={(e) => e.stopPropagation()}>
        <div className="settings-modal-header">
          <p className="settings-modal-title">Instellingen</p>
          <button className="settings-modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="settings-list">
          <SettingRow
            label="Zichtbaarheid op ranglijst"
            description="Bepaal of anderen jouw naam zien op de leaderboard."
            options={[
              { label: "Zichtbaar", value: "false" },
              { label: "Anoniem", value: "true" },
            ]}
            value={String(prefs.anonymousLeaderboard)}
            disabled={saving}
            onChange={(val) => onToggle(val === "true")}
          />
          {/* Add more SettingRow entries here */}
        </div>
      </div>
    </div>
  );
}
