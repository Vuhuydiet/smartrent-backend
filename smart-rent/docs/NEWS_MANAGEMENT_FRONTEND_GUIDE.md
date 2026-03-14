# News Management - Frontend Integration Guide

## Overview

Complete guide for integrating the News Management feature. This feature has two parts:
- **Admin Panel**: Full CRUD + publish/unpublish for managing articles
- **Public Pages**: Read-only listing, detail view, and newest articles

Articles support **rich text HTML content** (headings, bold, images, lists, links) — use a WYSIWYG editor like **TipTap**, **Quill**, or **TinyMCE** for the admin editor.

---

## Data Types (TypeScript)

```typescript
// ============ ENUMS ============

type NewsCategory = 'NEWS' | 'BLOG' | 'POLICY' | 'MARKET' | 'PROJECT' | 'INVESTMENT' | 'GUIDE';

type NewsStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

// Recommended labels for UI display (Vietnamese)
const CATEGORY_LABELS: Record<NewsCategory, string> = {
  NEWS: 'Tin tức',
  BLOG: 'Blog',
  POLICY: 'Chính sách',
  MARKET: 'Thị trường',
  PROJECT: 'Dự án',
  INVESTMENT: 'Đầu tư',
  GUIDE: 'Hướng dẫn',
};

const STATUS_LABELS: Record<NewsStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  ARCHIVED: 'Đã lưu trữ',
};

// ============ API WRAPPER ============

interface ApiResponse<T> {
  code: string;    // "999999" = success
  message: string | null;
  data: T;
}

// ============ REQUEST DTOs ============

interface NewsCreateRequest {
  title: string;              // Required, max 255 chars
  summary?: string;           // Optional, max 1000 chars
  content: string;            // Required, HTML from rich text editor
  category: NewsCategory;     // Required
  tags?: string;              // Optional, comma-separated: "rental,tips,guide"
  thumbnailUrl?: string;      // Optional, max 500 chars (cover image URL)
  metaTitle?: string;         // Optional SEO title
  metaDescription?: string;   // Optional SEO description
  metaKeywords?: string;      // Optional SEO keywords
}

interface NewsUpdateRequest {
  title?: string;             // All fields optional for partial update
  summary?: string;
  content?: string;
  category?: NewsCategory;
  tags?: string;
  thumbnailUrl?: string;
  metaTitle?: string;
  metaDescription?: string;
  metaKeywords?: string;
}

// ============ RESPONSE DTOs ============

/** Full news response (admin views, create/update responses) */
interface NewsResponse {
  newsId: number;
  title: string;
  slug: string;
  summary: string;
  content: string;             // Full HTML content
  category: NewsCategory;
  tags: string[];              // Parsed into array (backend handles this)
  thumbnailUrl: string;
  status: NewsStatus;
  publishedAt: string | null;  // ISO datetime
  authorId: string;
  authorName: string;
  viewCount: number;
  metaTitle: string;
  metaDescription: string;
  metaKeywords: string;
  createdAt: string;           // ISO datetime
  updatedAt: string;           // ISO datetime
}

/** Summary for list views (no full content) */
interface NewsSummaryResponse {
  newsId: number;
  title: string;
  slug: string;
  summary: string;
  category: NewsCategory;
  tags: string[];
  thumbnailUrl: string;
  publishedAt: string | null;
  authorName: string;
  viewCount: number;
  createdAt: string;
}

/** Detail view with related posts */
interface NewsDetailResponse {
  newsId: number;
  title: string;
  slug: string;
  summary: string;
  content: string;             // Full HTML content
  category: NewsCategory;
  tags: string[];
  thumbnailUrl: string;
  publishedAt: string | null;
  authorName: string;
  viewCount: number;
  metaTitle: string;
  metaDescription: string;
  metaKeywords: string;
  createdAt: string;
  updatedAt: string;
  relatedNews: NewsSummaryResponse[];  // Up to 5 related articles
}

/** Paginated list response */
interface NewsListResponse {
  news: NewsSummaryResponse[];
  totalItems: number;
  currentPage: number;         // 1-based
  pageSize: number;
  totalPages: number;
}
```

