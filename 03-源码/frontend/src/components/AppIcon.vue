<script setup lang="ts">
defineProps<{ name: string; size?: number }>()
const paths: Record<string, string> = {
  discover: 'M3 10 12 3l9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1Z',
  follow: 'M12 21s-9-5.5-9-12a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 6.5-9 12-9 12Z',
  create: 'M12 4v16M4 12h16',
  collection: 'M6 3h12a1 1 0 0 1 1 1v17l-7-4-7 4V4a1 1 0 0 1 1-1Z',
  bell: 'M18 8a6 6 0 0 0-12 0c0 7-3 8-3 8h18s-3-1-3-8M10 21h4',
  user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0ZM4 21v-2a8 8 0 0 1 16 0v2',
  search: 'M16 10a6 6 0 1 1-12 0 6 6 0 0 1 12 0Zm-1 5 6 6',
  chart: 'M4 4v16h16M8 15v-4M12 15V7M16 15v-6',
  settings: 'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8ZM3 12h2m14 0h2M12 3v2m0 14v2M6 6l1 1m10 10 1 1M6 18l1-1M17 7l1-1',
  check: 'm5 12 4 4L20 5',
  close: 'm5 5 14 14M19 5 5 19',
  arrow: 'M5 12h14m-5-5 5 5-5 5',
  comment: 'M21 11a9 9 0 0 1-9 9H5l-3 2V11a9 9 0 0 1 19 0Z',
  more: 'M5 12h.1M12 12h.1M19 12h.1',
}
</script>
<template><svg :width="size ?? 22" :height="size ?? 22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] ?? paths.discover" /></svg></template>
