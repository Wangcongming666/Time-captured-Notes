package cn.shiguang.notes.service.application;

import cn.shiguang.notes.converter.HttpBoundaryConverter;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.service.MediaService;
import cn.shiguang.notes.service.VideoService;
import cn.shiguang.notes.vo.*;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Media upload projections and authenticated streaming orchestration. */
@Service
public class MediaApplicationService {
  private final MediaService media;
  private final VideoService video;
  private final HttpBoundaryConverter views;

  public MediaApplicationService(
      MediaService media, VideoService video, HttpBoundaryConverter views) {
    this.media = media;
    this.video = video;
    this.views = views;
  }

  public ImageUploadVO upload(Actor actor, MultipartFile file, String purpose) throws IOException {
    return views.view(media.upload(actor, file, purpose), ImageUploadVO.class);
  }

  public VideoUploadVO uploadVideo(Actor actor, MultipartFile file) throws IOException {
    return views.view(video.upload(actor, file), VideoUploadVO.class);
  }

  public MediaCapabilitiesVO capabilities() {
    return views.view(video.capabilities(), MediaCapabilitiesVO.class);
  }

  public ResponseEntity<Resource> stream(
      Actor actor, long id, boolean poster, String range, String ifRange, boolean head)
      throws IOException {
    return media.stream(actor, id, poster, range, ifRange, head);
  }
}
