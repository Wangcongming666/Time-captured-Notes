<script setup lang="ts">defineProps<{ message: string; retry?: () => void }>()</script>
<template><div v-if="message" class="error-banner" role="alert"><span>{{ message }}</span><button v-if="retry" class="text-button" @click="retry">重试</button></div></template>