---

## User Flows

### Public User Flow

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  News List   │────▶│  Click article   │────▶│  Article Detail  │
│  /tin-tuc    │     │  card            │     │  /tin-tuc/{slug} │
└──────────────┘     └──────────────────┘     └──────────────────┘
       │                                              │
       │  Filter by category                          │  Shows related
       │  Search by keyword                           │  articles at bottom
       │  Filter by tag                               │
       ▼                                              ▼
  GET /v1/news                              GET /v1/news/{slug}
  ?category=MARKET                          (auto-increments views)
  &keyword=...
  &tag=...
  &page=1&size=20
```

### Admin Flow

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│  News List   │────▶│  Create / Edit   │────▶│  Save as DRAFT   │
│  (all status)│     │  with WYSIWYG    │     │  or PUBLISH      │
└──────────────┘     └──────────────────┘     └──────────────────┘
       │                                              │
       │  Filter by status                            │
       │  (DRAFT/PUBLISHED/ARCHIVED)                  ▼
       │                                    ┌──────────────────┐
       │                                    │  Manage Status:  │
       ▼                                    │  Publish         │
  GET /v1/admin/news                        │  Unpublish       │
  ?status=DRAFT                             │  Archive         │
  &page=1&size=20                           │  Delete          │
                                            └──────────────────┘
```

---

## API Endpoints

### Public APIs (No Auth Required)

#### 1. Get Published News List

**Endpoint:** `GET /v1/news`

**Query Parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | number | 1 | Page number (1-based) |
| `size` | number | 20 | Items per page |
| `category` | string | — | Filter: `NEWS`, `BLOG`, `POLICY`, `MARKET`, `PROJECT`, `INVESTMENT`, `GUIDE` |
| `tag` | string | — | Filter by tag (e.g., `"rental"`) |
| `keyword` | string | — | Search in title and summary |

**Response:**
```json
{
  "code": "999999",
  "message": null,
  "data": {
    "news": [
      {
        "newsId": 1,
        "title": "Chính sách mới về thị trường bất động sản 2026",
        "slug": "chinh-sach-moi-ve-thi-truong-bat-dong-san-2026",
        "summary": "Tổng quan các chính sách mới ảnh hưởng đến thị trường BĐS",
        "category": "POLICY",
        "tags": ["chính sách", "bất động sản", "2026"],
        "thumbnailUrl": "https://storage.example.com/images/cover.jpg",
        "publishedAt": "2026-03-14T10:30:00",
        "authorName": "Admin",
        "viewCount": 1250,
        "createdAt": "2026-03-14T09:00:00"
      }
    ],
    "totalItems": 150,
    "currentPage": 1,
    "pageSize": 20,
    "totalPages": 8
  }
}
```

---

#### 2. Get Newest News

**Endpoint:** `GET /v1/news/newest?limit=10`

Use for "Tin mới nhất" sidebar or homepage section.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `limit` | number | 10 | Number of articles (1–50) |

**Response:**
```json
{
  "code": "999999",
  "data": [
    {
      "newsId": 5,
      "title": "Xu hướng đầu tư BĐS 2026",
      "slug": "xu-huong-dau-tu-bds-2026",
      "summary": "Phân tích xu hướng đầu tư nổi bật...",
      "category": "INVESTMENT",
      "tags": ["đầu tư", "xu hướng"],
      "thumbnailUrl": "https://storage.example.com/img5.jpg",
      "publishedAt": "2026-03-14T14:00:00",
      "authorName": "Admin",
      "viewCount": 320,
      "createdAt": "2026-03-14T12:00:00"
    }
  ]
}
```

---

#### 3. Get Article Detail (by Slug)

**Endpoint:** `GET /v1/news/{slug}`

