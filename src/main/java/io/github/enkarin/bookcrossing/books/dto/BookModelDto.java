package io.github.enkarin.bookcrossing.books.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.github.enkarin.bookcrossing.books.model.Book;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import javax.annotation.concurrent.Immutable;
import java.util.Optional;

@Immutable
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Полные данные книги")
public class BookModelDto extends BookDto {
    @Schema(description = "Идентификатор", example = "15")
    private final int bookId;

    @Schema(description = "Идентификатор вложения")
    private final Integer attachmentId;

    private BookModelDto(final BookDto bookDto, final int bookId, final AttachmentDto attachment) {
        super(bookDto.title, bookDto.author, bookDto.genre, bookDto.publishingHouse, bookDto.year, bookDto.city);
        this.bookId = bookId;
        this.attachmentId = Optional.ofNullable(attachment).map(AttachmentDto::getAttachId).orElse(null);
    }

    @JsonCreator
    private BookModelDto(final String title,
                         final String author,
                         final int genre,
                         final String publishingHouse,
                         final int year,
                         final int bookId,
                         final AttachmentDto attachment,
                         final String city) {
        super(title, author, genre, publishingHouse, year, city);
        this.bookId = bookId;
        this.attachmentId = Optional.ofNullable(attachment).map(AttachmentDto::getAttachId).orElse(null);
    }

    public static BookModelDto fromBook(final Book book) {
        return new BookModelDto(create(book.getTitle(),
            book.getAuthor(),
            book.getGenre().getId(),
            book.getPublishingHouse(),
            book.getYear(),
            book.getOwner().getCity()),
            book.getBookId(),
            AttachmentDto.fromAttachment(book.getAttachment(), "origin"));
    }
}
