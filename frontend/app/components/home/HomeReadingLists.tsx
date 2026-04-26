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
      <div className="tabs-nav">
        <div className="tabBtn titleTab">Jouw leeslijsten</div>
      </div>

      <div className="homeReadingListsContent">
        <div className="homeReadingListsActions">
          <button
            className="semitransparentButton homeRlBtn"
            onClick={onOpenPersonal}
          >
            Mijn leeslijsten
          </button>
          <button
            className="semitransparentButton homeRlBtn homeRlBtnGhost"
            onClick={onOpenAll}
          >
            Alle leeslijsten
          </button>
        </div>

        {loading && (
          <p className="homeReadingListsState">Leeslijsten laden...</p>
        )}

        {!loading && error && (
          <div className="homeReadingListsState">
            <p>{error}</p>
            <button
              className="semitransparentButton homeRlBtn"
              onClick={onRetry}
            >
              Opnieuw proberen
            </button>
          </div>
        )}

        {!loading && !error && lists.length === 0 && (
          <div className="homeReadingListsState">
            <p>Je hebt nog geen persoonlijke leeslijst.</p>
            <button
              className="semitransparentButton homeRlBtn"
              onClick={onOpenPersonal}
            >
              Maak je eerste lijst
            </button>
          </div>
        )}

        {!loading && !error && lists.length > 0 && (
          <div className="homeReadingListsGrid">
            {lists.map((list) => {
              const count = list.bookIds?.length ?? list.bookCount ?? 0;
              return (
                <article key={list.id} className="homeReadingListCard">
                  <div className="homeReadingListCardInfo">
                    <h3>{list.title}</h3>
                    <span>
                      {count} {count === 1 ? "boek" : "boeken"}
                    </span>
                  </div>
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
      </div>
    </section>
  );
}
