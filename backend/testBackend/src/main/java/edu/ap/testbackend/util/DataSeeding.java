package edu.ap.testbackend.util;

import edu.ap.testbackend.entities.BookEntity;
import edu.ap.testbackend.repositories.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeding implements CommandLineRunner {
        private final BookRepository bookRepository;
        private static final Logger logger = LoggerFactory.getLogger(DataSeeding.class);

        public DataSeeding(BookRepository bookRepository) {
                this.bookRepository = bookRepository;
        }

        private BookEntity book(
                        String title,
                        List<String> authors,
                        String publisher,
                        String description,
                        int pageCount,
                        List<String> categories,
                        String thumbnail,
                        String language,
                        double rating,
                        String isbn,
                        Integer publishedYear,
                        boolean spotlight,
                        int totalCopies,      // <-- Parameter 1
                        int availableCopies) { // <-- Parameter 2
                BookEntity b = new BookEntity(
                                title,
                                authors,
                                publisher,
                                description,
                                pageCount,
                                categories,
                                thumbnail,
                                language,
                                rating,
                                isbn,
                                publishedYear);
                b.setSpotlight(spotlight);
                
                // Hier stellen we ze beide expliciet in
                b.setTotalCopies(totalCopies);
                b.setAvailableCopies(availableCopies);
                
                return b;
        }

        @Override
        public void run(String... args) {
                if (bookRepository.count() == 0) {

                        List<BookEntity> books = List.of(

                                // ===== FICTION — FANTASY =====
                                book("Harry Potter and the Philosopher's Stone", List.of("J.K. Rowling"), "Bloomsbury",
                                        "The boy who lived discovers he is a wizard and begins his magical education at Hogwarts.",
                                        332, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747532743-L.jpg", "en", 4.7, "9780747532743", 1997, true, 8, 8),

                                book("Harry Potter and the Chamber of Secrets", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry returns to Hogwarts to find a mysterious force petrifying students.",
                                        360, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747538486-L.jpg", "en", 4.6, "9780747538486", 1998, false, 5, 5),

                                book("Harry Potter and the Prisoner of Azkaban", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry learns about Sirius Black, a dangerous escaped prisoner with ties to his past.",
                                        468, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747542155-L.jpg", "en", 4.7, "9780747542155", 1999, false, 6, 6),

                                book("Harry Potter and the Goblet of Fire", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry is unexpectedly entered into the dangerous Triwizard Tournament.",
                                        636, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747546245-L.jpg", "en", 4.7, "9780747546245", 2000, false, 4, 4),

                                book("Harry Potter and the Order of the Phoenix", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry forms Dumbledore's Army as the wizarding world refuses to believe Voldemort has returned.",
                                        870, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747551003-L.jpg", "en", 4.6, "9780747551003", 2003, false, 7, 7),

                                book("Harry Potter and the Half-Blood Prince", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry discovers an old potions textbook with mysterious annotations and learns about Voldemort's past.",
                                        652, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747581086-L.jpg", "en", 4.6, "9780747581086", 2005, false, 5, 5),

                                book("Harry Potter and the Deathly Hallows", List.of("J.K. Rowling"), "Bloomsbury",
                                        "Harry, Ron, and Hermione set out to destroy Voldemort's Horcruxes and end the war.",
                                        759, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780747591054-L.jpg", "en", 4.7, "9780747591054", 2007, true, 9, 9),

                                book("The Hobbit", List.of("J.R.R. Tolkien"), "George Allen & Unwin",
                                        "Bilbo Baggins embarks on an unexpected adventure to reclaim dwarven treasure from a dragon.",
                                        310, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780547928227-L.jpg", "en", 4.7, "9780547928227", 1937, true, 10, 10),

                                book("The Fellowship of the Ring", List.of("J.R.R. Tolkien"), "George Allen & Unwin",
                                        "Frodo Baggins begins his epic quest to destroy the One Ring.",
                                        423, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780547928210-L.jpg", "en", 4.7, "9780547928210", 1954, false, 7, 7),

                                book("The Two Towers", List.of("J.R.R. Tolkien"), "George Allen & Unwin",
                                        "The fellowship is broken as the war of the Ring intensifies across Middle-earth.",
                                        352, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780547928203-L.jpg", "en", 4.6, "9780547928203", 1954, false, 6, 6),

                                book("The Return of the King", List.of("J.R.R. Tolkien"), "George Allen & Unwin",
                                        "The final battle for Middle-earth as Frodo reaches Mount Doom.",
                                        416, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780547928197-L.jpg", "en", 4.7, "9780547928197", 1955, false, 8, 8),

                                book("The Chronicles of Narnia: The Lion, the Witch and the Wardrobe", List.of("C.S. Lewis"), "Geoffrey Bles",
                                        "Four children discover a magical land through a wardrobe and help Aslan defeat the White Witch.",
                                        206, List.of("Fantasy", "Children's Literature"), "https://covers.openlibrary.org/b/isbn/9780064404990-L.jpg", "en", 4.6, "9780064404990", 1950, true, 4, 4),

                                book("A Game of Thrones", List.of("George R.R. Martin"), "Bantam Books",
                                        "Noble families vie for control of the Iron Throne in a brutal medieval fantasy world.",
                                        694, List.of("Fantasy", "Epic"), "https://covers.openlibrary.org/b/isbn/9780553573404-L.jpg", "en", 4.5, "9780553573404", 1996, false, 7, 7),

                                book("The Name of the Wind", List.of("Patrick Rothfuss"), "DAW Books",
                                        "Kvothe recounts his transformation from a young outcast to the most notorious wizard of his age.",
                                        662, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780756404741-L.jpg", "en", 4.6, "9780756404741", 2007, false, 3, 3),

                                book("Percy Jackson and the Lightning Thief", List.of("Rick Riordan"), "Disney Hyperion",
                                        "A twelve-year-old discovers he is the son of Poseidon and must find Zeus's stolen lightning bolt.",
                                        377, List.of("Fantasy", "Young Adult", "Mythology"), "https://covers.openlibrary.org/b/isbn/9780786838653-L.jpg", "en", 4.5, "9780786838653", 2005, true, 6, 6),

                                // ===== FICTION — SCIENCE FICTION =====
                                book("Dune", List.of("Frank Herbert"), "Chilton Books",
                                        "A young noble must navigate the dangerous politics and ecology of a desert planet.",
                                        688, List.of("Science Fiction", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780441172719-L.jpg", "en", 4.6, "9780441172719", 1965, true, 8, 8),

                                book("1984", List.of("George Orwell"), "Secker & Warburg",
                                        "A dystopian novel about totalitarian surveillance and the suppression of truth.",
                                        328, List.of("Science Fiction", "Dystopian"), "https://covers.openlibrary.org/b/isbn/9780451524935-L.jpg", "en", 4.7, "9780451524935", 1949, true, 10, 10),

                                book("Brave New World", List.of("Aldous Huxley"), "Chatto & Windus",
                                        "A futuristic society where humans are genetically modified and socially conditioned for stability.",
                                        288, List.of("Science Fiction", "Dystopian"), "https://covers.openlibrary.org/b/isbn/9780060850524-L.jpg", "en", 4.4, "9780060850524", 1932, false, 5, 5),

                                book("Fahrenheit 451", List.of("Ray Bradbury"), "Ballantine Books",
                                        "In a future where books are banned, a fireman begins to question his role in burning them.",
                                        194, List.of("Science Fiction", "Dystopian"), "https://covers.openlibrary.org/b/isbn/9781451673319-L.jpg", "en", 4.4, "9781451673319", 1953, true, 7, 7),

                                book("The Hitchhiker's Guide to the Galaxy", List.of("Douglas Adams"), "Pan Books",
                                        "Arthur Dent is swept off Earth moments before its destruction for a galactic highway.",
                                        224, List.of("Science Fiction", "Humor"), "https://covers.openlibrary.org/b/isbn/9780345391803-L.jpg", "en", 4.6, "9780345391803", 1979, false, 4, 4),

                                book("Ender's Game", List.of("Orson Scott Card"), "Tor Books",
                                        "A gifted child is sent to military school in space to prepare for an alien invasion.",
                                        324, List.of("Science Fiction", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780812550702-L.jpg", "en", 4.6, "9780812550702", 1985, false, 6, 6),

                                book("The Hunger Games", List.of("Suzanne Collins"), "Scholastic",
                                        "In a dystopian future, teenagers are forced to fight to the death in a televised competition.",
                                        374, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780439023481-L.jpg", "en", 4.5, "9780439023481", 2008, true, 8, 8),

                                book("Catching Fire", List.of("Suzanne Collins"), "Scholastic",
                                        "Katniss Everdeen becomes a symbol of rebellion against the oppressive Capitol.",
                                        391, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780439023498-L.jpg", "en", 4.4, "9780439023498", 2009, false, 6, 6),

                                book("Mockingjay", List.of("Suzanne Collins"), "Scholastic",
                                        "Katniss leads a revolution against the Capitol in the final installment of the series.",
                                        390, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780439023511-L.jpg", "en", 4.3, "9780439023511", 2010, false, 5, 5),

                                book("The Maze Runner", List.of("James Dashner"), "Delacorte Press",
                                        "A teenager wakes up in a mysterious maze with no memory and must find a way out.",
                                        375, List.of("Science Fiction", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780385737944-L.jpg", "en", 4.3, "9780385737944", 2009, false, 3, 3),

                                book("Divergent", List.of("Veronica Roth"), "Katherine Tegen Books",
                                        "In a society divided into factions, one girl discovers she doesn't fit into any single group.",
                                        487, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780062024039-L.jpg", "en", 4.3, "9780062024039", 2011, false, 4, 4),

                                book("The Giver", List.of("Lois Lowry"), "Houghton Mifflin",
                                        "A boy in a seemingly perfect society discovers the dark truth behind his community's sameness.",
                                        208, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780544336261-L.jpg", "en", 4.5, "9780544336261", 1993, false, 5, 5),

                                book("Ready Player One", List.of("Ernest Cline"), "Crown Publishers",
                                        "In a bleak future, a teenager searches for an Easter egg hidden inside a vast virtual reality.",
                                        374, List.of("Science Fiction", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780307887443-L.jpg", "en", 4.3, "9780307887443", 2011, false, 6, 6),

                                // ===== FICTION — LITERARY CLASSICS =====
                                book("To Kill a Mockingbird", List.of("Harper Lee"), "J.B. Lippincott & Co.",
                                        "A young girl in the American South witnesses her father defend a Black man accused of a crime.",
                                        336, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780061120084-L.jpg", "en", 4.7, "9780061120084", 1960, true, 8, 8),

                                book("The Great Gatsby", List.of("F. Scott Fitzgerald"), "Charles Scribner's Sons",
                                        "A mysterious millionaire's obsession with a beautiful woman in the Jazz Age.",
                                        180, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780743273565-L.jpg", "en", 4.4, "9780743273565", 1925, true, 7, 7),

                                book("Pride and Prejudice", List.of("Jane Austen"), "T. Egerton",
                                        "Elizabeth Bennet navigates love, class, and misunderstandings in Regency-era England.",
                                        432, List.of("Classic Literature", "Romance"), "https://covers.openlibrary.org/b/isbn/9780141439518-L.jpg", "en", 4.6, "9780141439518", 1813, false, 6, 6),

                                book("Jane Eyre", List.of("Charlotte Brontë"), "Smith, Elder & Co.",
                                        "An orphaned governess finds love and independence at the brooding Thornfield Hall.",
                                        532, List.of("Classic Literature", "Romance"), "https://covers.openlibrary.org/b/isbn/9780141441146-L.jpg", "en", 4.5, "9780141441146", 1847, false, 4, 4),

                                book("Wuthering Heights", List.of("Emily Brontë"), "Thomas Cautley Newby",
                                        "A tale of passionate and destructive love on the Yorkshire moors.",
                                        416, List.of("Classic Literature", "Romance"), "https://covers.openlibrary.org/b/isbn/9780141439556-L.jpg", "en", 4.3, "9780141439556", 1847, false, 5, 5),

                                book("The Catcher in the Rye", List.of("J.D. Salinger"), "Little, Brown and Company",
                                        "A disillusioned teenager wanders New York City after being expelled from prep school.",
                                        234, List.of("Classic Literature", "Coming of Age"), "https://covers.openlibrary.org/b/isbn/9780316769488-L.jpg", "en", 4.3, "9780316769488", 1951, false, 6, 6),

                                book("Lord of the Flies", List.of("William Golding"), "Faber and Faber",
                                        "Stranded boys on an island descend into savagery without adult supervision.",
                                        224, List.of("Classic Literature", "Allegory"), "https://covers.openlibrary.org/b/isbn/9780399501487-L.jpg", "en", 4.3, "9780399501487", 1954, false, 7, 7),

                                book("Of Mice and Men", List.of("John Steinbeck"), "Covici Friede",
                                        "Two displaced migrant workers dream of owning their own piece of land during the Great Depression.",
                                        107, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780140186420-L.jpg", "en", 4.4, "9780140186420", 1937, false, 8, 8),

                                book("Animal Farm", List.of("George Orwell"), "Secker & Warburg",
                                        "A political allegory in which farm animals overthrow their human owner only to face new tyranny.",
                                        112, List.of("Classic Literature", "Satire"), "https://covers.openlibrary.org/b/isbn/9780451526342-L.jpg", "en", 4.5, "9780451526342", 1945, false, 10, 10),

                                book("The Adventures of Huckleberry Finn", List.of("Mark Twain"), "Chatto & Windus",
                                        "A boy and a runaway slave travel down the Mississippi River seeking freedom.",
                                        366, List.of("Classic Literature", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780142437179-L.jpg", "en", 4.3, "9780142437179", 1884, false, 4, 4),

                                book("Great Expectations", List.of("Charles Dickens"), "Chapman & Hall",
                                        "An orphan named Pip navigates social class and personal growth in Victorian England.",
                                        544, List.of("Classic Literature", "Coming of Age"), "https://covers.openlibrary.org/b/isbn/9780141439563-L.jpg", "en", 4.3, "9780141439563", 1861, false, 3, 3),

                                book("A Tale of Two Cities", List.of("Charles Dickens"), "Chapman & Hall",
                                        "A story of love and sacrifice set against the backdrop of the French Revolution.",
                                        489, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780141439600-L.jpg", "en", 4.4, "9780141439600", 1859, false, 6, 6),

                                book("Romeo and Juliet", List.of("William Shakespeare"), "Various",
                                        "The tragic love story of two young people from feuding families in Verona.",
                                        320, List.of("Classic Literature", "Drama"), "https://covers.openlibrary.org/b/isbn/9780743477116-L.jpg", "en", 4.4, "9780743477116", 1597, false, 9, 9),

                                book("Hamlet", List.of("William Shakespeare"), "Various",
                                        "The Prince of Denmark seeks revenge after his father's murder by his uncle.",
                                        342, List.of("Classic Literature", "Drama"), "https://covers.openlibrary.org/b/isbn/9780743477123-L.jpg", "en", 4.5, "9780743477123", 1603, false, 8, 8),

                                book("Macbeth", List.of("William Shakespeare"), "Various",
                                        "A Scottish general's ambition leads to murder, guilt, and his ultimate downfall.",
                                        256, List.of("Classic Literature", "Drama"), "https://covers.openlibrary.org/b/isbn/9780743477109-L.jpg", "en", 4.4, "9780743477109", 1606, false, 7, 7),

                                book("The Odyssey", List.of("Homer"), "Various",
                                        "Odysseus's epic journey home after the Trojan War, facing monsters and divine obstacles.",
                                        541, List.of("Classic Literature", "Epic Poetry"), "https://covers.openlibrary.org/b/isbn/9780140268867-L.jpg", "en", 4.4, "9780140268867", -800, false, 5, 5),

                                book("Frankenstein", List.of("Mary Shelley"), "Lackington, Hughes, Harding, Mavor & Jones",
                                        "A scientist creates a sapient creature, leading to tragic consequences for both.",
                                        280, List.of("Classic Literature", "Gothic", "Science Fiction"), "https://covers.openlibrary.org/b/isbn/9780141439471-L.jpg", "en", 4.3, "9780141439471", 1818, false, 8, 8),

                                book("Dracula", List.of("Bram Stoker"), "Archibald Constable and Company",
                                        "A vampire count from Transylvania stalks his prey in Victorian England.",
                                        418, List.of("Classic Literature", "Gothic", "Horror"), "https://covers.openlibrary.org/b/isbn/9780141439846-L.jpg", "en", 4.3, "9780141439846", 1897, false, 7, 7),

                                // ===== FICTION — CONTEMPORARY & MODERN =====
                                book("The Fault in Our Stars", List.of("John Green"), "Dutton Books",
                                        "Two teenagers with cancer fall in love and travel to Amsterdam for one last adventure.",
                                        313, List.of("Contemporary Fiction", "Young Adult", "Romance"), "https://covers.openlibrary.org/b/isbn/9780525478812-L.jpg", "en", 4.4, "9780525478812", 2012, false, 9, 9),

                                book("The Book Thief", List.of("Markus Zusak"), "Picador",
                                        "Narrated by Death, a girl in Nazi Germany finds solace by stealing books.",
                                        584, List.of("Historical Fiction", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780375842207-L.jpg", "en", 4.6, "9780375842207", 2005, true, 8, 8),

                                book("The Kite Runner", List.of("Khaled Hosseini"), "Riverhead Books",
                                        "An Afghan man confronts his past betrayal and seeks redemption in war-torn Afghanistan.",
                                        371, List.of("Contemporary Fiction", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9781594631931-L.jpg", "en", 4.5, "9781594631931", 2003, false, 5, 5),

                                book("Life of Pi", List.of("Yann Martel"), "Knopf Canada",
                                        "A boy is stranded on a lifeboat in the Pacific Ocean with a Bengal tiger.",
                                        460, List.of("Contemporary Fiction", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780156027328-L.jpg", "en", 4.4, "9780156027328", 2001, false, 4, 4),

                                book("The Alchemist", List.of("Paulo Coelho"), "HarperOne",
                                        "A young Andalusian shepherd follows his dreams on a journey to the Egyptian pyramids.",
                                        197, List.of("Contemporary Fiction", "Philosophy"), "https://covers.openlibrary.org/b/isbn/9780062315007-L.jpg", "en", 4.3, "9780062315007", 1988, false, 7, 7),

                                book("The Perks of Being a Wallflower", List.of("Stephen Chbosky"), "MTV Books",
                                        "An introverted teen navigates high school life through a series of anonymous letters.",
                                        213, List.of("Contemporary Fiction", "Young Adult", "Coming of Age"), "https://covers.openlibrary.org/b/isbn/9781451696196-L.jpg", "en", 4.4, "9781451696196", 1999, false, 6, 6),

                                book("Wonder", List.of("R.J. Palacio"), "Knopf Books for Young Readers",
                                        "A boy with facial differences attends mainstream school for the first time.",
                                        315, List.of("Contemporary Fiction", "Children's Literature"), "https://covers.openlibrary.org/b/isbn/9780375869020-L.jpg", "en", 4.6, "9780375869020", 2012, true, 8, 8),

                                book("The Curious Incident of the Dog in the Night-Time", List.of("Mark Haddon"), "Jonathan Cape",
                                        "A boy with autism investigates the death of a neighbor's dog and uncovers family secrets.",
                                        226, List.of("Contemporary Fiction", "Mystery"), "https://covers.openlibrary.org/b/isbn/9781400032716-L.jpg", "en", 4.3, "9781400032716", 2003, false, 3, 3),

                                book("Holes", List.of("Louis Sachar"), "Farrar, Straus and Giroux",
                                        "A boy is sent to a juvenile detention camp where inmates dig holes all day.",
                                        233, List.of("Young Adult", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780440414803-L.jpg", "en", 4.5, "9780440414803", 1998, false, 5, 5),

                                book("Matilda", List.of("Roald Dahl"), "Jonathan Cape",
                                        "A brilliant little girl uses her intelligence and telekinetic powers to overcome her awful parents and headmistress.",
                                        240, List.of("Children's Literature", "Fantasy"), "https://covers.openlibrary.org/b/isbn/9780142410370-L.jpg", "en", 4.6, "9780142410370", 1988, false, 6, 6),

                                book("Charlie and the Chocolate Factory", List.of("Roald Dahl"), "Alfred A. Knopf",
                                        "A poor boy wins a tour of the most magnificent chocolate factory in the world.",
                                        176, List.of("Children's Literature", "Fantasy"), "https://covers.openlibrary.org/b/isbn/9780142410318-L.jpg", "en", 4.6, "9780142410318", 1964, false, 7, 7),

                                book("Charlotte's Web", List.of("E.B. White"), "Harper & Brothers",
                                        "A spider named Charlotte saves her pig friend Wilbur from slaughter through her webs.",
                                        184, List.of("Children's Literature", "Fantasy"), "https://covers.openlibrary.org/b/isbn/9780064400558-L.jpg", "en", 4.6, "9780064400558", 1952, false, 4, 4),

                                book("The Little Prince", List.of("Antoine de Saint-Exupéry"), "Reynal & Hitchcock",
                                        "A pilot stranded in the Sahara meets a young prince from a tiny asteroid.",
                                        96, List.of("Children's Literature", "Philosophy"), "https://covers.openlibrary.org/b/isbn/9780156012195-L.jpg", "en", 4.7, "9780156012195", 1943, true, 10, 10),

                                // ===== NON-FICTION — SCIENCE =====
                                book("A Brief History of Time", List.of("Stephen Hawking"), "Bantam Dell",
                                        "An accessible exploration of cosmology, black holes, and the nature of time.",
                                        256, List.of("Science", "Physics", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780553380163-L.jpg", "en", 4.6, "9780553380163", 1988, true, 8, 8),

                                book("Cosmos", List.of("Carl Sagan"), "Random House",
                                        "A sweeping journey through the universe exploring science, civilization, and our place in the cosmos.",
                                        396, List.of("Science", "Astronomy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780345539434-L.jpg", "en", 4.6, "9780345539434", 1980, false, 5, 5),

                                book("The Selfish Gene", List.of("Richard Dawkins"), "Oxford University Press",
                                        "A groundbreaking look at evolution from the gene's perspective.",
                                        360, List.of("Science", "Biology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780199291151-L.jpg", "en", 4.4, "9780199291151", 1976, false, 4, 4),

                                book("Sapiens: A Brief History of Humankind", List.of("Yuval Noah Harari"), "Harper",
                                        "A sweeping narrative of human history from the Stone Age to the present.",
                                        443, List.of("Science", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780062316097-L.jpg", "en", 4.5, "9780062316097", 2011, true, 9, 9),

                                book("The Origin of Species", List.of("Charles Darwin"), "John Murray",
                                        "Darwin's foundational work on the theory of evolution by natural selection.",
                                        502, List.of("Science", "Biology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780451529060-L.jpg", "en", 4.3, "9780451529060", 1859, false, 2, 2),

                                book("Silent Spring", List.of("Rachel Carson"), "Houghton Mifflin",
                                        "A landmark work documenting the environmental damage caused by pesticides.",
                                        378, List.of("Science", "Environment", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780618249060-L.jpg", "en", 4.4, "9780618249060", 1962, false, 4, 4),

                                book("The Immortal Life of Henrietta Lacks", List.of("Rebecca Skloot"), "Crown",
                                        "The story of the woman whose cells were taken without consent and became vital for medical research.",
                                        381, List.of("Science", "Biography", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781400052189-L.jpg", "en", 4.5, "9781400052189", 2010, false, 6, 6),

                                book("Astrophysics for People in a Hurry", List.of("Neil deGrasse Tyson"), "W. W. Norton",
                                        "A concise guide to the most important ideas in astrophysics.",
                                        224, List.of("Science", "Astronomy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780393609394-L.jpg", "en", 4.3, "9780393609394", 2017, false, 7, 7),

                                // ===== NON-FICTION — MATHEMATICS =====
                                book("Fermat's Last Theorem", List.of("Simon Singh"), "Fourth Estate",
                                        "The riveting story of how Andrew Wiles proved Fermat's famous conjecture.",
                                        340, List.of("Mathematics", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781841157917-L.jpg", "en", 4.5, "9781841157917", 1997, false, 3, 3),

                                book("Flatland: A Romance of Many Dimensions", List.of("Edwin A. Abbott"), "Seeley & Co.",
                                        "A satirical novella exploring dimensions through life in a two-dimensional world.",
                                        96, List.of("Mathematics", "Science Fiction"), "https://covers.openlibrary.org/b/isbn/9780486272634-L.jpg", "en", 4.2, "9780486272634", 1884, false, 2, 2),

                                book("Night", List.of("Elie Wiesel"), "Hill & Wang",
                                        "A harrowing memoir of survival in the Auschwitz and Buchenwald concentration camps.",
                                        120, List.of("History", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780374500016-L.jpg", "en", 4.6, "9780374500016", 1956, false, 6, 6),

                                book("Guns, Germs, and Steel", List.of("Jared Diamond"), "W. W. Norton",
                                        "An exploration of why some civilizations conquered others through geography, technology, and biology.",
                                        480, List.of("History", "Anthropology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780393317558-L.jpg", "en", 4.3, "9780393317558", 1997, false, 5, 5),

                                book("A People's History of the United States", List.of("Howard Zinn"), "Harper & Row",
                                        "American history told from the perspective of marginalized groups rather than political leaders.",
                                        729, List.of("History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780060838652-L.jpg", "en", 4.4, "9780060838652", 1980, false, 4, 4),

                                book("The Art of War", List.of("Sun Tzu"), "Various",
                                        "An ancient Chinese military treatise on strategy and tactics.",
                                        273, List.of("History", "Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781590302255-L.jpg", "en", 4.3, "9781590302255", -500, false, 8, 8),

                                // ===== NON-FICTION — PHILOSOPHY & PSYCHOLOGY =====
                                book("Thinking, Fast and Slow", List.of("Daniel Kahneman"), "Farrar, Straus and Giroux",
                                        "A Nobel laureate explores the two systems that drive the way we think.",
                                        499, List.of("Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780374533557-L.jpg", "en", 4.4, "9780374533557", 2011, false, 6, 6),

                                book("Man's Search for Meaning", List.of("Viktor E. Frankl"), "Beacon Press",
                                        "A psychiatrist's memoir of surviving the Holocaust and finding purpose through suffering.",
                                        184, List.of("Psychology", "Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780807014295-L.jpg", "en", 4.7, "9780807014295", 1946, true, 9, 9),

                                book("The Republic", List.of("Plato"), "Various",
                                        "Plato's foundational work on justice, the ideal state, and the nature of reality.",
                                        416, List.of("Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780140455113-L.jpg", "en", 4.3, "9780140455113", -380, false, 3, 3),

                                book("Meditations", List.of("Marcus Aurelius"), "Various",
                                        "Personal reflections by the Roman Emperor on Stoic philosophy and self-improvement.",
                                        256, List.of("Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780140449334-L.jpg", "en", 4.5, "9780140449334", 180, false, 5, 5),

                                // ===== NON-FICTION — BIOGRAPHY & MEMOIR =====
                                book("The Autobiography of Malcolm X", List.of("Malcolm X", "Alex Haley"), "Grove Press",
                                        "The life story of the civil rights leader, from his troubled youth to his transformation.",
                                        500, List.of("Biography", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780345350688-L.jpg", "en", 4.6, "9780345350688", 1965, false, 4, 4),

                                book("Long Walk to Freedom", List.of("Nelson Mandela"), "Little, Brown and Company",
                                        "Nelson Mandela's autobiography detailing his struggle against apartheid in South Africa.",
                                        656, List.of("Biography", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780316548182-L.jpg", "en", 4.6, "9780316548182", 1994, false, 6, 6),

                                book("Steve Jobs", List.of("Walter Isaacson"), "Simon & Schuster",
                                        "The authorized biography of the co-founder of Apple, based on extensive interviews.",
                                        656, List.of("Biography", "Technology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781451648539-L.jpg", "en", 4.4, "9781451648539", 2011, false, 5, 5),

                                book("I Know Why the Caged Bird Sings", List.of("Maya Angelou"), "Random House",
                                        "Maya Angelou's powerful memoir of growing up as a Black girl in the American South.",
                                        289, List.of("Biography", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780345514400-L.jpg", "en", 4.5, "9780345514400", 1969, false, 4, 4),

                                book("Educated", List.of("Tara Westover"), "Random House",
                                        "A memoir about a woman who grows up in a survivalist family and eventually earns a PhD from Cambridge.",
                                        334, List.of("Biography", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780399590504-L.jpg", "en", 4.5, "9780399590504", 2018, false, 7, 7),

                                book("Becoming", List.of("Michelle Obama"), "Crown",
                                        "The former First Lady's memoir covering her roots, career, and time in the White House.",
                                        448, List.of("Biography", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781524763138-L.jpg", "en", 4.5, "9781524763138", 2018, false, 8, 8),

                                // ===== PROGRAMMING & COMPUTER SCIENCE =====
                                book("Clean Code", List.of("Robert C. Martin"), "Prentice Hall",
                                        "A handbook of agile software craftsmanship focusing on writing maintainable code.",
                                        464, List.of("Programming", "Software Engineering"), "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg", "en", 4.7, "9780132350884", 2008, true, 10, 10),

                                book("The Pragmatic Programmer", List.of("Andrew Hunt", "David Thomas"), "Addison-Wesley",
                                        "Classic guide for pragmatic and effective software development practices.",
                                        352, List.of("Programming", "Best Practices"), "https://covers.openlibrary.org/b/isbn/9780201616224-L.jpg", "en", 4.6, "9780201616224", 1999, false, 5, 5),

                                book("Design Patterns: Elements of Reusable Object-Oriented Software", List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"), "Addison-Wesley",
                                        "The original Gang of Four book introducing foundational design patterns.",
                                        395, List.of("Programming", "Architecture"), "https://covers.openlibrary.org/b/isbn/9780201633610-L.jpg", "en", 4.5, "9780201633610", 1994, false, 4, 4),

                                book("Effective Java", List.of("Joshua Bloch"), "Addison-Wesley",
                                        "Best practices and expert recommendations for writing robust Java code.",
                                        416, List.of("Java", "Programming"), "https://covers.openlibrary.org/b/isbn/9780134685991-L.jpg", "en", 4.8, "9780134685991", 2018, true, 6, 6),

                                book("Introduction to Algorithms", List.of("Thomas H. Cormen", "Charles E. Leiserson", "Ronald L. Rivest", "Clifford Stein"), "MIT Press",
                                        "Widely used textbook covering a broad range of algorithms in depth.",
                                        1312, List.of("Algorithms", "Computer Science"), "https://covers.openlibrary.org/b/isbn/9780262033848-L.jpg", "en", 4.6, "9780262033848", 2009, false, 2, 2),

                                book("Clean Architecture", List.of("Robert C. Martin"), "Prentice Hall",
                                        "A guide to building scalable and maintainable software architecture.",
                                        432, List.of("Architecture", "Software Engineering"), "https://covers.openlibrary.org/b/isbn/9780134494166-L.jpg", "en", 4.4, "9780134494166", 2017, false, 5, 5),

                                book("Refactoring", List.of("Martin Fowler"), "Addison-Wesley",
                                        "Improving the design of existing code through structured refactoring techniques.",
                                        448, List.of("Programming", "Code Quality"), "https://covers.openlibrary.org/b/isbn/9780201485677-L.jpg", "en", 4.5, "9780201485677", 1999, false, 4, 4),

                                book("The Clean Coder", List.of("Robert C. Martin"), "Prentice Hall",
                                        "A code of conduct for professional programmers.",
                                        256, List.of("Programming", "Career"), "https://covers.openlibrary.org/b/isbn/9780137081073-L.jpg", "en", 4.3, "9780137081073", 2011, false, 3, 3),

                                book("Head First Design Patterns", List.of("Eric Freeman", "Elisabeth Robson"), "O'Reilly Media",
                                        "A visually rich introduction to design patterns in software development.",
                                        694, List.of("Programming", "Design Patterns"), "https://covers.openlibrary.org/b/isbn/9780596007126-L.jpg", "en", 4.6, "9780596007126", 2004, false, 6, 6),

                                book("Spring in Action", List.of("Craig Walls"), "Manning",
                                        "Comprehensive guide to developing applications using the Spring Framework.",
                                        520, List.of("Java", "Spring"), "https://covers.openlibrary.org/b/isbn/9781617294945-L.jpg", "en", 4.4, "9781617294945", 2018, false, 4, 4),

                                book("You Don't Know JS Yet", List.of("Kyle Simpson"), "Independently published",
                                        "Deep dive into JavaScript core mechanisms and advanced concepts.",
                                        143, List.of("JavaScript", "Web Development"), "https://covers.openlibrary.org/b/isbn/9781091210099-L.jpg", "en", 4.4, "9781091210099", 2020, false, 7, 7),

                                book("Domain-Driven Design", List.of("Eric Evans"), "Addison-Wesley",
                                        "Tackling complexity in the heart of software with domain-driven design.",
                                        560, List.of("Architecture", "Software Design"), "https://covers.openlibrary.org/b/isbn/9780321125217-L.jpg", "en", 4.5, "9780321125217", 2003, false, 3, 3),

                                book("Structure and Interpretation of Computer Programs", List.of("Harold Abelson", "Gerald Jay Sussman"), "MIT Press",
                                        "A classic computer science text using Scheme to teach fundamental programming concepts.",
                                        657, List.of("Computer Science", "Programming"), "https://covers.openlibrary.org/b/isbn/9780262510875-L.jpg", "en", 4.6, "9780262510875", 1996, false, 2, 2),

                                book("Code Complete", List.of("Steve McConnell"), "Microsoft Press",
                                        "A comprehensive handbook of software construction techniques and best practices.",
                                        960, List.of("Programming", "Software Engineering"), "https://covers.openlibrary.org/b/isbn/9780735619678-L.jpg", "en", 4.5, "9780735619678", 2004, false, 4, 4),

                                book("Cracking the Coding Interview", List.of("Gayle Laakmann McDowell"), "CareerCup",
                                        "189 programming questions and solutions to prepare for technical interviews.",
                                        687, List.of("Programming", "Career"), "https://covers.openlibrary.org/b/isbn/9780984782857-L.jpg", "en", 4.4, "9780984782857", 2015, false, 5, 5),

                                // ===== FICTION — MYSTERY & THRILLER =====
                                book("The Girl with the Dragon Tattoo", List.of("Stieg Larsson"), "Norstedts Förlag",
                                        "A journalist and a hacker investigate a decades-old disappearance in Sweden.",
                                        672, List.of("Mystery", "Thriller"), "https://covers.openlibrary.org/b/isbn/9780307454546-L.jpg", "en", 4.4, "9780307454546", 2005, false, 8, 8),

                                book("Gone Girl", List.of("Gillian Flynn"), "Crown Publishing Group",
                                        "A woman disappears on her fifth wedding anniversary, and suspicion falls on her husband.",
                                        432, List.of("Mystery", "Thriller"), "https://covers.openlibrary.org/b/isbn/9780307588371-L.jpg", "en", 4.3, "9780307588371", 2012, false, 7, 7),

                                book("And Then There Were None", List.of("Agatha Christie"), "Collins Crime Club",
                                        "Ten strangers are lured to an island where they are murdered one by one.",
                                        272, List.of("Mystery", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780062073488-L.jpg", "en", 4.6, "9780062073488", 1939, false, 9, 9),

                                book("Murder on the Orient Express", List.of("Agatha Christie"), "Collins Crime Club",
                                        "Detective Hercule Poirot investigates a murder on a snowbound train.",
                                        274, List.of("Mystery", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780062073501-L.jpg", "en", 4.5, "9780062073501", 1934, false, 6, 6),

                                book("The Da Vinci Code", List.of("Dan Brown"), "Doubleday",
                                        "A symbologist uncovers a religious mystery hidden in the works of Leonardo da Vinci.",
                                        689, List.of("Mystery", "Thriller"), "https://covers.openlibrary.org/b/isbn/9780307474278-L.jpg", "en", 4.3, "9780307474278", 2003, false, 8, 8),

                                book("The Hound of the Baskervilles", List.of("Arthur Conan Doyle"), "George Newnes",
                                        "Sherlock Holmes investigates a supernatural hound haunting a family on the moors of Devon.",
                                        256, List.of("Mystery", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141329390-L.jpg", "en", 4.4, "9780141329390", 1902, false, 5, 5),

                                // ===== FICTION — HORROR =====
                                book("It", List.of("Stephen King"), "Viking Press",
                                        "A group of friends confronts an evil entity that takes the form of a terrifying clown.",
                                        1138, List.of("Horror", "Thriller"), "https://covers.openlibrary.org/b/isbn/9781501142970-L.jpg", "en", 4.4, "9781501142970", 1986, false, 4, 4),

                                book("The Shining", List.of("Stephen King"), "Doubleday",
                                        "A family moves into an isolated hotel for the winter where a sinister presence influences the father.",
                                        447, List.of("Horror", "Thriller"), "https://covers.openlibrary.org/b/isbn/9780307743657-L.jpg", "en", 4.5, "9780307743657", 1977, false, 6, 6),

                                // ===== GRAPHIC NOVELS & COMICS =====
                                book("Maus", List.of("Art Spiegelman"), "Pantheon Books",
                                        "A graphic novel depicting the author's father's experience as a Holocaust survivor, with Jews as mice and Nazis as cats.",
                                        296, List.of("Graphic Novel", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780394747231-L.jpg", "en", 4.7, "9780394747231", 1986, false, 7, 7),

                                book("Persepolis", List.of("Marjane Satrapi"), "L'Association",
                                        "A graphic memoir about growing up in Iran during the Islamic Revolution.",
                                        160, List.of("Graphic Novel", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780375714573-L.jpg", "en", 4.4, "9780375714573", 2000, false, 5, 5),

                                book("Watchmen", List.of("Alan Moore", "Dave Gibbons"), "DC Comics",
                                        "A deconstruction of the superhero genre set in an alternate Cold War America.",
                                        416, List.of("Graphic Novel", "Science Fiction"), "https://covers.openlibrary.org/b/isbn/9781401245252-L.jpg", "en", 4.5, "9781401245252", 1987, false, 4, 4),

                                // ===== NON-FICTION — ECONOMICS & BUSINESS =====
                                book("Freakonomics", List.of("Steven D. Levitt", "Stephen J. Dubner"), "William Morrow",
                                        "A rogue economist explores the hidden side of everything using data and incentives.",
                                        336, List.of("Economics", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780060731335-L.jpg", "en", 4.2, "9780060731335", 2005, false, 6, 6),

                                book("The Lean Startup", List.of("Eric Ries"), "Crown Business",
                                        "How today's entrepreneurs use continuous innovation to create radically successful businesses.",
                                        336, List.of("Business", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780307887894-L.jpg", "en", 4.3, "9780307887894", 2011, false, 5, 5),

                                book("Rich Dad Poor Dad", List.of("Robert T. Kiyosaki"), "Warner Books",
                                        "Personal finance lessons learned from two father figures with different approaches to money.",
                                        336, List.of("Finance", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781612680194-L.jpg", "en", 4.2, "9781612680194", 1997, false, 9, 9),

                                // ===== NON-FICTION — SELF-HELP & PERSONAL DEVELOPMENT =====
                                book("How to Win Friends and Influence People", List.of("Dale Carnegie"), "Simon & Schuster",
                                        "A timeless guide to building relationships and influencing others positively.",
                                        288, List.of("Self-Help", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780671027032-L.jpg", "en", 4.4, "9780671027032", 1936, false, 10, 10),

                                book("Atomic Habits", List.of("James Clear"), "Avery",
                                        "Practical strategies for building good habits and breaking bad ones.",
                                        320, List.of("Self-Help", "Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780735211292-L.jpg", "en", 4.5, "9780735211292", 2018, false, 8, 8),

                                book("The 7 Habits of Highly Effective People", List.of("Stephen R. Covey"), "Free Press",
                                        "A principle-centered approach to personal and professional effectiveness.",
                                        381, List.of("Self-Help", "Business", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781451639612-L.jpg", "en", 4.4, "9781451639612", 1989, false, 7, 7),

                                book("Outliers: The Story of Success", List.of("Malcolm Gladwell"), "Little, Brown and Company",
                                        "An examination of the factors that contribute to high levels of success.",
                                        309, List.of("Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780316017930-L.jpg", "en", 4.3, "9780316017930", 2008, false, 5, 5),

                                book("Quiet: The Power of Introverts", List.of("Susan Cain"), "Crown Publishers",
                                        "An exploration of how introverts are undervalued in a culture that prizes extroversion.",
                                        333, List.of("Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780307352156-L.jpg", "en", 4.4, "9780307352156", 2012, false, 6, 6),

                                // ===== NON-FICTION — ARTS & CREATIVITY =====
                                book("Steal Like an Artist", List.of("Austin Kleon"), "Workman Publishing",
                                        "Ten things nobody told you about being creative, with practical advice for artists.",
                                        160, List.of("Art", "Creativity", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780761169253-L.jpg", "en", 4.3, "9780761169253", 2012, false, 8, 8),

                                book("The Elements of Style", List.of("William Strunk Jr.", "E.B. White"), "Longman",
                                        "The classic guide to writing clear, concise English prose.",
                                        105, List.of("Writing", "Reference", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780205309023-L.jpg", "en", 4.4, "9780205309023", 1959, false, 3, 3),

                                book("On Writing: A Memoir of the Craft", List.of("Stephen King"), "Scribner",
                                        "Part memoir, part masterclass on the craft of writing from one of the best-selling authors.",
                                        320, List.of("Writing", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781439156810-L.jpg", "en", 4.6, "9781439156810", 2000, false, 5, 5),

                                // ===== FICTION — ADVENTURE =====
                                book("Treasure Island", List.of("Robert Louis Stevenson"), "Cassell and Company",
                                        "A young boy's adventure in search of pirate treasure on a remote island.",
                                        292, List.of("Adventure", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141321004-L.jpg", "en", 4.3, "9780141321004", 1883, false, 4, 4),

                                book("Robinson Crusoe", List.of("Daniel Defoe"), "W. Taylor",
                                        "A castaway survives alone on a deserted island for nearly three decades.",
                                        320, List.of("Adventure", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141439822-L.jpg", "en", 4.2, "9780141439822", 1719, false, 3, 3),

                                book("The Call of the Wild", List.of("Jack London"), "Macmillan",
                                        "A domesticated dog is stolen and sold into service as a sled dog in the Yukon.",
                                        232, List.of("Adventure", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141321059-L.jpg", "en", 4.4, "9780141321059", 1903, false, 5, 5),

                                book("Around the World in Eighty Days", List.of("Jules Verne"), "Pierre-Jules Hetzel",
                                        "Phileas Fogg bets that he can circumnavigate the globe in just eighty days.",
                                        256, List.of("Adventure", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141441108-L.jpg", "en", 4.3, "9780141441108", 1873, false, 6, 6),

                                book("Twenty Thousand Leagues Under the Sea", List.of("Jules Verne"), "Pierre-Jules Hetzel",
                                        "Captain Nemo takes passengers on an extraordinary submarine voyage across the world's oceans.",
                                        304, List.of("Adventure", "Science Fiction", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141441948-L.jpg", "en", 4.3, "9780141441948", 1870, false, 4, 4),

                                // ===== FICTION — INTERNATIONAL LITERATURE =====
                                book("One Hundred Years of Solitude", List.of("Gabriel García Márquez"), "Harper & Row",
                                        "The multi-generational saga of the Buendía family in the fictional town of Macondo.",
                                        417, List.of("Literary Fiction", "Magical Realism"), "https://covers.openlibrary.org/b/isbn/9780060883287-L.jpg", "en", 4.5, "9780060883287", 1967, false, 4, 4),

                                book("Crime and Punishment", List.of("Fyodor Dostoevsky"), "The Russian Messenger",
                                        "A destitute student commits murder and struggles with guilt and moral dilemmas.",
                                        671, List.of("Classic Literature", "Psychological Fiction"), "https://covers.openlibrary.org/b/isbn/9780140449136-L.jpg", "en", 4.4, "9780140449136", 1866, false, 5, 5),

                                book("War and Peace", List.of("Leo Tolstoy"), "The Russian Messenger",
                                        "An epic novel of Russian society during the Napoleonic era, intertwining love, war, and philosophy.",
                                        1225, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780140447934-L.jpg", "en", 4.4, "9780140447934", 1869, false, 3, 3),

                                book("The Metamorphosis", List.of("Franz Kafka"), "Kurt Wolff Verlag",
                                        "A man wakes up one morning to find himself transformed into a giant insect.",
                                        201, List.of("Classic Literature", "Existentialism"), "https://covers.openlibrary.org/b/isbn/9780141023458-L.jpg", "en", 4.3, "9780141023458", 1915, false, 6, 6),

                                book("Don Quixote", List.of("Miguel de Cervantes"), "Francisco de Robles",
                                        "A deluded gentleman sets out on a quest as a knight-errant with his loyal squire Sancho Panza.",
                                        1072, List.of("Classic Literature", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780060934347-L.jpg", "en", 4.3, "9780060934347", 1605, false, 2, 2),

                                book("Les Misérables", List.of("Victor Hugo"), "A. Lacroix, Verboeckhoven & Cie.",
                                        "The story of ex-convict Jean Valjean's quest for redemption in 19th-century France.",
                                        1463, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780140444308-L.jpg", "en", 4.5, "9780140444308", 1862, false, 3, 3),

                                book("The Count of Monte Cristo", List.of("Alexandre Dumas"), "Chapman & Hall",
                                        "A young man is wrongfully imprisoned and, upon escape, seeks elaborate revenge on those who betrayed him.",
                                        1276, List.of("Classic Literature", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780140449266-L.jpg", "en", 4.6, "9780140449266", 1844, false, 4, 4),

                                // ===== NON-FICTION — SOCIAL SCIENCE =====
                                book("The New Jim Crow", List.of("Michelle Alexander"), "The New Press",
                                        "An examination of mass incarceration as a system of racial control in America.",
                                        312, List.of("Social Science", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781595588609-L.jpg", "en", 4.6, "9781595588609", 2010, false, 5, 5),

                                book("Between the World and Me", List.of("Ta-Nehisi Coates"), "Spiegel & Grau",
                                        "A letter to the author's son about what it means to be Black in America.",
                                        176, List.of("Social Science", "Memoir", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780812993547-L.jpg", "en", 4.4, "9780812993547", 2015, false, 6, 6),

                                // ===== FICTION — MORE YOUNG ADULT =====
                                book("Eragon", List.of("Christopher Paolini"), "Alfred A. Knopf",
                                        "A farm boy discovers a dragon egg and becomes embroiled in an empire-wide conflict.",
                                        509, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780375826696-L.jpg", "en", 4.3, "9780375826696", 2003, false, 8, 8),

                                book("The Golden Compass", List.of("Philip Pullman"), "Scholastic",
                                        "Lyra embarks on a journey to the Arctic to save her kidnapped friend and uncover a sinister plot.",
                                        399, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780679879244-L.jpg", "en", 4.5, "9780679879244", 1995, false, 6, 6),

                                book("Coraline", List.of("Neil Gaiman"), "Bloomsbury",
                                        "A young girl discovers a parallel world behind a secret door that seems better than her own—at first.",
                                        162, List.of("Fantasy", "Children's Literature", "Horror"), "https://covers.openlibrary.org/b/isbn/9780380807345-L.jpg", "en", 4.4, "9780380807345", 2002, false, 5, 5),

                                book("The Graveyard Book", List.of("Neil Gaiman"), "HarperCollins",
                                        "A boy raised by ghosts in a graveyard must confront the living world and the man who killed his family.",
                                        312, List.of("Fantasy", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780060530945-L.jpg", "en", 4.5, "9780060530945", 2008, false, 4, 4),

                                book("The Outsiders", List.of("S.E. Hinton"), "Viking Press",
                                        "A group of young greasers navigate violence, loyalty, and class division in 1960s Oklahoma.",
                                        192, List.of("Young Adult", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780142407332-L.jpg", "en", 4.4, "9780142407332", 1967, false, 10, 10),

                                book("Thirteen Reasons Why", List.of("Jay Asher"), "RazorBill",
                                        "A high school student receives cassette tapes recorded by a classmate who took her own life, explaining why.",
                                        288, List.of("Young Adult", "Contemporary Fiction"), "https://covers.openlibrary.org/b/isbn/9781595141712-L.jpg", "en", 4.2, "9781595141712", 2007, false, 7, 7),

                                book("Eleanor & Park", List.of("Rainbow Rowell"), "St. Martin's Press",
                                        "Two misfit teenagers find love through shared music and comic books on the school bus.",
                                        325, List.of("Young Adult", "Romance"), "https://covers.openlibrary.org/b/isbn/9781250012579-L.jpg", "en", 4.3, "9781250012579", 2013, false, 6, 6),

                                book("The Giver of Stars", List.of("Jojo Moyes"), "Penguin Books",
                                        "Based on a true story of Depression-era packhorse librarians who delivered books to remote communities.",
                                        400, List.of("Historical Fiction", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780399562495-L.jpg", "en", 4.3, "9780399562495", 2019, false, 5, 5),

                                // ===== POETRY =====
                                book("The Collected Poems of Langston Hughes", List.of("Langston Hughes"), "Vintage Classics",
                                        "The definitive collection of poetry by the leading voice of the Harlem Renaissance.",
                                        736, List.of("Poetry", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780679764083-L.jpg", "en", 4.7, "9780679764083", 1994, false, 3, 3),

                                book("Milk and Honey", List.of("Rupi Kaur"), "Andrews McMeel Publishing",
                                        "A collection of poems about survival, violence, abuse, love, loss, and femininity.",
                                        204, List.of("Poetry", "Contemporary"), "https://covers.openlibrary.org/b/isbn/9781449474256-L.jpg", "en", 4.3, "9781449474256", 2014, false, 8, 8),

                                // ===== NON-FICTION — TECHNOLOGY & SOCIETY =====
                                book("The Code Book", List.of("Simon Singh"), "Anchor Books",
                                        "The history and science of cryptography from ancient Egypt to quantum computing.",
                                        412, List.of("Technology", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780385495325-L.jpg", "en", 4.5, "9780385495325", 1999, false, 4, 4),

                                book("Algorithms to Live By", List.of("Brian Christian", "Tom Griffiths"), "Henry Holt",
                                        "How computer algorithms can be applied to everyday life decisions.",
                                        368, List.of("Computer Science", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781627790369-L.jpg", "en", 4.4, "9781627790369", 2016, false, 6, 6),

                                // ===== NON-FICTION — HEALTH & WELLNESS =====
                                book("Why We Sleep", List.of("Matthew Walker"), "Scribner",
                                        "A neuroscientist explores the purpose of sleep and the consequences of sleep deprivation.",
                                        368, List.of("Science", "Health", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781501144318-L.jpg", "en", 4.5, "9781501144318", 2017, false, 8, 8),

                                book("The Body: A Guide for Occupants", List.of("Bill Bryson"), "Doubleday",
                                        "A fascinating journey through the human body and how it works.",
                                        464, List.of("Science", "Health", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780385539302-L.jpg", "en", 4.4, "9780385539302", 2019, false, 7, 7),

                                // ===== FICTION — ROMANCE =====
                                book("The Notebook", List.of("Nicholas Sparks"), "Warner Books",
                                        "A love story spanning decades between two people from different social backgrounds.",
                                        214, List.of("Romance", "Contemporary Fiction"), "https://covers.openlibrary.org/b/isbn/9780446676090-L.jpg", "en", 4.3, "9780446676090", 1996, false, 9, 9),

                                book("Me Before You", List.of("Jojo Moyes"), "Penguin Books",
                                        "A young woman becomes a caregiver for a paralyzed man, and their lives change forever.",
                                        369, List.of("Romance", "Contemporary Fiction"), "https://covers.openlibrary.org/b/isbn/9780143124542-L.jpg", "en", 4.4, "9780143124542", 2012, false, 7, 7),

                                // ===== NON-FICTION — NATURE & ENVIRONMENT =====
                                book("The Hidden Life of Trees", List.of("Peter Wohlleben"), "Greystone Books",
                                        "A forester reveals the surprising social networks and communication of trees.",
                                        288, List.of("Nature", "Science", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781771642484-L.jpg", "en", 4.3, "9781771642484", 2015, false, 5, 5),

                                book("A Short History of Nearly Everything", List.of("Bill Bryson"), "Broadway Books",
                                        "An accessible and entertaining look at science and the history of human discovery.",
                                        544, List.of("Science", "History", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780767908184-L.jpg", "en", 4.6, "9780767908184", 2003, false, 6, 6),

                                // ===== MORE DIVERSE FICTION =====
                                book("Things Fall Apart", List.of("Chinua Achebe"), "William Heinemann",
                                        "The story of an Igbo community's clash with colonialism in Nigeria.",
                                        209, List.of("Classic Literature", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780385474542-L.jpg", "en", 4.4, "9780385474542", 1958, false, 7, 7),

                                book("The Color Purple", List.of("Alice Walker"), "Harcourt Brace Jovanovich",
                                        "An African-American woman in the rural South overcomes abuse and oppression to find her voice.",
                                        295, List.of("Literary Fiction", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9780156028356-L.jpg", "en", 4.5, "9780156028356", 1982, false, 5, 5),

                                book("Beloved", List.of("Toni Morrison"), "Alfred A. Knopf",
                                        "A former slave is haunted by the ghost of her dead daughter in post-Civil War Ohio.",
                                        324, List.of("Literary Fiction", "Historical Fiction"), "https://covers.openlibrary.org/b/isbn/9781400033416-L.jpg", "en", 4.4, "9781400033416", 1987, false, 4, 4),

                                book("The House on Mango Street", List.of("Sandra Cisneros"), "Arte Público Press",
                                        "A young Latina girl growing up in Chicago narrates vignettes about her neighborhood.",
                                        110, List.of("Literary Fiction", "Coming of Age"), "https://covers.openlibrary.org/b/isbn/9780679734772-L.jpg", "en", 4.3, "9780679734772", 1984, false, 6, 6),

                                book("A Wrinkle in Time", List.of("Madeleine L'Engle"), "Farrar, Straus and Giroux",
                                        "Three children travel through space and time to rescue their scientist father from an evil force.",
                                        256, List.of("Science Fiction", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9780374386139-L.jpg", "en", 4.4, "9780374386139", 1962, false, 5, 5),

                                book("Slaughterhouse-Five", List.of("Kurt Vonnegut"), "Delacorte Press",
                                        "An anti-war novel following a soldier who becomes 'unstuck in time' after surviving the firebombing of Dresden.",
                                        275, List.of("Science Fiction", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780385333481-L.jpg", "en", 4.4, "9780385333481", 1969, false, 4, 4),

                                book("The Handmaid's Tale", List.of("Margaret Atwood"), "McClelland and Stewart",
                                        "In a totalitarian theocracy, women are stripped of their rights and forced into servitude.",
                                        311, List.of("Science Fiction", "Dystopian"), "https://covers.openlibrary.org/b/isbn/9780385490818-L.jpg", "en", 4.4, "9780385490818", 1985, false, 8, 8),

                                book("Flowers for Algernon", List.of("Daniel Keyes"), "Harcourt",
                                        "A mentally disabled man undergoes an experiment to increase his intelligence, with bittersweet results.",
                                        311, List.of("Science Fiction", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780156030083-L.jpg", "en", 4.6, "9780156030083", 1966, false, 6, 6),

                                // ===== NON-FICTION — LANGUAGE & COMMUNICATION =====
                                book("Eats, Shoots & Leaves", List.of("Lynne Truss"), "Profile Books",
                                        "A humorous guide to punctuation and why it matters.",
                                        209, List.of("Language", "Humor", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781592402038-L.jpg", "en", 4.2, "9781592402038", 2003, false, 3, 3),

                                // ===== FICTION — HUMOR =====
                                book("Good Omens", List.of("Terry Pratchett", "Neil Gaiman"), "Gollancz",
                                        "An angel and a demon team up to prevent the apocalypse because they've grown fond of Earth.",
                                        288, List.of("Fantasy", "Humor"), "https://covers.openlibrary.org/b/isbn/9780060853983-L.jpg", "en", 4.5, "9780060853983", 1990, false, 7, 7),

                                book("Catch-22", List.of("Joseph Heller"), "Simon & Schuster",
                                        "A satirical novel about the absurdities of war and military bureaucracy during World War II.",
                                        453, List.of("Classic Literature", "Humor", "War"), "https://covers.openlibrary.org/b/isbn/9781451626650-L.jpg", "en", 4.3, "9781451626650", 1961, false, 5, 5),

                                // ===== MORE NON-FICTION =====
                                book("Homo Deus: A Brief History of Tomorrow", List.of("Yuval Noah Harari"), "Harper",
                                        "A look at the future of humanity and the challenges posed by technology and artificial intelligence.",
                                        450, List.of("Science", "Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780062464316-L.jpg", "en", 4.3, "9780062464316", 2015, false, 8, 8),

                                book("21 Lessons for the 21st Century", List.of("Yuval Noah Harari"), "Spiegel & Grau",
                                        "An exploration of the most pressing issues of our present moment.",
                                        372, List.of("Philosophy", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780525512172-L.jpg", "en", 4.3, "9780525512172", 2018, false, 6, 6),

                                book("The Power of Habit", List.of("Charles Duhigg"), "Random House",
                                        "Why habits exist and how they can be changed in individuals, organizations, and societies.",
                                        371, List.of("Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9781400069286-L.jpg", "en", 4.3, "9781400069286", 2012, false, 7, 7),

                                book("Blink: The Power of Thinking Without Thinking", List.of("Malcolm Gladwell"), "Little, Brown and Company",
                                        "How snap judgments and first impressions can be surprisingly accurate—or dangerously wrong.",
                                        296, List.of("Psychology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780316010665-L.jpg", "en", 4.2, "9780316010665", 2005, false, 5, 5),

                                // ===== FICTION — ADDITIONAL FANTASY/SCI-FI =====
                                book("The Ballad of Songbirds and Snakes", List.of("Suzanne Collins"), "Scholastic",
                                        "A prequel exploring the origins of the Hunger Games through a young Coriolanus Snow.",
                                        517, List.of("Science Fiction", "Dystopian", "Young Adult"), "https://covers.openlibrary.org/b/isbn/9781338635171-L.jpg", "en", 4.2, "9781338635171", 2020, false, 9, 9),

                                book("The Last Wish", List.of("Andrzej Sapkowski"), "SuperNOWA",
                                        "A collection of short stories about Geralt of Rivia, a monster hunter in a dark fantasy world.",
                                        352, List.of("Fantasy", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780316029186-L.jpg", "en", 4.4, "9780316029186", 1993, false, 6, 6),

                                book("American Gods", List.of("Neil Gaiman"), "William Morrow",
                                        "A man becomes entangled in a war between old and new gods of American mythology.",
                                        465, List.of("Fantasy", "Mythology"), "https://covers.openlibrary.org/b/isbn/9780063081918-L.jpg", "en", 4.4, "9780063081918", 2001, false, 4, 4),

                                // ===== NON-FICTION — ART & DESIGN =====
                                book("The Design of Everyday Things", List.of("Don Norman"), "Basic Books",
                                        "A classic guide to usability and human-centered design principles.",
                                        368, List.of("Design", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780465050659-L.jpg", "en", 4.4, "9780465050659", 1988, false, 5, 5),

                                book("Don't Make Me Think", List.of("Steve Krug"), "New Riders",
                                        "A common sense approach to web usability, focusing on intuitive navigation and design.",
                                        216, List.of("Design", "Technology", "Non-Fiction"), "https://covers.openlibrary.org/b/isbn/9780321965516-L.jpg", "en", 4.4, "9780321965516", 2000, false, 7, 7),

                                // ===== CHILDREN'S CLASSICS =====
                                book("The Secret Garden", List.of("Frances Hodgson Burnett"), "Frederick A. Stokes",
                                        "A spoiled young girl discovers a hidden garden and is transformed by nature and friendship.",
                                        331, List.of("Children's Literature", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141321066-L.jpg", "en", 4.4, "9780141321066", 1911, false, 6, 6),

                                book("Anne of Green Gables", List.of("L.M. Montgomery"), "L.C. Page and Company",
                                        "An imaginative orphan girl is mistakenly sent to a farm on Prince Edward Island.",
                                        320, List.of("Children's Literature", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141321592-L.jpg", "en", 4.6, "9780141321592", 1908, false, 8, 8),

                                book("Little Women", List.of("Louisa May Alcott"), "Roberts Brothers",
                                        "Four sisters grow up during the Civil War, navigating love, loss, and independence.",
                                        449, List.of("Classic Literature", "Coming of Age"), "https://covers.openlibrary.org/b/isbn/9780141439693-L.jpg", "en", 4.4, "9780141439693", 1868, false, 6, 6),

                                book("Alice's Adventures in Wonderland", List.of("Lewis Carroll"), "Macmillan",
                                        "A girl falls down a rabbit hole into a fantastical world of illogical creatures.",
                                        272, List.of("Fantasy", "Children's Literature", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141439761-L.jpg", "en", 4.4, "9780141439761", 1865, false, 10, 10),

                                book("Moby-Dick", List.of("Herman Melville"), "Harper & Brothers",
                                        "Captain Ahab's obsessive quest to kill the great white whale that took his leg.",
                                        752, List.of("Classic Literature", "Adventure"), "https://covers.openlibrary.org/b/isbn/9780142437247-L.jpg", "en", 4.2, "9780142437247", 1851, false, 4, 4),

                                book("The Picture of Dorian Gray", List.of("Oscar Wilde"), "Ward, Lock and Company",
                                        "A handsome young man remains forever youthful while his portrait ages and reveals his sins.",
                                        254, List.of("Classic Literature", "Gothic"), "https://covers.openlibrary.org/b/isbn/9780141439570-L.jpg", "en", 4.4, "9780141439570", 1890, false, 5, 5),

                                book("The Phantom Tollbooth", List.of("Norton Juster"), "Epstein & Carroll",
                                        "A bored boy drives through a magic tollbooth into a land where words and numbers are at war.",
                                        256, List.of("Children's Literature", "Fantasy"), "https://covers.openlibrary.org/b/isbn/9780394820378-L.jpg", "en", 4.5, "9780394820378", 1961, false, 4, 4),

                                book("Where the Wild Things Are", List.of("Maurice Sendak"), "Harper & Row",
                                        "A mischievous boy is sent to bed without supper and sails to the land of Wild Things.",
                                        48, List.of("Children's Literature", "Picture Book"), "https://covers.openlibrary.org/b/isbn/9780060254926-L.jpg", "en", 4.6, "9780060254926", 1963, false, 7, 7),

                                book("The Wind in the Willows", List.of("Kenneth Grahame"), "Methuen",
                                        "The adventures of Mole, Rat, Badger, and Toad along the river bank in the English countryside.",
                                        288, List.of("Children's Literature", "Classic Literature"), "https://covers.openlibrary.org/b/isbn/9780141321134-L.jpg", "en", 4.4, "9780141321134", 1908, false, 6, 6)
                        );

                        bookRepository.saveAll(books);
                        logger.info("Database seeded with {} books!", books.size());
                } else {
                        logger.info("Database already contains data; seeding skipped.");
                }
        }
}