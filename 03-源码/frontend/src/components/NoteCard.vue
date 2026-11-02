<script setup lang="ts">
import type { Card } from "../api/types";
import { mediaUrl, posterUrl } from "../api/client";
import AppIcon from "./AppIcon.vue";
defineProps<{ note: Card }>();
const labels: Record<string, string> = {
  INTEREST: "与你的兴趣相关",
  FOLLOWING: "来自你关注的人",
  POPULAR: "近期热门",
  LATEST: "最新分享",
};
</script>
<template>
  <article class="note-card">
    <RouterLink :to="'/notes/' + note.id" class="card-picture"
      ><img
        :src="
          note.coverMediaType === 'VIDEO'
            ? posterUrl(note.coverMediaId)
            : mediaUrl(note.coverMediaId)
        "
        :alt="note.title"
        loading="lazy"
      /><span
        v-if="note.coverMediaType === 'VIDEO'"
        class="video-badge"
        aria-label="视频笔记"
        >▶</span
      ><span
        v-if="note.reason !== 'LATEST' && labels[note.reason]"
        class="card-reason"
        >{{ labels[note.reason] }}</span
      ></RouterLink
    >
    <div class="card-copy">
      <RouterLink :to="'/notes/' + note.id" class="card-title">{{
        note.title
      }}</RouterLink>
      <div class="card-meta">
        <RouterLink :to="'/users/' + note.authorId" class="card-author"
          ><img
            v-if="note.authorAvatarMediaId"
            class="avatar tiny"
            :src="mediaUrl(note.authorAvatarMediaId)"
            alt=""
            loading="lazy"
          /><span v-else class="avatar tiny">{{
            note.authorName?.slice(0, 1) || "拾"
          }}</span
          >{{ note.authorName }}</RouterLink
        ><span class="card-likes"
          ><AppIcon name="follow" :size="15" />{{ note.likeCount }}</span
        >
      </div>
    </div>
  </article>
</template>
