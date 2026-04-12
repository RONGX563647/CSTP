<template>
  <MobileLayout :title="isEdit ? '编辑地址' : '新建地址'">
    <div class="address-form-page">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-position="top"
        class="address-form"
      >
        <!-- 收件人信息 -->
        <el-form-item label="收件人姓名" prop="receiverName">
          <el-input
            v-model="formData.receiverName"
            placeholder="请输入收件人姓名"
            clearable
          />
        </el-form-item>

        <el-form-item label="收件人电话" prop="receiverPhone">
          <el-input
            v-model="formData.receiverPhone"
            placeholder="请输入收件人手机号"
            maxlength="11"
            clearable
          />
        </el-form-item>

        <!-- 省市区选择 -->
        <el-form-item label="所在地区" prop="region">
          <el-input
            v-model="regionText"
            placeholder="请选择省/市/区"
            readonly
            @click="showRegionDialog = true"
          />
        </el-form-item>

        <!-- 详细地址 -->
        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="formData.detailAddress"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、门牌号等"
          />
        </el-form-item>

        <!-- 默认地址开关 -->
        <el-form-item>
          <div class="default-switch">
            <span>设为默认地址</span>
            <el-switch
              v-model="formData.isDefault"
              active-color="#F59E0B"
            />
          </div>
        </el-form-item>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            block
            :loading="submitting"
            @click="handleSubmit"
          >
            {{ isEdit ? '保存修改' : '立即创建' }}
          </el-button>
        </div>
      </el-form>

      <!-- 地区选择对话框 -->
      <el-dialog
        v-model="showRegionDialog"
        title="选择地区"
        width="90%"
        top="50%"
        :before-close="handleCloseRegion"
      >
        <el-input
          v-model="regionFilter"
          placeholder="搜索地区"
          clearable
          class="region-search"
        />
        <div class="region-list">
          <div
            v-for="region in filteredRegions"
            :key="region.name"
            class="region-item"
            :class="{ selected: selectedRegion === region.name }"
            @click="selectRegion(region)"
          >
            {{ region.name }}
          </div>
        </div>
      </el-dialog>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { createAddress, updateAddress, getAddressById, type Address } from '@/api/address'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const showRegionDialog = ref(false)
const regionFilter = ref('')
const selectedRegion = ref('')

// 表单数据
const formData = reactive<Address>({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

// 地区文本
const regionText = computed(() => {
  const parts = [formData.province, formData.city, formData.district].filter(Boolean)
  return parts.join('')
})

// 表单验证规则
const rules: FormRules = {
  receiverName: [
    { required: true, message: '请输入收件人姓名', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入收件人电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  region: [
    { required: true, message: '请选择所在地区', trigger: 'change' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

// 简单的地区数据
const regions = ref([
  { name: '北京市', type: 'province' },
  { name: '上海市', type: 'province' },
  { name: '广东省', type: 'province' },
  { name: '江苏省', type: 'province' },
  { name: '浙江省', type: 'province' }
])

const filteredRegions = computed(() => {
  if (!regionFilter.value) return regions.value
  return regions.value.filter(r => r.name.includes(regionFilter.value))
})

// 是否是编辑模式
const isEdit = computed(() => !!route.params.id)

// 加载地址数据
const loadAddress = async () => {
  const id = route.params.id
  if (id) {
    try {
      const res = await getAddressById(Number(id))
      Object.assign(formData, res.data)
    } catch (error) {
      ElMessage.error('加载地址失败')
      router.back()
    }
  }
}

// 选择地区
const selectRegion = (region: { name: string; type: string }) => {
  if (region.type === 'province') {
    formData.province = region.name
    formData.city = ''
    formData.district = ''
  }
  selectedRegion.value = region.name
  showRegionDialog.value = false
}

// 关闭地区选择框
const handleCloseRegion = (done: () => void) => {
  done()
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateAddress(Number(route.params.id), formData)
        ElMessage.success('修改成功')
      } else {
        await createAddress(formData)
        ElMessage.success('创建成功')
      }
      router.back()
    } catch (error: any) {
      ElMessage.error(error.message || (isEdit.value ? '修改失败' : '创建失败'))
    } finally {
      submitting.value = false
    }
  })
}

onMounted(() => {
  loadAddress()
})
</script>

<style scoped>
.address-form-page {
  padding: 16px;
  background: #F5F5F5;
  min-height: calc(100vh - 50px);
}

.address-form {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 16px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #1F2937;
  margin-bottom: 8px;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  border-radius: 8px;
  background: #F9FAFB;
}

.default-switch {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 12px 0;
  font-size: 15px;
  color: #1F2937;
}

.form-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #F3F4F6;
}

/* 地区选择器 */
:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
}

:deep(.el-dialog__title) {
  color: #1F2937;
}

:deep(.el-dialog__body) {
  padding: 16px;
}

.region-search {
  margin-bottom: 16px;
}

.region-list {
  max-height: 300px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.region-item {
  padding: 14px 16px;
  font-size: 15px;
  color: #1F2937;
  border-bottom: 1px solid #F3F4F6;
  cursor: pointer;
  transition: background 0.2s;
}

.region-item:last-child {
  border-bottom: none;
}

.region-item:hover {
  background: #F9FAFB;
}

.region-item.selected {
  color: #F59E0B;
  font-weight: 500;
}
</style>
