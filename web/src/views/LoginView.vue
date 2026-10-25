<template>
  <div class="atp-login">
    <div class="atp-login-card">
      <div class="atp-login-brand">
        <img class="atp-login-logo" src="/brand-logo.svg" alt="智能工单 Agent 平台" />
        <div>
          <div class="atp-login-title">智能工单 Agent 平台</div>
          <div class="atp-login-sub">Agent Ticket Platform · 智能工单与审批</div>
        </div>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" clearable>
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>

        <el-button type="primary" size="large" class="atp-login-btn" :loading="loading" @click="submit">
          登 录
        </el-button>
      </el-form>

      <div class="atp-login-tip">
        <div class="atp-login-tip-title">演示账号（密码都是 123456）</div>
        <div class="atp-login-accounts">
          <span
            v-for="acc in accounts"
            :key="acc.username"
            class="atp-account"
            @click="fill(acc.username)"
          >{{ acc.username }}<i>{{ acc.label }}</i></span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 五个演示账号，权限差别很明显，方便一进来就看出 RBAC 的效果
const accounts = [
  { username: 'admin', label: '管理员 13 权限' },
  { username: 'zhangsan', label: '主管 可审批' },
  { username: 'lisi', label: '客服' },
  { username: 'wangwu', label: '客服' },
  { username: 'zhaoliu', label: '提交人' }
]

const fill = (username) => {
  form.username = username
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.doLogin({ ...form })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/chat')
  } catch (e) {
    // 提示已经由 axios 拦截器弹过
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.atp-login {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #333333 0%, #4a4a4a 55%, #5a4536 100%);
}

.atp-login-card {
  width: 380px;
  background: #ffffff;
  border-radius: 10px;
  padding: 28px 28px 22px;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.28);
}

.atp-login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 22px;
}

.atp-login-logo {
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffb066 0%, #e8833a 100%);
  color: #ffffff;
  font-weight: 700;
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.atp-login-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--atp-text);
  line-height: 22px;
}

.atp-login-sub {
  font-size: 11.5px;
  color: var(--atp-text-mute);
  line-height: 16px;
}

.atp-login-btn {
  width: 100%;
  margin-top: 4px;
}

.atp-login-tip {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed var(--atp-border);
}

.atp-login-tip-title {
  font-size: 12px;
  color: var(--atp-text-mute);
  margin-bottom: 8px;
}

.atp-login-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.atp-account {
  cursor: pointer;
  font-size: 12px;
  padding: 3px 8px;
  border-radius: 4px;
  background: var(--atp-primary-soft);
  color: var(--atp-primary-dark);
  transition: background 0.15s;
}

.atp-account:hover {
  background: #f8dcc4;
}

.atp-account i {
  font-style: normal;
  color: var(--atp-text-mute);
  margin-left: 4px;
  font-size: 11px;
}

/* 登录页品牌图标：保持图标原始比例，使用圆角而非裁成圆形 */
.atp-login-logo {
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  display: block;
  object-fit: contain;
  border-radius: 14px;
  background: transparent;
  box-shadow: 0 10px 24px rgba(48, 36, 27, 0.22), 0 0 0 1px rgba(255, 255, 255, 0.14);
}
</style>



