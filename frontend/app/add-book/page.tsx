"use client";

import { useState } from 'react';

export default function AddBookPage() {
  const [isbn, setIsbn] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const handleAddBook = async (e: React.FormEvent) => {
    e.preventDefault(); // Voorkomt dat de pagina herlaadt
    if (!isbn) return;

    setLoading(true);
    setMessage('');

    try {
      // Omdat we via Traefik werken, begint de API-route met /api
      const response = await fetch(`/api/catalog/add/${isbn}`, {
        method: 'POST',
      });

      if (response.ok) {
        const data = await response.json();
        setMessage(`✅ Boek succesvol toegevoegd: "${data.title}"`);
        setIsbn(''); // Maak het invoerveld weer leeg voor het volgende boek
      } else if (response.status === 404) {
        setMessage('❌ Geen boek gevonden met dit ISBN-nummer bij Google Books.');
      } else {
        setMessage('❌ Er is een onverwachte serverfout opgetreden.');
      }
    } catch (error) {
      setMessage('❌ Kan de server niet bereiken. Controleer of de backend draait.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ padding: '2rem', maxWidth: '600px', margin: '0 auto', fontFamily: 'sans-serif' }}>
      <h1 style={{ fontSize: '2rem', marginBottom: '1rem' }}>Nieuw Boek Toevoegen</h1>
      <p style={{ marginBottom: '2rem', color: '#555' }}>
        Scan of typ het ISBN-nummer van het boek in. Wij halen de details op bij Google Books en voegen het toe aan de catalogus.
      </p>

      <form onSubmit={handleAddBook} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
        <div>
          <label htmlFor="isbn" style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 'bold' }}>
            ISBN Nummer:
          </label>
          <input
            id="isbn"
            type="text"
            value={isbn}
            onChange={(e) => setIsbn(e.target.value)}
            placeholder="Bijv. 9789045122588"
            style={{ width: '100%', padding: '0.75rem', fontSize: '1rem', borderRadius: '4px', border: '1px solid #ccc' }}
          />
        </div>
        <button 
          type="submit" 
          disabled={loading}
          style={{ 
            padding: '0.75rem 1.5rem', 
            fontSize: '1rem', 
            cursor: loading ? 'not-allowed' : 'pointer', 
            backgroundColor: loading ? '#ccc' : '#0070f3', 
            color: 'white', 
            border: 'none', 
            borderRadius: '4px',
            fontWeight: 'bold'
          }}
        >
          {loading ? 'Bezig met zoeken...' : 'Boek Toevoegen'}
        </button>
      </form>
      
      {/* Meldingen weergeven (succes of error) */}
      {message && (
        <div style={{ 
          marginTop: '2rem', 
          padding: '1rem', 
          backgroundColor: message.includes('✅') ? '#d4edda' : '#f8d7da', 
          color: message.includes('✅') ? '#155724' : '#721c24',
          border: message.includes('✅') ? '1px solid #c3e6cb' : '1px solid #f5c6cb',
          borderRadius: '4px' 
        }}>
          {message}
        </div>
      )}
    </div>
  );
}