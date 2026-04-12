<template>
  <AdminLayout>
    <div class="admin-product-form-page">
    <div class="form-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h1>{{ isEdit ? '编辑商品' : '新增商品' }}</h1>
      </div>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        提交
      </el-button>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="right"
      label-width="120px"
      class="product-form"
    >
      <el-card class="form-section">
        <template #header>
          <span class="section-title">基本信息</span>
        </template>

        <!-- 商品名称 -->
        <el-form-item label="商品名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入商品名称"
            maxlength="100"
            show-word-limit
            style="max-width: 600px"
          />
        </el-form-item>

        <!-- 商品描述 -->
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="6"
            placeholder="请输入商品描述，建议详细描述商品的规格、成色、使用情况等"
            maxlength="2000"
            show-word-limit
            style="max-width: 800px"
          />
        </el-form-item>

        <!-- 商品分类 -->
        <el-form-item label="商品分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 200px">
            <el-option label="手机数码" value="手机数码" />
            <el-option label="电脑办公" value="电脑办公" />
            <el-option label="家用电器" value="家用电器" />
            <el-option label="娱乐玩具" value="娱乐玩具" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>

        <!-- 商品标签 -->
        <el-form-item label="商品标签" prop="tags">
          <el-input
            v-model="tagsInput"
            placeholder="输入标签后按回车添加"
            style="max-width: 400px"
            @keyup.enter="addTag"
          />
          <div class="tags-list" v-if="form.tags && form.tags.length > 0">
            <el-tag
              v-for="(tag, index) in form.tags"
              :key="index"
              closable
              @close="removeTag(index)"
              class="product-tag"
            >
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>
      </el-card>

      <el-card class="form-section">
        <template #header>
          <span class="section-title">价格与库存</span>
        </template>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="售价（元）" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                :step="0.01"
                :max="999999"
                placeholder="0.00"
                style="width: 200px"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原价（元）" prop="originalPrice">
              <el-input-number
                v-model="form.originalPrice"
                :min="0"
                :precision="2"
                :step="0.01"
                :max="999999"
                placeholder="可选"
                style="width: 200px"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="库存数量" prop="stock">
              <el-input-number
                v-model="form.stock"
                :min="0"
                :max="99999"
                placeholder="0"
                style="width: 200px"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否在售" prop="isOnSale">
              <el-switch v-model="form.isOnSale" active-text="在售" inactive-text="下架" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-card>

      <el-card class="form-section">
        <template #header>
          <span class="section-title">商品图片</span>
        </template>

        <!-- 主图 -->
        <el-form-item label="商品主图" prop="mainImage">
          <el-input
            v-model="form.mainImage"
            placeholder="请输入主图 URL"
            style="max-width: 600px"
          />
          <div class="image-preview" v-if="form.mainImage">
            <el-image :src="form.mainImage" fit="cover" class="preview-image" />
            <span class="preview-label">主图</span>
          </div>
        </el-form-item>

        <!-- 商品图片列表 -->
        <el-form-item label="商品图片" prop="images">
          <el-input
            v-model="imagesInput"
            type="textarea"
            :rows="4"
            placeholder="每行一个图片 URL"
            style="max-width: 600px"
          />
          <el-button @click="parseImages" style="margin-top: 8px">
            <el-icon><Plus /></el-icon>
            解析图片 URL
          </el-button>
          <div class="image-list" v-if="form.images && form.images.length > 0">
            <div
              v-for="(img, index) in form.images"
              :key="index"
              class="image-item"
            >
              <el-image :src="img" fit="cover" class="preview-image-small" />
              <el-button
                size="small"
                text
                type="danger"
                @click="removeImage(index)"
                class="delete-btn"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </el-form-item>
      </el-card>

      <el-card class="form-section">
        <template #header>
          <span class="section-title">高级设置</span>
        </template>

        <el-form-item label="推荐商品" prop="isFeatured">
          <el-switch v-model="form.isFeatured" active-text="推荐" inactive-text="不推荐" />
          <span class="form-tip">设为推荐后将在首页推荐展示</span>
        </el-form-item>
      </el-card>
    </el-form>

    <div class="form-footer">
      <el-button @click="goBack">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        提交
      </el-button>
    </div>
  </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Delete, Plus } from '@element-plus/icons-vue'