> **Note:** This endpoint automatically increments the view count. Only returns PUBLISHED articles.

**Response:**
```json
{
  "code": "999999",
  "data": {
    "newsId": 1,
    "title": "Chính sách mới về thị trường bất động sản 2026",
    "slug": "chinh-sach-moi-ve-thi-truong-bat-dong-san-2026",
    "summary": "Tổng quan các chính sách mới...",
    "content": "<h2>Tổng quan</h2><p>Nội dung chi tiết về chính sách...</p><img src='https://storage.example.com/inline.jpg' /><ul><li>Điểm 1</li><li>Điểm 2</li></ul>",
    "category": "POLICY",
    "tags": ["chính sách", "bất động sản", "2026"],
    "thumbnailUrl": "https://storage.example.com/images/cover.jpg",
    "publishedAt": "2026-03-14T10:30:00",
    "authorName": "Admin",
    "viewCount": 1251,
    "metaTitle": "Chính sách BĐS 2026",
    "metaDescription": "Tổng quan các chính sách mới...",
    "metaKeywords": "chính sách, bất động sản, 2026",
    "createdAt": "2026-03-14T09:00:00",
    "updatedAt": "2026-03-14T10:00:00",
    "relatedNews": [
      {
        "newsId": 3,
        "title": "Quy định mới về sổ đỏ...",
        "slug": "quy-dinh-moi-ve-so-do",
        "summary": "...",
        "category": "POLICY",
        "tags": ["pháp lý"],
        "thumbnailUrl": "https://storage.example.com/img3.jpg",
        "publishedAt": "2026-03-10T08:00:00",
        "authorName": "Admin",
        "viewCount": 890,
        "createdAt": "2026-03-10T07:00:00"
      }
    ]
  }
}
```

---

### Admin APIs (Auth Required)

> All admin endpoints require `Authorization: Bearer <jwt_token>` header.

#### 4. Create Article

**Endpoint:** `POST /v1/admin/news`

**Request:**
```json
{
  "title": "Top 10 dự án BĐS nổi bật Q1/2026",
  "summary": "Tổng hợp các dự án bất động sản đáng chú ý...",
  "content": "<h2>1. Vinhomes Grand Park</h2><p>Dự án nằm tại...</p>",
  "category": "PROJECT",
  "tags": "dự án,bất động sản,2026,Q1",
  "thumbnailUrl": "https://storage.example.com/cover-project.jpg",
  "metaTitle": "Top 10 dự án BĐS Q1/2026",
  "metaDescription": "Tổng hợp các dự án bất động sản đáng chú ý nhất quý 1 năm 2026"
}
```

> **Important:** `tags` is sent as a **comma-separated string** in the request. The response returns `tags` as a **parsed array**.

**Response:** `ApiResponse<NewsResponse>` — article created in **DRAFT** status.

---

#### 5. Update Article

**Endpoint:** `PUT /v1/admin/news/{newsId}`

All fields are optional — only send what you want to update:

```json
{
  "title": "Updated title",
  "content": "<h2>Updated content</h2><p>...</p>",
  "category": "MARKET"
}
```

**Response:** `ApiResponse<NewsResponse>`

---

#### 6. Delete Article

**Endpoint:** `DELETE /v1/admin/news/{newsId}`

**Response:**
```json
{
  "code": "999999",
  "message": "News deleted successfully",
  "data": null
}
```

---

#### 7. Publish Article

**Endpoint:** `POST /v1/admin/news/{newsId}/publish`

Sets status to `PUBLISHED` and records `publishedAt` timestamp (only on first publish).

**Response:** `ApiResponse<NewsResponse>` with `status: "PUBLISHED"`

---

#### 8. Unpublish Article

**Endpoint:** `POST /v1/admin/news/{newsId}/unpublish`

Sets status back to `DRAFT`. Article is no longer visible to public.

**Response:** `ApiResponse<NewsResponse>` with `status: "DRAFT"`

---

#### 9. Archive Article

