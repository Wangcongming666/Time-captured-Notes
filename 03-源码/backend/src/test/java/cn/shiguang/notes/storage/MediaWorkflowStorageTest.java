package cn.shiguang.notes.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.exception.BizException;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.repository.query.PublicQueryRepository;
import cn.shiguang.notes.service.ContentService;
import cn.shiguang.notes.service.MediaService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;

class MediaWorkflowStorageTest {
  @TempDir Path root;

  MockMultipartFile image() throws Exception {
    var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var bytes = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, "png", bytes);
    return new MockMultipartFile("file", "image.png", "image/png", bytes.toByteArray());
  }

  @Test
  void unknownCommitOutcomeKeepsThePossiblyCommittedObject() throws Exception {
    var content = mock(ContentService.class);
    when(content.addMedia(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyLong(),
            anyInt(),
            anyInt(),
            anyString()))
        .thenReturn(Map.of("id", "1"));
    var use =
        new MediaService(
            new LocalMediaStore(root.toString()), content, mock(PublicQueryRepository.class));
    TransactionSynchronizationManager.initSynchronization();
    try {
      use.upload(new Actor(1, Set.of("USER")), image(), "NOTE");
      for (var callback : TransactionSynchronizationManager.getSynchronizations())
        callback.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
      try (var files = Files.list(root)) {
        assertEquals(1, files.count());
      }
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void rollbackDeletesObjectAfterSuccessfulUpload() throws Exception {
    var content = mock(ContentService.class);
    when(content.addMedia(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyLong(),
            anyInt(),
            anyInt(),
            anyString()))
        .thenReturn(Map.of("id", "1"));
    var use =
        new MediaService(
            new LocalMediaStore(root.toString()), content, mock(PublicQueryRepository.class));
    TransactionSynchronizationManager.initSynchronization();
    try {
      use.upload(new Actor(1, Set.of("USER")), image(), "NOTE");
      try (var files = Files.list(root)) {
        assertEquals(1, files.count());
      }
      for (var callback : TransactionSynchronizationManager.getSynchronizations())
        callback.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
      try (var files = Files.list(root)) {
        assertEquals(0, files.count());
      }
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void metadataFailureCleansObjectWithoutMaskingOriginalError() throws Exception {
    var content = mock(ContentService.class);
    when(content.addMedia(
            any(),
            anyString(),
            anyString(),
            anyString(),
            anyLong(),
            anyInt(),
            anyInt(),
            anyString()))
        .thenThrow(BizException.conflict("metadata conflict"));
    var use =
        new MediaService(
            new LocalMediaStore(root.toString()), content, mock(PublicQueryRepository.class));
    TransactionSynchronizationManager.initSynchronization();
    try {
      assertEquals(
          409,
          assertThrows(
                  BizException.class,
                  () -> use.upload(new Actor(1, Set.of("USER")), image(), "NOTE"))
              .status());
      try (var files = Files.list(root)) {
        assertEquals(0, files.count());
      }
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  void unauthorizedReadsNeverContactTheObjectStore() throws Exception {
    var store = mock(MediaStore.class);
    var content = mock(ContentService.class);
    when(content.media(1)).thenReturn(Map.of("storageKey", "private.png"));
    var use = new MediaService(store, content, mock(PublicQueryRepository.class));
    assertEquals(404, assertThrows(BizException.class, () -> use.read(Actor.guest(), 1)).status());
    verifyNoInteractions(store);
  }
}
