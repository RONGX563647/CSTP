<template>
  <div class="auth-layout">
    <div class="auth-background">
      <div class="auth-bg-pattern"></div>
    </div>
    <div class="auth-container">
      <div class="auth-card">
        <div class="auth-header">
          <div class="logo">
            <el-icon :size="32" color="#F59E0B"><ShoppingCart /></el-icon>
          </div>
          <h1 class="title">AiSale</h1>
          <p class="subtitle">AI 智能销售系统</p>
        </div>
        <div class="auth-body">
          <slot></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFFDF5 0%, #FEF3C7 100%);
  position: relative;
  overflow: hidden;
}

.auth-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.auth-bg-pattern {
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(245, 158, 11, 0.08) 0%, transparent 70%);
  border-radius: 50%;
  top: -200px;
  right: -200px;
  animation: float 6s ease-in-out infinite;
}

.auth-bg-pattern::before {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(253, 230, 138, 0.15) 0%, transparent 70%);
  border-radius: 50%;
  bottom: -100px;
  left: -100px;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  50% {
    transform: translate(-20px, 20px) scale(1.05);
  }
}

.auth-container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.auth-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(245, 158, 11, 0.15);
  padding: 40px;
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  border-radius: 16px;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.25);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.subtitle {
  font-size: 14px;
  color: #6B7280;
  font-weight: 400;
}

.auth-body {
  width: 100%;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .auth-card {
    padding: 32px 24px;
  }

  .title {
    font-size: 24px;
  }
}
</style>
