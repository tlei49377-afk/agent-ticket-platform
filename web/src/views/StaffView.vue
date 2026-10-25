<template>
  <div class="atp-page">
    <h2 class="atp-page-title">员工与角色</h2>
    <p class="atp-page-desc">
      账号和权限是一体的：给员工选角色，角色挂权限码。
      员工登录时后端把权限码一起发下来，前端就靠这些码控制菜单和按钮。
    </p>

    <el-tabs v-model="tab">
      <!-- ---------------- 员工 ---------------- -->
      <el-tab-pane label="员工" name="user">
        <div class="atp-toolbar">
          <el-input
            v-model="userQuery.keyword"
            placeholder="姓名 / 账号 / 手机号"
            style="width: 200px"
            clearable
            @keyup.enter="loadUsers"
            @clear="loadUsers"
          />
          <el-select v-model="userQuery.roleCode" placeholder="角色" style="width: 140px" clearable @change="loadUsers">
            <el-option v-for="r in roles" :key="r.code" :label="r.name" :value="r.code" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadUsers">查询</el-button>
          <el-button v-if="store.can('user:manage')" type="primary" plain :icon="Plus" style="margin-left: auto" @click="openUser()">
            新增员工
          </el-button>
        </div>

        <el-table v-loading="loadingUsers" :data="users" border stripe size="default">
          <el-table-column prop="username" label="账号" width="130">
            <template #default="{ row }"><span class="atp-mono">{{ row.username }}</span></template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="角色" min-width="180">
            <template #default="{ row }">
              <span v-for="rn in row.roleNames || []" :key="rn" class="atp-tag atp-role-tag">{{ rn }}</span>
              <span v-if="!(row.roleNames || []).length" class="atp-none">未分配</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <span class="atp-tag" :style="row.status === 1 ? onTag : offTag">{{ row.status === 1 ? '启用' : '停用' }}</span>
            </template>
          </el-table-column>
          <el-table-column v-if="store.can('user:manage')" label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="openUser(row)">编辑</el-button>
              <el-button link type="primary" size="small" @click="openPwd(row)">重置密码</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="atp-pager">
          <el-pagination
            v-model:current-page="userQuery.page"
            v-model:page-size="userQuery.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="userTotal"
            layout="total, sizes, prev, pager, next"
            @current-change="loadUsers"
            @size-change="loadUsers"
          />
        </div>
      </el-tab-pane>

      <!-- ---------------- 角色 ---------------- -->
      <el-tab-pane label="角色" name="role">
        <div class="atp-toolbar">
          <el-button v-if="store.can('user:manage')" type="primary" plain :icon="Plus" @click="openRole">
            新增角色
          </el-button>
          <el-button :icon="Refresh" @click="loadRoles">刷新</el-button>
          <span class="atp-toolbar-tip">共 {{ roles.length }} 个角色 / {{ allPermissions.length }} 个权限码</span>
        </div>

        <div class="atp-role-grid">
          <div v-for="r in roles" :key="r.id" class="atp-card atp-role-card">
            <div class="atp-role-head">
              <span class="atp-role-name">{{ r.name }}</span>
              <span class="atp-tag atp-role-code">{{ r.code }}</span>
            </div>
            <div class="atp-role-desc">{{ r.description || '（没有描述）' }}</div>
            <div class="atp-role-perms">
              <span v-for="p in permissionTagsOf(r.id)" :key="p.id" class="atp-tag atp-perm-tag">{{ p.name }}</span>
              <span v-if="!permissionTagsOf(r.id).length" class="atp-none">没有分配权限</span>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增 / 编辑员工 -->
    <el-dialog v-model="userVisible" :title="userForm.id ? '编辑员工' : '新增员工'" width="540px">
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="90px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="userForm.username" :disabled="!!userForm.id" placeholder="登录用的账号，创建后不可改" />
        </el-form-item>
        <el-form-item v-if="!userForm.id" label="登录密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="userForm.name" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="可选" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="可以多选" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name + '（' + r.code + '）'" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="userForm.id" label="状态">
          <el-switch v-model="userForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" title="重置密码" width="420px">
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-width="80px">
        <el-form-item label="账号">
          <el-input :model-value="pwdForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPwd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增角色 -->
    <el-dialog v-model="roleVisible" title="新增角色" width="620px">
      <el-form :model="roleForm" :rules="roleRules" ref="roleFormRef" label-width="90px">
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="roleForm.code" placeholder="英文小写，例如 quality" />
        </el-form-item>
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="例如 质检员" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" placeholder="这个角色负责什么" />
        </el-form-item>
        <el-form-item label="权限" prop="permissionIds">
          <el-checkbox-group v-model="roleForm.permissionIds" class="atp-perm-group">
            <el-checkbox v-for="p in allPermissions" :key="p.id" :label="p.id">
              {{ p.name }}<span class="atp-perm-code">{{ p.code }}</span>
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRole">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import {
  createRole,
  createUser,
  listRoles,
  listUsers,
  resetPassword,
  rolePermissions,
  updateUser
} from '../api/user'
import { allPermissions as fetchAllPermissions } from '../api/user'
import { useUserStore } from '../store/user'

const store = useUserStore()

const tab = ref('user')
const saving = ref(false)

const users = ref([])
const userTotal = ref(0)
const loadingUsers = ref(false)
const userQuery = reactive({ keyword: '', roleCode: '', page: 1, pageSize: 10 })

const roles = ref([])
const allPermissions = ref([])
// roleId -> 权限 id 数组，角色卡片上展开成中文标签用
const rolePermMap = ref({})

