declare module 'element-plus/dist/locale/zh-cn.mjs' {
  const locale: any
  export default locale
}

interface ImportMetaEnv {
  readonly DEV: boolean
  readonly PROD: boolean
  readonly MODE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
