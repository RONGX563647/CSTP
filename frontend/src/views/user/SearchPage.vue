<template>
  <div class="search-page">
    <div class="search-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      
      <div class="search-input-wrapper">
        <el-input
          ref="searchInputRef"
          v-model="keyword"
          placeholder="搜索商品名称、描述、标签..."
          clearable
          @input="handleSearchInput"
          @clear="clearSearch"
          @keyup.enter="handleSearch"
          autofocus
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      
      <el-button type="primary" @click="handleSearch" :disabled="!keyword.trim()">
        搜索
      </el-button>
    </div>

    <div class="search-content">
      <!-- 搜索历史 -->
      <div class="search-history-section" v-if="!keyword && searchHistory.length > 0">
        <div class="section-header">
          <span>搜索历史</span>
          <el-button text size="small" @click="clearHistory">
            <el-icon><Delete /></el-icon>
            清除
          </el-button>
        </div>
        <div class="history-tags">
          <el-tag
            v-for="item in searchHistory"
            :key="item"
            size="large"
            class="history-tag"
            @click="selectHistory(item)"
          >
            {{ item }}
          </el-tag>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div class="hot-keywords-section" v-if="!keyword && hotKeywords.length > 0">
        <div class="section-header">
          <span>热门搜索</span>
        </div>
        <div class="hot-tags">
          <el-tag
            v-for="(item, index) in hotKeywords"
            :key="item"
            size="large"
            :type="index < 3 ? 'danger' : 'warning'"
            class="hot-tag"
            @click="selectHotKeyword(item)"
          >
            <span v-if="index < 3" class="hot-rank">{{ index + 1 }}</span>
            {{ item }}
          </el-tag>
        </div>
      </div>

      <!-- 搜索建议（输入中但未搜索） -->
      <div class="search-suggest-section" v-if="keyword && !hasSearched && suggestKeywords.length > 0">
        <div class="section-header">
          <span>搜索建议</span>
        </div>
        <div class="suggest-list">
          <div
            v-for="item in suggestKeywords"
            :key="item"
            class="suggest-item"
            @click="selectHistory(item)"
          >
            <el-icon><Search /></el-icon>
            <span v-html="highlightKeyword(item, keyword)"></span>
          </div>
        </div>
      </div>

      <!-- 搜索结果 -->
      <div class="search-results" v-if="hasSearched">
        <div class="results-header" v-if="searchResults.length > 0">
          <span>找到 {{ totalResults }} 个相关商品</span>
          <el-select v-model="sortBy" size="small" @change="handleSortChange">
            <el-option label="最相关" value="relevance" />
            <el-option label="最新" value="createdAt,desc" />
            <el-option label="价格从低到高" value="price,asc" />
            <el-option label="价格从高到低" value="price,desc" />
            <el-option label="最热门" value="sales,desc" />
          </el-select>
        </div>

        <div class="results-list" v-if="searchResults.length > 0">
          <div
            v-for="result in searchResults"
            :key="result.product.id"
            class="result-item"
            @click="goToDetail(result.product.id)"
          >
            <el-image
              :src="result.product.mainImage"
              fit="cover"
              class="result-image"
            >
              <template #error>
                <div class="image-error">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            
            <div class="result-info">
              <div class="result-name">
                <span v-html="highlightKeyword(result.product.name, keyword)"></span>
                <el-tag v-if="result.matchType && result.matchType !== 'none'" size="small" :type="getMatchTagType(result.matchType)" effect="plain">
                  {{ getMatchTypeText(result.matchType) }}
                </el-tag>
              </div>
              <div class="result-desc" v-if="result.product.description">
                <span v-html="highlightDescription(result.product.description, keyword)"></span>
              </div>
              <div class="result-tags" v-if="result.product.tags && result.product.tags.length > 0">
                <el-tag
                  v-for="tag in result.product.tags.slice(0, 3)"
                  :key="tag"
                  size="small"
                  type="info"
                  effect="plain"
                  class="result-tag"
                >
                  {{ tag }}
                </el-tag>
              </div>
              <div class="result-meta">
                <span class="result-price">¥{{ result.product.price }}</span>
                <span class="result-sales">{{ result.product.salesCount }}人想要</span>
              </div>
            </div>
          </div>
        </div>

        <div class="no-results" v-if="searchResults.length === 0 && !loading">
          <el-empty description="没有找到相关商品">
            <template #description>
              <p>没有找到"{{ keyword }}"的相关商品</p>
              <p class="no-results-tip">试试其他关键词，或使用更简短的词</p>
            </template>
            <el-button type="primary" @click="clearSearch">清除搜索</el-button>
          </el-empty>
        </div>

        <div class="loading-more" v-if="loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>搜索中...</span>
        </div>

        <div class="load-more" v-if="searchResults.length > 0 && hasMore && !loading">
          <el-button @click="loadMore">加载更多</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowLeft, Delete, Picture, Loading } from '@element-plus/icons-vue'
