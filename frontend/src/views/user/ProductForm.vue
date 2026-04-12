<template>
  <div class="product-form-page">
    <div class="form-header">
      <el-button text @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <h1>{{ isEdit ? '编辑商品' : (isAdminMode ? '新增商品' : '发布商品') }}</h1>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">
        提交
      </el-button>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="product-form"
    >
      <!-- 商品名称 -->
      <el-form-item label="商品名称" prop="name">
        <el-input
          v-model="form.name"
          placeholder="请输入商品名称"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <!-- 商品描述 -->
      <el-form-item label="商品描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请输入商品描述"
          maxlength="2000"
          show-word-limit
        />
      </el-form-item>

      <!-- 价格和原价 -->
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="售价（元）" prop="price">
            <el-input-number
              v-model="form.price"
              :min="0"
              :precision="2"
              :step="0.01"
              :max="999999"
              placeholder="0.00"
              style="width: 100%"
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
              placeholder="0.00"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <!-- 库存 -->
      <el-form-item label="库存数量" prop="stock">
        <el-input-number
          v-model="form.stock"
          :min="0"
          :max="9999"
          placeholder="0"
          style="width: 100%"
        />
      </el-form-item>

      <!-- 商品分类 -->
      <el-form-item label="商品分类" prop="category">
        <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
          <el-option label="手机数码" value="手机数码" />
          <el-option label="电脑办公" value="电脑办公" />
          <el-option label="家用电器" value="家用电器" />
          <el-option label="娱乐玩具" value="娱乐玩具" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>

      <!-- 主图 -->
      <el-form-item label="商品主图" prop="mainImage">
        <el-input
          v-model="form.mainImage"
          placeholder="请输入主图 URL"
        />
        <div class="image-preview" v-if="form.mainImage">
          <el-image :src="form.mainImage" fit="cover" class="preview-image" />
        </div>
      </el-form-item>

      <!-- 商品图片列表 -->
      <el-form-item label="商品图片" prop="images">
        <el-input
          v-model="imagesInput"
          type="textarea"
          :rows="3"
          placeholder="每行一个图片 URL"
        />
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
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
        <el-button @click="parseImages" style="margin-top: 8px">
          解析图片 URL
        </el-button>
      </el-form-item>

      <!-- 标签 -->
      <el-form-item label="商品标签" prop="tags">
        <el-input
          v-model="tagsInput"
          placeholder="输入标签后按回车添加"
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

      <!-- 是否在售 -->
      <el-form-item label="上架状态" prop="isOnSale">
        <el-switch v-model="form.isOnSale" active-text="在售" inactive-text="下架" />
      </el-form-item>

      <!-- 是否推荐 -->
      <el-form-item label="推荐商品" prop="isFeatured">
        <el-switch v-model="form.isFeatured" active-text="推荐" inactive-text="不推荐" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Delete } from '@element-plus/icons-vue'
import {
  createProduct,
  updateProduct,
  getMyProductById,
  createProductForAdmin,
  updateProductForAdmin,
  getProductById
} from '@/api/product'
import type { ProductCreateRequest } from '@/api/product'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)
// 判断是否为管理端模式
const isAdminMode = computed(() => route.path.startsWith('/admin'))

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
    const res = isAdminMode.value
      ? await getProductById(Number(route.params.id))
      : await getMyProductById(Number(route.params.id))
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
        if (isAdminMode.value) {
          await updateProductForAdmin(Number(route.params.id), form)
          ElMessage.success('商品更新成功')
        } else {
          await updateProduct(Number(route.params.id), form)
          ElMessage.success('商品更新成功')
        }
      } else {
        if (isAdminMode.value) {
          await createProductForAdmin(form)
          ElMessage.success('商品创建成功')
        } else {
          await createProduct(form)
          ElMessage.success('商品发布成功')
        }
      }
      // 管理端返回列表到商品管理页，用户端返回到我的商品页
      router.push(isAdminMode.value ? '/admin/products' : '/user/products/my')
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

// 返回
const goBack = () => {
  router.push(isAdminMode.value ? '/admin/products' : '/user/products/my')
}

onMounted(() => {
  if (isEdit.value) {
    loadProduct()
  }
})
</script>

<style scoped>
.product-form-page {
  min-height: 100vh;
  background: #F5F5F5;
}

/* 表单头部 */
.form-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.form-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

/* 表单内容 */
.product-form {
  padding: 16px;
  background: #FFFFFF;
}

/* 图片预览 */
.image-preview {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  background: #F9FAFB;
  padding: 16px;
  border-radius: 8px;
}

.preview-image {
  max-width: 300px;
  max-height: 300px;
  border-radius: 8px;
}

/* 图片列表 */
.image-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #F9FAFB;
}

.image-item .el-button {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(255, 255, 255, 0.9);
}

.preview-image-small {
  width: 100%;
  height: 100%;
}

/* 标签列表 */
.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.product-tag {
  background: #F3F4F6;
  color: #4B5563;
}
</style>
