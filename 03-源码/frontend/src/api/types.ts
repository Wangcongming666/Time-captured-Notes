export interface Page<T> {
  items: T[];
  nextCursor: string | null;
  hasMore: boolean;
}
export interface User {
  id: string;
  nickname: string;
  bio: string;
  avatarMediaId: string | null;
  roles?: string[];
  status?: string;
  username?: string;
}
export interface Topic {
  id: string;
  name: string;
  category: string;
  status: string;
}
export interface Card {
  coverMediaType?: "IMAGE" | "VIDEO";
  durationMs?: number | null;
  authorAvatarMediaId?: string | null;
  id: string;
  authorId: string;
  authorName: string;
  title: string;
  coverMediaId: string;
  reason: string;
  score: number;
  likeCount: number;
  favoriteCount: number;
}
export interface Note {
  mediaEntries?: MediaEntry[];
  id: string;
  authorId: string;
  title: string;
  body: string;
  mediaIds: string[];
  topicIds: string[];
  publishedAt: string | null;
  status?: string;
  version?: number;
  revision?: number;
  reason?: string | null;
  liked?: boolean;
  following?: boolean;
  favorite?: boolean;
  likeCount?: number;
  favoriteCount?: number;
  commentCount?: number;
}
export interface MediaEntry {
  id: string;
  mediaType: "IMAGE" | "VIDEO";
  mime: string;
  width: number;
  height: number;
  durationMs?: number | null;
}
export interface Version {
  id: string;
  version: number;
  status: string;
}
export interface Comment {
  id: string;
  authorId: string;
  noteId: string;
  rootId: string | null;
  replyToId: string | null;
  body: string;
  status: string;
  createdAt: string;
}
export interface Folder {
  id: string;
  name: string;
  version: number;
}
export interface Favorite {
  id: string;
  noteId: string;
  available: boolean;
  title: string | null;
}
export interface Practice {
  id: string;
  noteId: string;
  status: string;
  version: number;
  publicStatus: string;
  resultText: string | null;
  helpfulness: string | null;
  sourceAvailable: boolean;
  mediaIds: string[];
}
export interface History {
  id: string;
  fromStatus: string | null;
  toStatus: string;
  createdAt: string;
}
export interface Feedback {
  id: string;
  noteId: string;
  authorId: string;
  resultText: string;
  helpfulness: string;
  mediaIds: string[];
}
export interface Review {
  id: string;
  targetType: string;
  targetId: string;
  targetRevision: number;
  version: number;
  status: string;
  applicantId: string;
}
export interface ReviewDetail {
  review: Review;
  note: Note | null;
  practice: Practice | null;
}
export interface Report {
  id: string;
  targetType: string;
  targetId: string;
  status: string;
  version: number;
  resultReason: string | null;
  reason?: string;
}
export interface Notice {
  id: string;
  type: string;
  actorId: string | null;
  targetType: string;
  targetId: string;
  targetAvailable: boolean;
  readAt: string | null;
  createdAt: string;
}
export interface Metric {
  day: string;
  metric: string;
  value: number;
}
export interface Dashboard {
  topicRanking?: Rank[];
  points: Metric[];
  calculatedAt: string;
  timezone: string;
}
export interface Rank {
  subjectId: string;
  label: string;
  metric: string;
  value: number;
}
