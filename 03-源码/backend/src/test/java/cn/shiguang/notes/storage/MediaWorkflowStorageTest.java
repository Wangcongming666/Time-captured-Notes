package cn.shiguang.notes.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.shiguang.notes.modules.content.api.ContentApi;
import cn.shiguang.notes.platform.readmodel.PublicReadModel;
import cn.shiguang.notes.platform.storage.LocalMediaStore;
import cn.shiguang.notes.shared.contract.*;
import cn.shiguang.notes.shared.error.BizException;
import cn.shiguang.notes.workflows.content.MediaUseCases;
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
    var content = mock(ContentApi.class);
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
        new MediaUseCases(
            new LocalMediaStore(root.toString()), content, mock(PublicReadModel.class));
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
    var content = mock(ContentApi.class);
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
        new MediaUseCases(
            new LocalMediaStore(root.toString()), content, mock(PublicReadModel.class));
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
    var content = mock(ContentApi.class);
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
        new MediaUseCases(
            new LocalMediaStore(root.toString()), content, mock(PublicReadModel.class));
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
    var content = mock(ContentApi.class);
    when(content.media(1)).thenReturn(Map.of("storageKey", "private.png"));
    var use = new MediaUseCases(store, content, mock(PublicReadModel.class));
    assertEquals(404, assertThrows(BizException.class, () -> use.read(Actor.guest(), 1)).status());
    verifyNoInteractions(store);
  }
}