**Endpoint:** `POST /v1/admin/news/{newsId}/archive`

Sets status to `ARCHIVED`. Hidden from public.

**Response:** `ApiResponse<NewsResponse>` with `status: "ARCHIVED"`

---

#### 10. Get All News (Admin)

**Endpoint:** `GET /v1/admin/news`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | number | 1 | Page number (1-based) |
| `size` | number | 20 | Items per page |
| `status` | string | — | Optional filter: `DRAFT`, `PUBLISHED`, `ARCHIVED` |

**Response:** `ApiResponse<NewsListResponse>`

---

#### 11. Get Article by ID (Admin)

**Endpoint:** `GET /v1/admin/news/{newsId}`

Returns full article including DRAFT and ARCHIVED ones (unlike public endpoint).

**Response:** `ApiResponse<NewsResponse>`

---

## Error Codes

| Code | HTTP Status | Message | When |
|------|-------------|---------|------|
| `15001` | 404 | News not found | Article ID/slug doesn't exist |
| `15002` | 409 | News slug already exists | Title generates duplicate slug |
| `15003` | 400 | News is not published | Public user tries to view unpublished article |
| `15004` | 400 | Invalid limit | `limit` param < 1 or > 50 on `/newest` |

**Error response format:**
```json
{
  "code": "15001",
  "message": "News not found",
  "data": null
}
```

---

## Frontend Implementation Examples

### API Service Layer

```typescript
const API_BASE = '/api/v1';

// ============ PUBLIC APIs ============

export async function getPublishedNews(params: {
  page?: number;
  size?: number;
  category?: NewsCategory;
  tag?: string;
  keyword?: string;
}): Promise<NewsListResponse> {
  const query = new URLSearchParams();
  if (params.page) query.set('page', String(params.page));
  if (params.size) query.set('size', String(params.size));
  if (params.category) query.set('category', params.category);
  if (params.tag) query.set('tag', params.tag);
  if (params.keyword) query.set('keyword', params.keyword);

  const res = await fetch(`${API_BASE}/news?${query}`);
  const json: ApiResponse<NewsListResponse> = await res.json();
  return json.data;
}

export async function getNewestNews(limit = 10): Promise<NewsSummaryResponse[]> {
  const res = await fetch(`${API_BASE}/news/newest?limit=${limit}`);
  const json: ApiResponse<NewsSummaryResponse[]> = await res.json();
  return json.data;
}

export async function getNewsDetail(slug: string): Promise<NewsDetailResponse> {
  const res = await fetch(`${API_BASE}/news/${slug}`);
  const json: ApiResponse<NewsDetailResponse> = await res.json();
  if (json.code !== '999999') throw new Error(json.message || 'News not found');
  return json.data;
}

// ============ ADMIN APIs ============

function authHeaders(): HeadersInit {
  return {
    'Authorization': `Bearer ${getToken()}`,
    'Content-Type': 'application/json',
  };
}

export async function createNews(data: NewsCreateRequest): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}

export async function updateNews(newsId: number, data: NewsUpdateRequest): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news/${newsId}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}

export async function deleteNews(newsId: number): Promise<void> {
  await fetch(`${API_BASE}/admin/news/${newsId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
}

export async function publishNews(newsId: number): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news/${newsId}/publish`, {
    method: 'POST',
    headers: authHeaders(),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}

export async function unpublishNews(newsId: number): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news/${newsId}/unpublish`, {
    method: 'POST',
    headers: authHeaders(),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}

