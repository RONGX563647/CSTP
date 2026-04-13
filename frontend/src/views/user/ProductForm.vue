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

      <!-- 主图上传 -->
      <el-form-item label="商品主图" prop="mainImage">
        <ImageUpload
          v-model="form.mainImage"
          upload-type="product-main"
          :max-size="10"
          placeholder="点击上传商品主图"
          @success="handleMainImageSuccess"
        />
      </el-form-item>

      <!-- 商品图片列表上传 -->
      <el-form-item label="商品详情图片" prop="images">
        <MultiImageUpload
          v-model="form.images"
          :max-size="10"
          :max-count="9"
          placeholder="点击上传详情图片（可多选）"
        />
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
import { ArrowLeft } from '@element-plus/icons-vue'
import ImageUpload from '@/components/ImageUpload.vue'
import MultiImageUpload from '@/components/MultiImageUpload.vue'
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
const isAdminMode = computed(() => route.path.startsWith('/admin'))

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

const tagsInput = ref('')

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
    { required: true, message: '请上传商品主图', trigger: 'change' }
  ],
  category: [
    { required: true, message: '请选择商品分类', trigger: 'change' }
  ]
}

const handleMainImageSuccess = (data: { url: string; fileName: string; size: number }) => {
  console.log('主图上传成功:', data)
}

const addTag = () => {
  const tag = tagsInput.value.trim()
  if (tag) {
    form.tags = [...(form.tags || []), tag]
    tagsInput.value = ''
  }
}

const removeTag = (index: number) => {
  form.tags?.splice(index, 1)
}

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
      router.push(isAdminMode.value ? '/admin/products' : '/user/products/my')
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitting.value = false
    }
  })
}

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

.product-form {
  padding: 16px;
  background: #FFFFFF;
}

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