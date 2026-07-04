<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', 'PingFang SC', 'Helvetica Neue', Arial, sans-serif;
  color: #2c3e50;
  /* 全局轻极光渐变底色，保持整体明亮，同时为时光轴和毛玻璃提供通透感 */
  background-color: #f7f9fc;
  background-image: 
    radial-gradient(at 0% 0%, rgba(161, 196, 253, 0.25) 0px, transparent 50%),
    radial-gradient(at 100% 0%, rgba(194, 233, 251, 0.3) 0px, transparent 50%),
    radial-gradient(at 100% 100%, rgba(253, 235, 113, 0.15) 0px, transparent 50%);
  background-attachment: fixed;
  background-size: cover;
  min-height: 100vh;
}

a {
  text-decoration: none;
  color: inherit;
}

/* 优雅的全局轻量滚动条 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 4px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
</style>