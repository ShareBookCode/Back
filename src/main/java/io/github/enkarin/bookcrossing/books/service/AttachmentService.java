package io.github.enkarin.bookcrossing.books.service;

import io.github.enkarin.bookcrossing.books.dto.AttachmentMultipartDto;
import io.github.enkarin.bookcrossing.books.dto.BookModelDto;
import io.github.enkarin.bookcrossing.books.exceptions.NoAccessToAttachmentException;
import io.github.enkarin.bookcrossing.books.exceptions.UnsupportedFormatException;
import io.github.enkarin.bookcrossing.books.model.Attachment;
import io.github.enkarin.bookcrossing.books.model.Book;
import io.github.enkarin.bookcrossing.books.repository.AttachmentRepository;
import io.github.enkarin.bookcrossing.books.repository.BookRepository;
import io.github.enkarin.bookcrossing.exception.AttachmentNotFoundException;
import io.github.enkarin.bookcrossing.exception.BadRequestException;
import io.github.enkarin.bookcrossing.exception.BookNotFoundException;
import io.github.enkarin.bookcrossing.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

@RequiredArgsConstructor
@Service
@Transactional
public class AttachmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentService.class);
    private final UserRepository userRepository;
    private final AttachmentRepository attachRepository;
    private final BookRepository bookRepository;

    public byte[] findAttachmentData(final int id, final String imageFormat) {
        final Attachment attachment = attachRepository.findById(id).orElseThrow(AttachmentNotFoundException::new);
        return switch (imageFormat) {
            case "origin" -> attachment.getOriginalImage();
            case "list" -> attachment.getListImage();
            case "thumb" -> attachment.getThumbImage();
            default -> throw new UnsupportedFormatException();
        };
    }

    public BookModelDto saveAttachment(final AttachmentMultipartDto attachmentMultipartDto, final String login) throws IOException {
        final Book book = userRepository.findByLogin(login).orElseThrow().getBooks().stream()
            .filter(b -> b.getBookId() == attachmentMultipartDto.getBookId())
            .findFirst()
            .orElseThrow(BookNotFoundException::new);
        final String fileName = attachmentMultipartDto.getFile().getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("Имя не должно быть пустым");
        } else {
            final String expansion = fileName.substring(fileName.indexOf('.') + 1).toLowerCase(Locale.ROOT);
            if ("jpeg".equals(expansion) || "jpg".equals(expansion) || "png".equals(expansion) || "bmp".equals(expansion)) {
                final Attachment attachment = new Attachment();
                attachment.setOriginalImage(attachmentMultipartDto.getFile().getBytes());
                final BufferedImage image = ImageIO.read(attachmentMultipartDto.getFile().getInputStream());
                attachment.setListImage(compressImage(image, expansion, 200, 300));
                attachment.setThumbImage(compressImage(image, expansion, 70, 70));
                attachment.setBook(book);
                attachment.setExpansion(expansion);
                book.setAttachment(attachment);
                attachRepository.save(attachment);
                return BookModelDto.fromBook(bookRepository.getReferenceById(book.getBookId()));
            } else {
                throw new BadRequestException("Недопустимый формат файла");
            }
        }
    }

    private byte[] compressImage(final BufferedImage imageData, final String expansion, final int height, final int weight) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(byteArrayOutputStream)) {
                final ImageWriter writer = ImageIO.getImageWritersByFormatName(expansion).next();
                writer.setOutput(imageOutputStream);
                final BufferedImage outputImage = Scalr.resize(imageData, weight, height);
                writer.write(null, new IIOImage(outputImage, null, null), null);
                final byte[] result = byteArrayOutputStream.toByteArray();
                writer.dispose();
                return result;
            }
        } catch (IOException exception) {
            LOGGER.error(exception.getMessage(), exception);
            throw new BadRequestException(exception.getMessage(), exception);
        }
    }

    public void deleteAttachment(final int id, final String login) {
        final Attachment attachment = attachRepository.findById(id).orElseThrow(AttachmentNotFoundException::new);
        if (attachment.getBook().getOwner().getLogin().equals(login)) {
            attachRepository.delete(attachment);
        } else {
            throw new NoAccessToAttachmentException();
        }
    }
}
