"use client";

import { useEffect, useState } from "react";
import { Book } from "../interfaces/Book";
import "../catalog/bookList.css";

export default function ManageCatalogPage() {
    const [books, setBooks] = useState<Book[]>([]);
    const [selectedBook, setSelectedBook] = useState<Book | null>(null);

    useEffect(() => {
        const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

        fetch(`${apiUrl}/books/all`)
            .then((res) => {
                if (!res.ok) throw new Error("Netwerk response was niet ok");
                return res.json();
            })
            .then((data: Book[]) => setBooks(data))
            .catch((err) => console.error("Fout bij ophalen boeken:", err));
    }, []);

    return (
        <main style={{ display: 'flex', flexDirection: 'column' }}>
            <style>{`
        .manage-wrapper {
          display: flex;
          flex-direction: column-reverse;
          gap: 2rem;
          width: 100%;
          max-width: 1400px;
          flex: 1;
          margin: 0 auto; 
          padding-bottom: 0; 
        }
        .eiland-lijst {
          width: 100%;
        }
        .eiland-details {
          width: 100%;
        }

        @media (min-width: 900px) {
          .manage-wrapper {
            flex-direction: row; 
          }
          .eiland-lijst {
            width: 350px;
            flex-shrink: 0;
          }
          .eiland-details {
            flex: 1;
            min-width: 0;
          }
        }
      `}</style>

            {/* AANGEPAST: margin-bottom verhoogd naar 1.5rem voor iets meer ademruimte */}
            <h1 style={{ margin: '0 0 1.5rem 0', padding: 0, lineHeight: '1' }}>Beheer catalogus</h1>

            <div className="manage-wrapper">
                <div className="eiland-lijst" style={{
                    height: 'calc(80vh - 4rem)',
                    boxSizing: 'border-box',
                    backgroundColor: 'white',
                    borderRadius: '7px',
                    border: '1.2px solid #8a1d40',
                    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.12)',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden'
                }}>
                    <div style={{ padding: '1.5rem', borderBottom: '1px solid #ddd', backgroundColor: '#fdfdfd' }}>
                        <input
                            type="text"
                            placeholder="Zoek op titel, auteur of ISBN..."
                            style={{
                                width: '100%', padding: '0.8rem', borderRadius: '6px',
                                border: '1px solid #ccc', boxSizing: 'border-box',
                                backgroundColor: '#ece6f0',
                                outline: 'none'
                            }}
                        />
                    </div>

                    <div style={{ flex: 1, overflowY: 'auto', padding: '1rem' }}>
                        {books.length === 0 ? (
                            <p style={{ textAlign: 'center', color: '#888' }}>Geen boeken gevonden.</p>
                        ) : (
                            <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                {books.map((book) => {
                                    const isSelected = selectedBook?.id === book.id;
                                    return (
                                        <li
                                            key={book.id}
                                            onClick={() => setSelectedBook(book)}
                                            style={{
                                                padding: '0.8rem',
                                                borderRadius: '6px',
                                                cursor: 'pointer',
                                                backgroundColor: isSelected ? '#8e2446' : '#fff',
                                                color: isSelected ? 'white' : '#333',
                                                border: isSelected ? '1px solid #8e2446' : '1px solid #eee',
                                                transition: 'all 0.2s ease',
                                                display: 'flex',
                                                alignItems: 'flex-start',
                                                gap: '1rem'
                                            }}
                                        >
                                            <div style={{
                                                flexShrink: 0,
                                                width: '45px',
                                                height: '65px',
                                                backgroundColor: '#eee',
                                                borderRadius: '4px',
                                                overflow: 'hidden',
                                                display: 'flex',
                                                alignItems: 'center',
                                                justifyContent: 'center',
                                                marginTop: '0.2rem'
                                            }}>
                                                {book.thumbnail ? (
                                                    <img src={book.thumbnail} alt={book.title} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                                                ) : (
                                                    <span style={{ fontSize: '0.6rem', color: '#aaa' }}>Geen cover</span>
                                                )}
                                            </div>

                                            <div style={{ flex: 1, overflow: 'hidden' }}>
                                                <h3 style={{
                                                    margin: '0 0 0.25rem 0',
                                                    fontSize: '1rem',
                                                    whiteSpace: 'normal',
                                                    wordBreak: 'break-word'
                                                }}>
                                                    {book.title}
                                                </h3>
                                                <p style={{
                                                    margin: 0,
                                                    fontSize: '0.85rem',
                                                    opacity: isSelected ? 0.9 : 0.6,
                                                    whiteSpace: 'normal',
                                                    wordBreak: 'break-word'
                                                }}>
                                                    {book.authors ? book.authors.join(', ') : 'Onbekend'}
                                                </p>
                                                <p style={{
                                                    margin: '0.25rem 0 0 0',
                                                    fontSize: '0.75rem',
                                                    opacity: isSelected ? 0.7 : 0.5,
                                                    wordBreak: 'break-word'
                                                }}>
                                                    ISBN: {book.isbn || '-'}
                                                </p>
                                            </div>
                                        </li>
                                    )
                                })}
                            </ul>
                        )}
                    </div>
                </div>

                <div className="eiland-details" style={{
                    height: 'calc(80vh - 4rem)',
                    boxSizing: 'border-box', 
                    backgroundColor: 'white',
                    borderRadius: '7px',
                    border: '1.2px solid #8a1d40',
                    boxShadow: '0 2px 12px rgba(0, 0, 0, 0.12)',
                    display: 'flex',
                    flexDirection: 'column', 
                    overflowY: 'auto',
                    padding: 'clamp(1.5rem, 3vw, 3rem)'
                }}>
                    {!selectedBook ? (
                        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#888' }}>
                            <p style={{ fontSize: '1.2rem', textAlign: 'center' }}>Klik op een boek in de lijst om de details te bekijken.</p>
                        </div>
                    ) : (
                        <div>
                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
                                <div style={{ flex: '1 1 300px' }}>
                                    <h1 style={{ margin: '0 0 0.5rem 0', fontSize: 'clamp(1.5rem, 4vw, 2rem)', color: '#c0392b', padding: 0 }}>{selectedBook.title}</h1>
                                    <p style={{ margin: 0, fontSize: '1.1rem', color: '#666' }}>Door {selectedBook.authors?.join(', ') || 'Onbekend'}</p>
                                </div>

                                <div style={{ display: 'flex', gap: '1rem' }}>
                                    <button className="confirmButton" type="button" style={{ padding: '0.6rem 1.2rem', backgroundColor: '#ece6f0', color: '#333', border: 'none', cursor: 'pointer', fontWeight: 'bold' }}>
                                        Bewerken
                                    </button>
                                    <button className="confirmButton" type="button" style={{ padding: '0.6rem 1.2rem', backgroundColor: '#c0392b', color: 'white', border: 'none', cursor: 'pointer', fontWeight: 'bold' }}>
                                        Verwijderen
                                    </button>
                                </div>
                            </div>

                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '2rem' }}>
                                <div style={{ flexShrink: 0, margin: '0 auto' }}>
                                    {selectedBook.thumbnail ? (
                                        <img
                                            src={selectedBook.thumbnail}
                                            alt={`Cover van ${selectedBook.title}`}
                                            style={{ width: '200px', maxWidth: '100%', height: 'auto', borderRadius: '6px', boxShadow: '0 4px 8px rgba(0,0,0,0.1)' }}
                                        />
                                    ) : (
                                        <div style={{ width: '200px', height: '300px', backgroundColor: '#eee', display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '6px' }}>
                                            <span style={{ color: '#aaa' }}>Geen cover</span>
                                        </div>
                                    )}
                                </div>

                                <div style={{ flex: '1 1 300px', minWidth: '0' }}>
                                    <div style={{ overflowX: 'auto' }}>
                                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', minWidth: '250px' }}>
                                            <tbody>
                                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                                    <th style={{ padding: '1rem 0', color: '#8e2446', width: '130px' }}>ISBN</th>
                                                    <td style={{ padding: '1rem 0', fontWeight: 'bold', color: '#333' }}>{selectedBook.isbn || '-'}</td>
                                                </tr>
                                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                                    <th style={{ padding: '1rem 0', color: '#8e2446' }}>Uitgeverij</th>
                                                    <td style={{ padding: '1rem 0', color: '#333' }}>{selectedBook.publisher || '-'}</td>
                                                </tr>
                                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                                    <th style={{ padding: '1rem 0', color: '#8e2446' }}>Jaar</th>
                                                    <td style={{ padding: '1rem 0', color: '#333' }}>{selectedBook.publishedYear || '-'}</td>
                                                </tr>
                                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                                    <th style={{ padding: '1rem 0', color: '#8e2446' }}>Pagina's</th>
                                                    <td style={{ padding: '1rem 0', color: '#333' }}>{selectedBook.pageCount || '-'}</td>
                                                </tr>
                                                <tr style={{ borderBottom: '1px solid #ddd' }}>
                                                    <th style={{ padding: '1rem 0', color: '#8e2446' }}>Categorie</th>
                                                    <td style={{ padding: '1rem 0', color: '#333' }}>{selectedBook.categories?.join(', ') || '-'}</td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>

                                    <div style={{ marginTop: '2rem' }}>
                                        <h3 style={{ fontSize: '1.2rem', color: '#8e2446', marginBottom: '1rem' }}>Samenvatting</h3>
                                        <p style={{ lineHeight: '1.6', color: '#444', whiteSpace: 'pre-wrap' }}>
                                            {selectedBook.description || 'Geen samenvatting beschikbaar voor dit boek.'}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </main>
    );
}