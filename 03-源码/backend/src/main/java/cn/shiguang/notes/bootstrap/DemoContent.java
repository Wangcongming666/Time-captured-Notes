package cn.shiguang.notes.bootstrap;

import cn.shiguang.notes.common.contract.EventPort;
import cn.shiguang.notes.common.contract.MediaStore;
import cn.shiguang.notes.common.util.Data;
import cn.shiguang.notes.dto.Actor;
import cn.shiguang.notes.entity.*;
import cn.shiguang.notes.mapper.*;
import cn.shiguang.notes.repository.support.MapperRows;
import cn.shiguang.notes.service.CollectionService;
import cn.shiguang.notes.service.ContentService;
import cn.shiguang.notes.service.GovernanceService;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opt-in, prominently labelled synthetic content. Uses the same lifecycle rules as ordinary notes.
 */
@Component
@org.springframework.core.annotation.Order(20)
public class DemoContent implements ApplicationRunner {
  private final boolean enabled;
  private final MediaStore store;
  private final NoteMapper noteMapper;
  private final TopicMapper topicMapper;
  private final UserMapper userMapper;
  private final ContentService content;
  private final GovernanceService governance;
  private final CollectionService collections;
  private final EventPort events;

  public DemoContent(
      @Value("${app.seed-demo:false}") boolean enabled,
      MediaStore store,
      NoteMapper noteMapper,
      TopicMapper topicMapper,
      UserMapper userMapper,
      ContentService content,
      GovernanceService governance,
      CollectionService collections,
      EventPort events) {
    this.enabled = enabled;
    this.store = store;
    this.noteMapper = noteMapper;
    this.topicMapper = topicMapper;
    this.userMapper = userMapper;
    this.content = content;
    this.governance = governance;
    this.collections = collections;
    this.events = events;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) throws Exception {
    if (!enabled || noteMapper.demoContentRun() > 0) return;
    long owner = Data.id(MapperRows.one(userMapper.demoContentRun()), "id"),
        reviewerId = Data.id(MapperRows.one(userMapper.demoContentRun2()), "id");
    var actor = new Actor(owner, Set.of("USER"));
    var reviewer = new Actor(reviewerId, Set.of("USER", "REVIEWER"));
    var topics = MapperRows.list(topicMapper.demoContentRun());
    String[] titles = {
      "演示｜把周末，留给一杯好咖啡",
      "演示｜去森林里，慢慢走一会儿",
      "演示｜把远方的湖光写进日常",
      "演示｜整理书桌，也整理心情",
      "演示｜在山野里，找回自己的节奏",
      "演示｜让家的一个角落安静下来",
      "演示｜从手冲咖啡开始的小仪式",
      "演示｜轻装出发的周末徒步清单",
      "演示｜旅行里的留白时刻",
      "演示｜一本书与一个不被打扰的下午",
      "演示｜为下一次远行，认真做计划",
      "演示｜收藏之后，把喜欢变成实践"
    };
    for (int i = 0; i < titles.length; i++) {
      var resource = new ClassPathResource("demo/photo-" + (i % 6 + 1) + ".jpg");
      byte[] bytes;
      try (var in = resource.getInputStream()) {
        bytes = in.readAllBytes();
      }
      BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
      String key = "demo-" + (i + 1) + ".jpg";
      store.put(key, bytes, "image/jpeg");
      var media =
          content.addMedia(
              actor,
              key,
              "NOTE",
              "image/jpeg",
              bytes.length,
              image.getWidth(),
              image.getHeight(),
              HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
      var note =
          content.save(
              actor,
              null,
              Data.map(
                  "title",
                  titles[i],
                  "body",
                  "【合成演示内容】本篇用于课程功能验收，不代表真实用户经历。\n\n"
                      + "生活里细微的美好，值得被认真记录。这里是一段图文分享的样例：你可以点赞、评论、关注作者，或者把它放进自己的收藏夹。\n\n"
                      + "喜欢不必停留在收藏。创建一次实践计划，记录开始、完成与自己的体验；私人反馈默认不会公开。图片来源与许可见项目素材声明。",
                  "mediaIds",
                  List.of(media.get("id")),
                  "topicIds",
                  List.of(topics.get(i % topics.size()).get("id"))));
      long noteId = Data.id(note, "id");
      note = content.submit(actor, noteId, Data.number(note, "version"));
      int revision = Data.number(note, "revision");
      long review = governance.openReview("NOTE", noteId, revision, owner);
      governance.decide(reviewer, review, 0, "APPROVE", "合成样例初始化");
      content.moderate(noteId, revision, "APPROVE", "合成样例初始化");
      governance.audit(
          reviewer,
          "DEMO_REVIEW",
          "NOTE",
          noteId,
          Map.of("status", "PENDING"),
          Map.of("status", "PUBLISHED"),
          "明确标注的合成演示内容");
      events.append(
          "ReviewDecided",
          reviewerId,
          "NOTE",
          noteId,
          revision,
          Data.map(
              "noteId",
              Long.toString(noteId),
              "authorId",
              Long.toString(owner),
              "decision",
              "APPROVE"),
          "statistics");
    }
    collections.createFolder(actor, "周末想尝试");
    collections.createFolder(actor, "旅行与灵感");
  }
}
