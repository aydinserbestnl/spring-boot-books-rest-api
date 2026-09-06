package com.luv2code.books.controller;

import com.luv2code.books.entity.Book;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final List<Book> books = new ArrayList<>();

    // maakt de controller aan en vult de boekenlijst
    public BookController() {
        initializeBooks();
    }

    // vult de lijst met wat standaard boeken
    private void initializeBooks() {
        books.addAll(List.of(
                new Book("The Hobbit", "", "Fantasy"),
                new Book("The Lord of the Rings", "", "Fantasy"),
                new Book("The Hunger Games", "", "Fantasy"),
                new Book("The Da Vinci Code", "", "Fantasy"),
                new Book("The Alchemist", "", "Fantasy"),
                new Book("The Lord of the Rings: The Fellowship of the Ring", "", "Fantasy")
        ));

    }

    // geeft alle boeken terug, optioneel gefilterd op categorie
    @GetMapping
    public List<Book> getBooks(@RequestParam(required = false) String category) {
        if (category != null) {
            return books.stream()
                    .filter(book -> book.getCategory().equalsIgnoreCase(category))
                    .toList();
        }
        return books;
    }
/*

2 query param:

@GetMapping("/api/books")
  public List<Book> getBooks(@RequestParam(required = false) String category,
                              @RequestParam(required = false) String title) {
      return books.stream()
              .filter(b -> category == null || Objects.equals(b.getCategory(), category))
              .filter(b -> title == null || Objects.equals(b.getTitle(), title))
              .toList();
  }

 */
    // zoekt een boek op titel
    @GetMapping("/{title}")
    public Book getBookByTitle(@PathVariable String title) {
        return books.stream()
                .filter(book -> book.getTitle().equals(title))
                .findFirst()
                .orElse(null);

    /*
    @GetMapping("/api/books/{title}") daki path varaible'in sonda olmasina gerek yok
    arada da olabilir
    or:
    @GetMapping("/api/books/{title}/findbook")
     */
}
    // voegt een nieuw boek toe als de titel nog niet bestaat
    @PostMapping
    public  void createBook(@RequestBody Book newBook) {
        boolean isNewBook = books.stream()
                .noneMatch(book -> book.getTitle().equalsIgnoreCase(newBook.getTitle()));
        if (isNewBook) {
            books.add(newBook);
        } else {
            throw new IllegalArgumentException("Book with title '" + newBook.getTitle() + "' already exists.");
        }

    }
    // werkt een bestaand boek bij aan de hand van de titel
    @PutMapping("/{title}")
    public void updateBook(@PathVariable String title, @RequestBody Book updatedBook) {
        books.stream()
                .filter(book -> book.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .ifPresent(book -> {
                    book.setTitle(updatedBook.getTitle());
                    book.setAuthor(updatedBook.getAuthor());
                    book.setCategory(updatedBook.getCategory());
                });
    }
    // verwijdert een boek op basis van de titel
    @DeleteMapping("/{title}")
    public void deleteBook(@PathVariable String title) {
        books.removeIf(book -> book.getTitle().equalsIgnoreCase(title));
    }
}
