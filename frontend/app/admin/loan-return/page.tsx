"use client";

import "./general.css";

export default function GeneralLoanReturn() {
  return (
    <div className="loan-return-page">
      <div className="loan-return-header">
        <h2 className="loan-return-title">Uitlenen of terugbrengen</h2>
        <p className="loan-return-subtitle">Kies een actie om verder te gaan</p>
      </div>

      <div className="loan-return-grid">
        <a href="/admin/loan-return/loan" className="action-card loan-card">
          <div className="action-icon">
            <img
              src="/book-icon.png"
              alt="Uitlenen"
              className="action-icon-img"
            />
          </div>
          <div className="action-body">
            <p className="action-title">Boek uitlenen</p>
            <p className="action-description">
              Registreer een nieuw boek dat wordt meegegeven aan een leerling of
              leerkracht
            </p>
          </div>
        </a>

        <a href="/admin/loan-return/return" className="action-card return-card">
          <div className="action-icon">
            <img
              src="/admin/return.png"
              alt="Terugbrengen"
              className="action-icon-img"
            />
          </div>
          <div className="action-body">
            <p className="action-title">Boek terugbrengen</p>
            <p className="action-description">
              Verwerk de terugbreng van een geleend boek en werk de voorraad bij
            </p>
          </div>
        </a>
        <a href="/admin/loans-overview" className="action-card">
          <div className="action-icon">
            <img
              src="/history.png"
              alt="Leenbeheer"
              className="action-icon-img"
            />
          </div>
          <div className="action-body">
            <p className="action-title">Leenbeheer</p>
            <p className="action-description">
              Bekijk alle actieve leningen en de ontleengeschiedenis binnen jouw
              school
            </p>
          </div>
        </a>
      </div>
    </div>
  );
}