import {
  createProductForAdmin,
  updateProductForAdmin,
  getProductById
} from '@/api/product'
import type { ProductCreateRequest } from '@/api/product'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)

// 表单数据
const form = reactive<ProductCreateRequest>({
  name: '',
  description: '',
  price: 0,
  originalPrice: undefined,
  stock: 0,
  mainImage: '',
  images: [],
  category: '',
  tags: [],
  isOnSale: true,
  isFeatured: false
})

const imagesInput = ref('')
const tagsInput = ref('')

// 表单验证规则
const rules: FormRules = {
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 100, message: '商品名称不能超过 100 个字符', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入商品价格', trigger: 'blur' }
  ],
  stock: [
    { required: true, message: '请输入商品库存', trigger: 'blur' }
  ],
  mainImage: [
    { required: true, message: '请输入商品主图', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ]
}

// 添加标签
const addTag = () => {
  const tag = tagsInput.value.trim()
  if (tag) {
    form.tags = [...(form.tags || []), tag]
    tagsInput.value = ''
  }
}

// 删除标签
const removeTag = (index: number) => {
  form.tags?.splice(index, 1)
}

// 解析图片 URL
const parseImages = () => {
  const urls = imagesInput.value.split('\n').filter(url => url.trim())
  if (urls.length > 0) {
    form.images = [...(form.images || []), ...urls]
    imagesInput.value = ''
    ElMessage.success(`已添加 ${urls.length} 张图片`)
  }
}

// 删除图片
const removeImage = (index: number) => {
  form.images?.splice(index, 1)
}

// 加载商品数据（编辑模式）
const loadProduct = async () => {
  if (!route.params.id) return
  try {
    const res = await getProductById(Number(route.params.id))
    const product = res.data.data
    Object.assign(form, {
      name: product.name,
      description: product.description,
      price: product.price,
      originalPrice: product.originalPrice,
      stock: product.stock,
      mainImage: product.mainImage,
      images: product.images || [],
      category: product.category,
      tags: product.tags || [],
      isOnSale: product.isOnSale,
      isFeatured: product.isFeatured
    })
  } catch (error) {
    console.error('加载商品数据失败:', error)
    ElMessage.error('加载商品数据失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      if (isEdit.value) {
        await updateProductForAdmin(Number(route.params.id), form)
        ElMessage.success('商品更新成功')
      } else {
        await createProductForAdmin(form)
        ElMessage.success('商品创建成功')
      }
      router.push('/admin/products')
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

// 返回
const goBack = () => {
  router.push('/admin/products')
}

onMounted(() => {
  if (isEdit.value) {
    loadProduct()
  }
})
</script>

<style scoped>
.admin-product-form-page {
  min-height: 100vh;
  background: #F5F5F6;
  padding: 20px;
}

/* 表单头部 */
.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background: #FFFFFF;
  padding: 16px 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

/* 表单内容 */
.product-form {
  max-width: 900px;
  margin: 0 auto;
}

.form-section {
  margin-bottom: 20px;
  border-radius: 8px;
}

.form-section :deep(.el-card__header) {
  padding: 16px 20px;
  background: #FAFAFA;
  border-bottom: 1px solid #F0F0F0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
}

/* 标签列表 */
.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.product-tag {
  background: #F3F4F6;
  color: #4B5563;
}

/* 图片预览 */
.image-preview {
  margin-top: 16px;
  position: relative;
  display: inline-block;
}

.preview-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #E5E7EB;
}

.preview-label {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.7);
  color: #FFFFFF;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

/* 图片列表 */
.image-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #F9FAFB;
  border: 1px solid #E5E7EB;
}

.image-item .el-image {
  width: 100%;
  height: 100%;
}

.delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  padding: 4px;
}

.delete-btn:hover {
  background: #FFFFFF;
}

/* 表单提示 */
.form-tip {
  margin-left: 12px;
  font-size: 13px;
  color: #6B7280;
}

/* 表单底部 */
.form-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  background: #FFFFFF;
  border-radius: 8px;
  margin-top: 20px;
}

.form-footer .el-button {
  min-width: 120px;
  height: 40px;
  font-size: 15px;
}
</style>
