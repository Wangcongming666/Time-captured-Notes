package cn.shiguang.notes.entity;

import com.baomidou.mybatisplus.annotation.*;

/** Persistent row of t_note_topic; never used as an HTTP response. */
@TableName("t_note_topic")
public class NoteTopicEntity extends BaseEntity {
  @TableField("note_id")
  private Long noteId;

  public Long getNoteId() {
    return noteId;
  }

  public void setNoteId(Long value) {
    noteId = value;
  }

  @TableField("topic_id")
  private Long topicId;

  public Long getTopicId() {
    return topicId;
  }

  public void setTopicId(Long value) {
    topicId = value;
  }
}
