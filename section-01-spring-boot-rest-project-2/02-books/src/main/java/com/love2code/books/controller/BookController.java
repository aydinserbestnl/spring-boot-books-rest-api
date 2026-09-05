package com.love2code.books.controller;

import com.love2code.books.entity.Book;
import com.love2code.books.exception.BookErrorResponse;
import com.love2code.books.exception.BookNotFoundException;
import com.love2code.books.request.BookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Tag(name = "Books REST API endpoints", description = "API voor boeken")
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
                new Book(1, "The Hobbit", "J.R.R. Tolkien", "Fantasy", 5),
                new Book(2, "1984", "George Orwell", "Dystopian", 5),
                new Book(3, "To Kill a Mockingbird", "Harper Lee", "Classic", 4),
                new Book(4, "The Great Gatsby", "F. Scott Fitzgerald", "Classic", 4),
                new Book(5, "Dune", "Frank Herbert", "Science Fiction", 5),
                new Book(6, "The Da Vinci Code", "Dan Brown", "Thriller", 3),
                new Book(7, "Clean Code", "Robert C. Martin", "Programming", 5)
        ));

    }

    // geeft alle boeken terug, optioneel gefilterd op categorie
    @Operation(summary = "Get all books", description = "Returns a list of all books, optionally filtered by category")
    @GetMapping
    public List<Book> getBooks(@Parameter(description = "optional query parameter")
                                   @RequestParam(required = false) String category) {
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
    // zoekt een boek op id
    @Operation(summary = "Get book by ID", description = "Returns a book by its ID")
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@Parameter(description = "ID of the book to retrieve") @PathVariable @Min(1) long id) {

        Book book = books.stream()
                .filter(b -> b.getId() == id)
                .findFirst()
                .orElseThrow(() -> new BookNotFoundException("Book not found with id: " + id));
        return ResponseEntity.ok(book);

    /*
    @GetMapping("/api/books/{title}") daki path varaible'in sonda olmasina gerek yok
    arada da olabilir
    or:
    @GetMapping("/api/books/{title}/findbook")
     */
    }

    // voegt een nieuw boek toe als de titel nog niet bestaat
    @Operation(summary = "Create a new book", description = "Creates a new book")
    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody BookRequest bookRequest) {
        long id = books.isEmpty() ? 1 : books.stream()
                .mapToLong(Book::getId)
                .max()
                .orElse(0) + 1;

        Book newBook = convertToBook(id, bookRequest);
        books.add(newBook);

        URI location = URI.create("/api/books/" + newBook.getId());
        return ResponseEntity.created(location).body(newBook);
    }
    // werkt een bestaand boek bij aan de hand van de id
    @Operation(summary = "Update an existing book", description = "Updates an existing book by its ID")
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@Parameter(description = "ID of the book to update") @PathVariable long id, @Valid @RequestBody BookRequest updatedBook) {
        return books.stream()
                .filter(book -> book.getId() == id)
                .findFirst()
                .map(book -> {
                    book.setTitle(updatedBook.getTitle());
                    book.setAuthor(updatedBook.getAuthor());
                    book.setCategory(updatedBook.getCategory());
                    book.setRating(updatedBook.getRating());
                    return ResponseEntity.ok(book);
                })
                .orElse(ResponseEntity.notFound().build());
    }
    // verwijdert een boek op basis van de id
    @Operation(summary = "Delete a book", description = "Deletes a book by its ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@Parameter(description = "ID of the book to delete") @PathVariable @Min(1) long id) {
        boolean removed = books.removeIf(book -> book.getId() == id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
    private Book convertToBook(long id, BookRequest bookRequest) {
        return new Book(id, bookRequest.getTitle(), bookRequest.getAuthor(),
                bookRequest.getCategory(), bookRequest.getRating());
    }

    @ExceptionHandler
    public ResponseEntity<BookErrorResponse> handleBookNotFoundException(BookNotFoundException ex) {
        BookErrorResponse errorResponse = new BookErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                System.currentTimeMillis()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        }
}