import { smartSearchProducts, getHotKeywords, getUserSearchHistory, clearSearchHistory } from '@/api/product'

const router = useRouter()

const keyword = ref('')
const searchResults = ref<any[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(20)
const totalResults = ref(0)
const hasMore = ref(false)
const sortBy = ref('relevance')
const hasSearched = ref(false)

const searchHistory = ref<string[]>([])
const hotKeywords = ref<string[]>([])
const searchInputRef = ref()

// 搜索建议 - 从历史和热门中过滤
const suggestKeywords = computed(() => {
  if (!keyword.value || keyword.value.length < 1) return []
  const kw = keyword.value.toLowerCase()
  const all = [...searchHistory.value, ...hotKeywords.value]
  const filtered = all.filter(item => item.toLowerCase().includes(kw))
  return [...new Set(filtered)].slice(0, 8)
})

const loadSearchHistory = async () => {
  try {
    const res = await getUserSearchHistory(15)
    searchHistory.value = res.data.data || []
  } catch (e) {
    console.error('加载搜索历史失败:', e)
  }
}

const loadHotKeywords = async () => {
  try {
    const res = await getHotKeywords(10)
    hotKeywords.value = res.data.data || []
  } catch (e) {
    console.error('加载热门关键词失败:', e)
  }
}

let searchTimer: ReturnType<typeof setTimeout> | null = null

const handleSearchInput = () => {
  hasSearched.value = false
  if (searchTimer) clearTimeout(searchTimer)
  if (keyword.value.length >= 1) {
    searchTimer = setTimeout(() => {
      page.value = 0
      searchResults.value = []
      handleSearch()
    }, 400) // 防抖 400ms
  }
}

const handleSearch = async () => {
  if (!keyword.value.trim()) return
  
  loading.value = true
  hasSearched.value = true
  
  try {
    const params: any = {
      keyword: keyword.value.trim(),
      page: page.value,
      size: size.value
    }
    
    // 非 relevance 排序时，切换到普通搜索接口的逻辑（后续可扩展）
    // 目前智能搜索本身已按相关性排序，其他排序在前端处理
    
    const res = await smartSearchProducts(params)
    
    const { content, totalElements, totalPages } = res.data.data
    
    if (page.value === 0) {
      searchResults.value = content
    } else {
      searchResults.value = [...searchResults.value, ...content]
    }
    
    totalResults.value = totalElements
    hasMore.value = page.value < totalPages - 1
  } catch (e) {
    console.error('搜索失败:', e)
  } finally {
    loading.value = false
  }
}

const handleSortChange = () => {
  page.value = 0
  searchResults.value = []
  handleSearch()
}

const loadMore = async () => {
  page.value++
  await handleSearch()
}

const clearSearch = () => {
  keyword.value = ''
  searchResults.value = []
  totalResults.value = 0
  hasSearched.value = false
  nextTick(() => {
    searchInputRef.value?.focus()
  })
}

const selectHistory = (item: string) => {
  keyword.value = item
  page.value = 0
  searchResults.value = []
  handleSearch()
}

const selectHotKeyword = (item: string) => {
  keyword.value = item
  page.value = 0
  searchResults.value = []
  handleSearch()
}

const clearHistory = async () => {
  try {
    await clearSearchHistory()
    searchHistory.value = []
  } catch (e) {
    console.error('清除搜索历史失败:', e)
    searchHistory.value = [] // 即使后端失败也清除前端显示
  }
}

const goBack = () => {
  router.back()
}

const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

const getMatchTypeText = (matchType: string) => {
  const texts: Record<string, string> = {
    'name': '名称匹配',
    'category': '分类匹配',
    'tag': '标签匹配',
    'description': '描述匹配',
    'synonym': '相关推荐'
  }
  return texts[matchType] || ''
}

const getMatchTagType = (matchType: string) => {
  const types: Record<string, string> = {
    'name': 'success',
    'category': 'primary',
    'tag': 'warning',
    'description': 'info',
    'synonym': 'danger'
  }
  return types[matchType] || 'info'
}

/**
 * 高亮关键词
 */
const highlightKeyword = (text: string, kw: string) => {
  if (!text || !kw) return text
  const escapedKw = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKw})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