export async function archiveNews(newsId: number): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news/${newsId}/archive`, {
    method: 'POST',
    headers: authHeaders(),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}

export async function getAdminNews(params: {
  page?: number;
  size?: number;
  status?: NewsStatus;
}): Promise<NewsListResponse> {
  const query = new URLSearchParams();
  if (params.page) query.set('page', String(params.page));
  if (params.size) query.set('size', String(params.size));
  if (params.status) query.set('status', params.status);

  const res = await fetch(`${API_BASE}/admin/news?${query}`, {
    headers: authHeaders(),
  });
  const json: ApiResponse<NewsListResponse> = await res.json();
  return json.data;
}

export async function getNewsById(newsId: number): Promise<NewsResponse> {
  const res = await fetch(`${API_BASE}/admin/news/${newsId}`, {
    headers: authHeaders(),
  });
  const json: ApiResponse<NewsResponse> = await res.json();
  return json.data;
}
```

---

### React Component: News List Page (Public)

```tsx
function NewsListPage() {
  const [news, setNews] = useState<NewsSummaryResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(1);
  const [category, setCategory] = useState<NewsCategory | ''>('');
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    loadNews();
  }, [page, category]);

  const loadNews = async () => {
    const data = await getPublishedNews({
      page,
      size: 12,
      category: category || undefined,
      keyword: keyword || undefined,
    });
    setNews(data.news);
    setTotalPages(data.totalPages);
  };

  return (
    <div>
      {/* Category tabs */}
      <div className="category-tabs">
        {['', 'NEWS', 'POLICY', 'MARKET', 'PROJECT', 'INVESTMENT', 'GUIDE', 'BLOG'].map(cat => (
          <button
            key={cat}
            className={category === cat ? 'active' : ''}
            onClick={() => { setCategory(cat as NewsCategory); setPage(1); }}
          >
            {cat ? CATEGORY_LABELS[cat as NewsCategory] : 'Tất cả'}
          </button>
        ))}
      </div>

      {/* Search bar */}
      <input
        placeholder="Tìm kiếm bài viết..."
        value={keyword}
        onChange={e => setKeyword(e.target.value)}
        onKeyDown={e => e.key === 'Enter' && loadNews()}
      />

      {/* News grid */}
      <div className="news-grid">
        {news.map(item => (
          <a key={item.newsId} href={`/tin-tuc/${item.slug}`}>
            <img src={item.thumbnailUrl} alt={item.title} />
            <span className="category-badge">{CATEGORY_LABELS[item.category]}</span>
            <h3>{item.title}</h3>
            <p>{item.summary}</p>
            <div className="meta">
              <span>{item.authorName}</span>
              <span>{formatDate(item.publishedAt)}</span>
              <span>{item.viewCount} lượt xem</span>
            </div>
            <div className="tags">
              {item.tags.map(tag => <span key={tag} className="tag">{tag}</span>)}
            </div>
          </a>
        ))}
      </div>

      {/* Pagination */}
      <Pagination current={page} total={totalPages} onChange={setPage} />
    </div>
  );
}
```

---

### React Component: Article Detail Page (Public)

```tsx
function NewsDetailPage({ slug }: { slug: string }) {
  const [article, setArticle] = useState<NewsDetailResponse | null>(null);

  useEffect(() => {
    getNewsDetail(slug).then(setArticle).catch(() => navigate('/404'));
  }, [slug]);

  if (!article) return <Loading />;

  return (
    <article>
      {/* SEO meta tags */}
      <Helmet>
        <title>{article.metaTitle || article.title}</title>
        <meta name="description" content={article.metaDescription || article.summary} />
        <meta name="keywords" content={article.metaKeywords || ''} />
      </Helmet>

      {/* Cover image */}
      <img src={article.thumbnailUrl} alt={article.title} className="cover-image" />

      {/* Article header */}
      <span className="category-badge">{CATEGORY_LABELS[article.category]}</span>
      <h1>{article.title}</h1>
      <div className="meta">
        <span>{article.authorName}</span>
        <span>{formatDate(article.publishedAt)}</span>
        <span>{article.viewCount} lượt xem</span>
      </div>

      {/* Tags */}
      <div className="tags">
        {article.tags.map(tag => <span key={tag} className="tag">{tag}</span>)}
      </div>

      {/* Rich text content — render HTML safely */}
      <div
        className="article-content"
        dangerouslySetInnerHTML={{ __html: article.content }}
      />

      {/* Related articles */}
      {article.relatedNews.length > 0 && (
        <section className="related-news">
          <h2>Bài viết liên quan</h2>
          <div className="related-grid">
            {article.relatedNews.map(related => (
              <a key={related.newsId} href={`/tin-tuc/${related.slug}`}>
                <img src={related.thumbnailUrl} alt={related.title} />
                <h4>{related.title}</h4>
                <span>{formatDate(related.publishedAt)}</span>
              </a>
            ))}
          </div>
        </section>
      )}
    </article>
  );
}
```