const userVisible = ref(false)
const userFormRef = ref()
const userForm = reactive({
  id: null, username: '', password: '', name: '', phone: '', email: '', roleIds: [], status: 1
})
const userRules = {
  username: [{ required: true, message: '请填写账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请填写登录密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请填写姓名', trigger: 'blur' }],
  roleIds: [{ required: true, type: 'array', min: 1, message: '至少选一个角色', trigger: 'change' }]
}

const pwdVisible = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ id: null, username: '', password: '' })
const pwdRules = {
  password: [
    { required: true, message: '请填写新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

const roleVisible = ref(false)
const roleFormRef = ref()
const roleForm = reactive({ code: '', name: '', description: '', permissionIds: [] })
const roleRules = {
  code: [{ required: true, message: '请填写角色编码', trigger: 'blur' }],
  name: [{ required: true, message: '请填写角色名称', trigger: 'blur' }],
  permissionIds: [{ required: true, type: 'array', min: 1, message: '至少勾一个权限', trigger: 'change' }]
}

const onTag = { background: '#e8f5ee', color: '#15803d' }
const offTag = { background: '#f3f4f6', color: '#78716c' }

const permissionTagsOf = (roleId) => {
  const ids = rolePermMap.value[roleId] || []
  return allPermissions.value.filter((p) => ids.includes(p.id))
}

onMounted(async () => {
  await loadRoles()
  await loadPermissions()
  await loadUsers()
})

async function loadRoles() {
  try {
    roles.value = await listRoles()
    const entries = await Promise.all(
      roles.value.map(async (r) => {
        try {
          return [r.id, await rolePermissions(r.id)]
        } catch (e) {
          return [r.id, []]
        }
      })
    )
    rolePermMap.value = Object.fromEntries(entries)
  } catch (e) {
    roles.value = []
  }
}

async function loadPermissions() {
  try {
    allPermissions.value = await fetchAllPermissions()
  } catch (e) {
    allPermissions.value = []
  }
}

async function loadUsers() {
  loadingUsers.value = true
  try {
    const page = await listUsers({
      keyword: userQuery.keyword || undefined,
      roleCode: userQuery.roleCode || undefined,
      page: userQuery.page,
      pageSize: userQuery.pageSize
    })
    users.value = page.records || []
    userTotal.value = page.total || 0
  } finally {
    loadingUsers.value = false
  }
}

function openUser(row) {
  if (row) {
    Object.assign(userForm, {
      id: row.id,
      username: row.username,
      password: '',
      name: row.name,
      phone: row.phone || '',
      email: row.email || '',
      roleIds: row.roleIds || [],
      status: row.status === undefined ? 1 : row.status
    })
  } else {
    Object.assign(userForm, {
      id: null, username: '', password: '', name: '', phone: '', email: '', roleIds: [], status: 1
    })
  }
  userVisible.value = true
}

async function submitUser() {
  await userFormRef.value.validate()
  saving.value = true
  try {
    if (userForm.id) {
      await updateUser({
        id: userForm.id,
        name: userForm.name,
        phone: userForm.phone,
        email: userForm.email,
        status: userForm.status,
        roleIds: userForm.roleIds
      })
      ElMessage.success('已保存')
    } else {
      await createUser({
        username: userForm.username,
        password: userForm.password,
        name: userForm.name,
        phone: userForm.phone,
        email: userForm.email,
        roleIds: userForm.roleIds
      })
      ElMessage.success('员工已创建，可以拿这个账号登录了')
    }
    userVisible.value = false
    await loadUsers()
  } catch (e) {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function openPwd(row) {
  Object.assign(pwdForm, { id: row.id, username: row.username, password: '' })
  pwdVisible.value = true
}

async function submitPwd() {
  await pwdFormRef.value.validate()
  saving.value = true
  try {
    await resetPassword(pwdForm.id, pwdForm.password)
    ElMessage.success('密码已重置')
    pwdVisible.value = false
  } catch (e) {
  } finally {
    saving.value = false
  }
}

function openRole() {
  Object.assign(roleForm, { code: '', name: '', description: '', permissionIds: [] })
  roleVisible.value = true
}

async function submitRole() {
  await roleFormRef.value.validate()
  saving.value = true
  try {
    await createRole({ ...roleForm })
    ElMessage.success('角色已创建，去员工那边就能选到它')
    roleVisible.value = false
    await loadRoles()
  } catch (e) {
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.atp-mono {
  font-family: Consolas, Monaco, monospace;
  font-size: 12.5px;
}

.atp-none {
  font-size: 12px;
  color: var(--atp-text-mute);
}

.atp-role-tag,
.atp-perm-tag {
  background: var(--atp-primary-soft);
  color: var(--atp-primary-dark);
  margin-right: 6px;
}

.atp-pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.atp-role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.atp-role-card { padding: 14px 16px; }

.atp-role-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.atp-role-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--atp-text);
}

.atp-role-code {
  background: #f3f4f6;
  color: #4b5563;
  font-family: Consolas, Monaco, monospace;
}

.atp-role-desc {
  font-size: 12.5px;
  color: var(--atp-text-mute);
  margin-bottom: 10px;
}

.atp-role-perms {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.atp-role-perms .atp-tag { margin-right: 0; }

.atp-toolbar-tip {
  margin-left: auto;
  font-size: 12.5px;
  color: var(--atp-text-mute);
}

.atp-perm-group {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 2px 12px;
}

.atp-perm-code {
  margin-left: 6px;
  font-size: 11px;
  color: var(--atp-text-mute);
  font-family: Consolas, Monaco, monospace;
}
</style>