/**
 * 高亮描述中的关键词并截断
 */
const highlightDescription = (desc: string, kw: string) => {
  if (!desc) return ''
  if (!kw) return desc.length > 80 ? desc.substring(0, 80) + '...' : desc
  
  const lowerDesc = desc.toLowerCase()
  const lowerKw = kw.toLowerCase()
  const index = lowerDesc.indexOf(lowerKw)
  
  let result = desc
  if (index !== -1) {
    const start = Math.max(0, index - 25)
    const end = Math.min(desc.length, index + kw.length + 35)
    result = desc.substring(start, end)
    if (start > 0) result = '...' + result
    if (end < desc.length) result = result + '...'
  } else {
    result = desc.length > 80 ? desc.substring(0, 80) + '...' : desc
  }
  
  // 高亮关键词
  const escapedKw = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escapedKw})`, 'gi')
  return result.replace(regex, '<mark class="search-highlight">$1</mark>')
}

onMounted(async () => {
  await Promise.all([loadSearchHistory(), loadHotKeywords()])
  nextTick(() => {
    searchInputRef.value?.focus()
  })
})
</script>

<style scoped>
.search-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #FFFFFF;
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.search-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.back-btn {
  color: #1F2937;
  font-size: 20px;
}

.search-input-wrapper {
  flex: 1;
}

.search-input-wrapper :deep(.el-input__wrapper) {
  background: #FFFFFF;
  border-radius: 20px;
  box-shadow: none;
}

.search-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.search-history-section,
.hot-keywords-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
  color: #6B7280;
  font-weight: 500;
}

.history-tags,
.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag,
.hot-tag {
  cursor: pointer;
  border-radius: 16px;
  padding: 8px 16px;
  transition: all 0.2s;
}

.history-tag:hover,
.hot-tag:hover {
  opacity: 0.8;
  transform: scale(1.05);
}

.hot-rank {
  display: inline-block;
  width: 16px;
  height: 16px;
  line-height: 16px;
  text-align: center;
  border-radius: 50%;
  background: #EF4444;
  color: #FFF;
  font-size: 10px;
  margin-right: 4px;
  font-weight: bold;
}

/* 搜索建议 */
.search-suggest-section {
  margin-bottom: 24px;
}

.suggest-list {
  background: #F9FAFB;
  border-radius: 12px;
  overflow: hidden;
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  color: #374151;
  font-size: 14px;
}

.suggest-item:hover {
  background: #F3F4F6;
}

.suggest-item + .suggest-item {
  border-top: 1px solid #E5E7EB;
}

.suggest-item .el-icon {
  color: #9CA3AF;
  font-size: 14px;
}

/* 搜索结果 */
.search-results {
  flex: 1;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E5E7EB;
  font-size: 14px;
  color: #6B7280;
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.result-item:hover {
  background: #F3F4F6;
  transform: translateX(4px);
}

.result-image {
  width: 90px;
  height: 90px;
  border-radius: 8px;
  flex-shrink: 0;
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #E5E7EB;
  color: #9CA3AF;
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.result-name {
  font-size: 15px;
  font-weight: 500;
  color: #1F2937;
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;
}

.result-name :deep(.search-highlight) {
  background: #FEF08A;
  color: inherit;
  padding: 0 2px;
  border-radius: 2px;
}

.result-desc {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.result-desc :deep(.search-highlight) {
  background: #FEF08A;
  color: inherit;
  padding: 0 2px;
  border-radius: 2px;
}

.result-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.result-tag {
  font-size: 11px;
}

.result-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 4px;
}

.result-price {
  font-size: 16px;
  font-weight: 600;
  color: #F59E0B;
}

.result-sales {
  font-size: 12px;
  color: #9CA3AF;
}

.no-results {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.no-results-tip {
  font-size: 13px;
  color: #9CA3AF;
  margin-top: 4px;
}

.loading-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 20px;
  color: #6B7280;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: 16px;
}
</style>
