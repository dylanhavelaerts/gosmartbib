"use client";

import { useEffect, useRef } from "react";
import type { BookCopyLabel } from "../../interfaces/BookCopyLabel";
import JsBarcode from "jsbarcode";

function BarcodeLabel({ value }: { value: string }) {
  const svgRef = useRef<SVGSVGElement>(null);
  useEffect(() => {
    if (svgRef.current) {
      JsBarcode(svgRef.current, value, {
        format: "EAN13",
        width: 2,
        height: 50,
        displayValue: true,
        fontSize: 12,
        margin: 4,
      });
    }
  }, [value]);
  return <svg ref={svgRef} />;
}

const PRINT_CSS = `
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: sans-serif; background: white; }
  .label-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 4mm;
    padding: 10mm;
  }
  .label-card {
    border: 1px solid #ccc;
    border-radius: 6px;
    display: flex;
    flex-direction: column;
    break-inside: avoid;
    page-break-inside: avoid;
  }
  .label-card-top {
    padding: 2mm 3mm;
    border-bottom: 1px solid #e8dde2;
    background: #fdf6f9;
  }
  .label-title {
    font-size: 7pt;
    font-weight: 700;
    color: #3d2030;
    line-height: 1.3;
    display: block;
  }
  .label-campus {
    font-size: 6pt;
    color: #9a7a8a;
    display: block;
  }
  .label-card-middle {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 2mm 1mm;
  }
  .label-copy-number {
    font-size: 18pt;
    font-weight: 800;
    color: #8e2446;
    line-height: 1;
  }
  .label-copy-text {
    font-size: 5pt;
    color: #9a7a8a;
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }
  .label-card-bottom {
    padding: 1mm 2mm 2mm;
    border-top: 1px solid #e8dde2;
    background: #fdf6f9;
    display: flex;
    justify-content: center;
  }
  .label-card-bottom svg { width: 100%; height: auto; max-width: 50mm; display: block; }
`;

interface Props {
  labels: BookCopyLabel[];
  onClose: () => void;
}

export default function LabelPrintModal({ labels, onClose }: Props) {
  const gridRef = useRef<HTMLDivElement>(null);

  function handlePrint() {
    const grid = gridRef.current;
    if (!grid) return;
    const win = window.open("", "_blank", "width=900,height=700");
    if (!win) return;
    win.document.write(
      `<!DOCTYPE html><html><head><meta charset="utf-8"/><title>Labels afdrukken</title>` +
        `<style>${PRINT_CSS}</style></head><body>` +
        grid.outerHTML +
        `<script>window.onload=function(){window.print();window.addEventListener('afterprint',function(){window.close();});};<\/script>` +
        `</body></html>`
    );
    win.document.close();
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        className="modal-box label-print-modal"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header label-modal-header">
          <div>
            <h2>Labels afdrukken</h2>
            <p className="label-modal-count">
              {labels.length === 0
                ? "Barcodes worden toegewezen bij het afdrukken"
                : `${labels.length} exempla${labels.length !== 1 ? "ren" : "ar"}`}
            </p>
          </div>
          <div className="label-modal-header-actions">
            {labels.length > 0 && (
              <button
                type="button"
                className="modal-btn-save"
                onClick={handlePrint}
              >
                Afdrukken
              </button>
            )}
            <button type="button" className="modal-btn-cancel" onClick={onClose}>
              Sluiten
            </button>
          </div>
        </div>
        <div className="modal-body">
          {labels.length === 0 ? (
            <div className="label-empty-state">
              <p>Nog geen barcodes toegewezen.</p>
              <p>
                Voeg exemplaren toe aan de inventaris en druk daarna opnieuw af
                — barcodes worden automatisch aangemaakt.
              </p>
            </div>
          ) : (
            <div className="label-grid" ref={gridRef}>
              {labels.map((label) => (
                <div key={label.copyId} className="label-card">
                  <div className="label-card-top">
                    <span className="label-title">{label.bookTitle}</span>
                    {label.campus && (
                      <span className="label-campus">{label.campus}</span>
                    )}
                  </div>
                  <div className="label-card-middle">
                    <span className="label-copy-number">{label.copyNumber}</span>
                    <span className="label-copy-text">exemplaar</span>
                  </div>
                  <div className="label-card-bottom">
                    <BarcodeLabel value={label.barcode} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
