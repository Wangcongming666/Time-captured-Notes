import { createRouter, createWebHistory } from "vue-router";
export const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: (_to, _from, saved) => saved ?? { top: 0 },
  routes: [
    {
      path: "/",
      component: () => import("./features/discovery/DiscoverPage.vue"),
    },
    {
      path: "/following",
      component: () => import("./features/discovery/DiscoverPage.vue"),
    },
    {
      path: "/search",
      component: () => import("./features/discovery/DiscoverPage.vue"),
    },
    {
      path: "/notes/:id",
      component: () => import("./features/content/NotePage.vue"),
    },
    {
      path: "/login",
      component: () => import("./features/identity/LoginPage.vue"),
    },
    {
      path: "/register",
      component: () => import("./features/identity/LoginPage.vue"),
    },
    {
      path: "/me",
      component: () => import("./features/identity/ProfilePage.vue"),
    },
    {
      path: "/users/:id",
      component: () => import("./features/identity/ProfilePage.vue"),
    },
    {
      path: "/creator",
      component: () => import("./features/content/CreatorPage.vue"),
    },
    {
      path: "/editor/:id?",
      component: () => import("./features/content/EditorPage.vue"),
    },
    {
      path: "/collections",
      component: () => import("./features/collection/CollectionsPage.vue"),
    },
    {
      path: "/practices/:id",
      component: () => import("./features/collection/PracticePage.vue"),
    },
    {
      path: "/notifications",
      component: () => import("./features/notification/NotificationsPage.vue"),
    },
    {
      path: "/preferences",
      component: () => import("./features/discovery/PreferencesPage.vue"),
    },
    {
      path: "/dashboard",
      component: () => import("./features/statistics/DashboardPage.vue"),
    },
    {
      path: "/admin/:section",
      component: () => import("./features/governance/AdminPage.vue"),
    },
    {
      path: "/:pathMatch(.*)*",
      component: () => import("./features/NotFoundPage.vue"),
    },
  ],
});