---

### React Component: Admin Article Editor

```tsx
function AdminNewsEditor({ newsId }: { newsId?: number }) {
  const isEdit = !!newsId;
  const [form, setForm] = useState<NewsCreateRequest>({
    title: '',
    summary: '',
    content: '',
    category: 'NEWS',
    tags: '',
    thumbnailUrl: '',
  });
  const [saving, setSaving] = useState(false);

  // Load existing article for editing
  useEffect(() => {
    if (isEdit && newsId) {
      getNewsById(newsId).then(data => {
        setForm({
          title: data.title,
          summary: data.summary,
          content: data.content,
          category: data.category,
          tags: data.tags.join(', '),
          thumbnailUrl: data.thumbnailUrl,
          metaTitle: data.metaTitle,
          metaDescription: data.metaDescription,
          metaKeywords: data.metaKeywords,
        });
      });
    }
  }, [newsId]);

  const handleSave = async () => {
    setSaving(true);
    try {
      if (isEdit) {
        await updateNews(newsId!, form);
        toast.success('Đã cập nhật bài viết');
      } else {
        const created = await createNews(form);
        toast.success('Đã tạo bài viết');
        navigate(`/admin/news/${created.newsId}/edit`);
      }
    } catch (err: any) {
      if (err.code === '15002') {
        toast.error('Tiêu đề này đã tồn tại, vui lòng chọn tiêu đề khác');
      } else {
        toast.error('Có lỗi xảy ra');
      }
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async () => {
    if (!newsId) return;
    await publishNews(newsId);
    toast.success('Đã xuất bản bài viết');
  };

  return (
    <div className="editor-page">
      {/* Title */}
      <input
        placeholder="Tiêu đề bài viết..."
        value={form.title}
        onChange={e => setForm({ ...form, title: e.target.value })}
      />

      {/* Summary */}
      <textarea
        placeholder="Tóm tắt nội dung..."
        value={form.summary}
        onChange={e => setForm({ ...form, summary: e.target.value })}
      />

      {/* Category selector */}
      <select
        value={form.category}
        onChange={e => setForm({ ...form, category: e.target.value as NewsCategory })}
      >
        {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
          <option key={value} value={value}>{label}</option>
        ))}
      </select>

      {/* Tags input */}
      <input
        placeholder="Tags (cách nhau bởi dấu phẩy): chính sách, bất động sản"
        value={form.tags}
        onChange={e => setForm({ ...form, tags: e.target.value })}
      />

      {/* Cover image upload — use your existing media upload API */}
      <ImageUpload
        value={form.thumbnailUrl}
        onChange={url => setForm({ ...form, thumbnailUrl: url })}
      />

      {/* Rich text editor — Use TipTap / Quill / TinyMCE */}
      <RichTextEditor
        value={form.content}
        onChange={html => setForm({ ...form, content: html })}
      />

      {/* Actions */}
      <div className="actions">
        <button onClick={handleSave} disabled={saving}>
          {saving ? 'Đang lưu...' : 'Lưu bản nháp'}
        </button>
        {isEdit && (
          <button onClick={handlePublish} className="primary">
            Xuất bản
          </button>
        )}
      </div>
    </div>
  );
}
```

---

## Image Upload for Cover & Inline Images

Use the **existing Media Upload API** to upload images:

```
POST /v1/media/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>

file: <image file>
```

