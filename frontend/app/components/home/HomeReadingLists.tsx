"use client";

import { ReadingListOverview } from "../../interfaces/ReadingList";
import "./homeReadingLists.css";

interface HomeReadingListsProps {
  lists: ReadingListOverview[];
  loading: boolean;
  error: string | null;
  onOpenList: (id: number) => void;
  onOpenPersonal: () => void;
  onOpenAll: () => void;
  onRetry: () => void;
}

export default function HomeReadingLists({
  lists,
  loading,
  error,
  onOpenList,
  onOpenPersonal,
  onOpenAll,
  onRetry,
}: HomeReadingListsProps) {
  return (
    <section className="homeReadingLists">
      <div className="homeReadingListsHeader">
        <div>
          <h2>Jouw leeslijsten</h2>
          <p>Snel toegang tot je persoonlijke lijsten.</p>
        </div>

        <div className="homeReadingListsActions">
          <button className="homeRlBtn" onClick={onOpenPersonal}>
            Mijn leeslijsten
          </button>
          <button className="homeRlBtn homeRlBtnGhost" onClick={onOpenAll}>
            Alle leeslijsten
          </button>
        </div>
      </div>

      {loading && <p className="homeReadingListsState">Leeslijsten laden...</p>}

      {!loading && error && (
        <div className="homeReadingListsState">
          <p>{error}</p>
          <button className="homeRlBtn" onClick={onRetry}>
            Opnieuw proberen
          </button>
        </div>
      )}

      {!loading && !error && lists.length === 0 && (
        <div className="homeReadingListsState">
          <p>Je hebt nog geen persoonlijke leeslijst.</p>
          <button className="homeRlBtn" onClick={onOpenPersonal}>
            Maak je eerste lijst
          </button>
        </div>
      )}

      {!loading && !error && lists.length > 0 && (
        <div className="homeReadingListsGrid">
          {lists.slice(0, 3).map((list) => {
            const count = list.bookIds?.length ?? list.bookCount ?? 0;
            return (
              <article key={list.id} className="homeReadingListCard">
                <h3>{list.title}</h3>
                {list.taskDescription && <p>{list.taskDescription}</p>}
                <span>
                  {count} {count === 1 ? "boek" : "boeken"}
                </span>
                <button
                  className="homeListOpenBtn"
                  onClick={() => onOpenList(list.id)}
                >
                  Open lijst
                </button>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
