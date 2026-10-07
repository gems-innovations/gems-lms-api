package com.gems.education.infrastructure.driving.rest;

import com.gems.education.infrastructure.driven.files.LocalFileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileControllerTest {

  @Test
  void fileNamesLoseTheirPathAndControlCharacters() {
    assertEquals("informe.pdf", FileController.safeName("C:\\Users\\ana\\informe.pdf"));
    assertEquals("passwd", FileController.safeName("../../etc/passwd"));
    assertEquals("tarea final.docx", FileController.safeName("tarea \"final\".docx".replace("\"", "")));
    assertEquals("archivo", FileController.safeName("  "));
    assertEquals("archivo", FileController.safeName(null));
  }

  @Test
  void publicFilesAreServedFromTheirOwnPath() {
    assertEquals("/api/v1/files/public/abc", FileController.url("abc", FileController.PUBLIC));
    assertEquals("/api/v1/files/abc", FileController.url("abc", FileController.PRIVATE));
  }

  @Test
  void storageKeysCannotEscapeTheUploadsDirectory(@TempDir Path dir) {
    LocalFileStorage storage = new LocalFileStorage(dir.toString());

    assertThrows(IllegalArgumentException.class, () -> storage.read("../secret").block());
    StepVerifier.create(storage.read("00000000-0000-0000-0000-000000000000")).verifyComplete();
  }
}