The response will contain the uploaded file URL. Use this URL for:
- `thumbnailUrl` (cover image)
- Inline images in the rich text editor content

---

## Rich Text Editor Setup

The `content` field stores **raw HTML**. Recommended editors:

| Editor | Package | Notes |
|--------|---------|-------|
| **TipTap** | `@tiptap/react` | Best for React, highly customizable |
| **React Quill** | `react-quill` | Simple, quick setup |
| **TinyMCE** | `@tinymce/tinymce-react` | Most feature-rich, needs API key |

Key features to enable:
- ✅ Headings (H2, H3, H4)
- ✅ Bold, Italic, Underline
- ✅ Ordered/Unordered lists
- ✅ Links
- ✅ Image insertion (upload via Media API, then insert URL)
- ✅ Blockquotes
- ✅ Code blocks (optional)

---

## Suggested Page Routes

### Public Pages

| Route | Component | API |
|-------|-----------|-----|
| `/tin-tuc` | NewsListPage | `GET /v1/news` |
| `/tin-tuc/:slug` | NewsDetailPage | `GET /v1/news/{slug}` |

### Admin Pages

| Route | Component | API |
|-------|-----------|-----|
| `/admin/news` | AdminNewsList | `GET /v1/admin/news` |
| `/admin/news/create` | AdminNewsEditor | `POST /v1/admin/news` |
| `/admin/news/:id/edit` | AdminNewsEditor | `GET /v1/admin/news/{id}` + `PUT` |

---

## UI/UX Recommendations

### Public News List (like vnexpress.net/bat-dong-san)

```
┌──────────────────────────────────────────────────────────────────────┐
│  Tất cả │ Chính sách │ Thị trường │ Dự án │ Đầu tư │ Hướng dẫn    │
├──────────────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │  [Cover Image]   │  │  [Cover Image]   │  │  [Cover Image]   │  │
│  │  CHÍNH SÁCH      │  │  THỊ TRƯỜNG      │  │  DỰ ÁN           │  │
│  │  Title here...   │  │  Title here...   │  │  Title here...   │  │
│  │  Summary text    │  │  Summary text    │  │  Summary text    │  │
│  │  Admin · 2h ago  │  │  Admin · 5h ago  │  │  Admin · 1d ago  │  │
│  │  👁 1,250 views  │  │  👁 890 views    │  │  👁 2,100 views  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│                                                                      │
│  ◀  1  2  3  ...  8  ▶                                              │
└──────────────────────────────────────────────────────────────────────┘
```

### Admin News List

```
┌──────────────────────────────────────────────────────────────────────┐
│  🔍 Search...                     [+ Tạo bài viết mới]             │
│  Status: [All ▼]                                                     │
├──────────────────────────────────────────────────────────────────────┤
│  Title                    │ Category   │ Status    │ Actions         │
│  ─────────────────────────┼────────────┼───────────┼──────────────── │
│  Chính sách mới BĐS 2026 │ Chính sách │ ✅ Đã XB  │ ✏️ 🗑️ 📤      │
│  Xu hướng đầu tư Q1      │ Đầu tư     │ 📝 Nháp  │ ✏️ 🗑️ 📤      │
│  Top 10 dự án nổi bật    │ Dự án      │ 📦 Lưu trữ│ ✏️ 🗑️ 📤      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Testing Scenarios

| Scenario | Expected |
|----------|----------|
| Create article with empty title | 400 error: "Title is required" |
| Create article with duplicate title | 409 error code `15002` |
| Public user views DRAFT article by slug | 400 error code `15003` |
| Public user views non-existent slug | 404 error code `15001` |
| Filter news by `POLICY` category | Only policy articles returned |
| Search keyword `"đầu tư"` | Articles matching in title/summary |
| Get newest with `limit=5` | Max 5 articles, newest first |
| Publish → Unpublish → Publish | `publishedAt` preserved from first publish |
| Delete article | 200, article permanently removed |
