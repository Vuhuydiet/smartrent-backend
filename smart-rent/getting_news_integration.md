# News & Blog API - Frontend Integration Guide

## 📋 Overview

This document provides comprehensive guidance for frontend developers to integrate the News & Blog feature into the SmartRent application. The API provides both **public endpoints** for end users and **admin endpoints** for content management.

### Use Cases
- **Public News Listing Page**: Display published news and blog posts with pagination and filtering
- **News Detail Page**: Show full article content with related posts
- **Admin Content Management**: Create, edit, publish, and manage news articles
- **Category Filtering**: Filter news by category (NEWS, BLOG, MARKET_TREND, GUIDE, ANNOUNCEMENT)
- **Tag-based Discovery**: Find related content through tags
- **Full-text Search**: Search articles by keywords in title and summary

---

## 🔌 API Endpoints

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/news` | Get paginated list of published news |
| GET | `/v1/news/{slug}` | Get news detail by URL-friendly slug |

### Admin Endpoints (Authentication Required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/admin/news` | Create a new news/blog post |
| PUT | `/v1/admin/news/{newsId}` | Update an existing news post |
| POST | `/v1/admin/news/{newsId}/publish` | Publish a news post |
| POST | `/v1/admin/news/{newsId}/unpublish` | Unpublish a news post |
| POST | `/v1/admin/news/{newsId}/archive` | Archive a news post |
| DELETE | `/v1/admin/news/{newsId}` | Delete a news post |
| GET | `/v1/admin/news` | Get all news (admin view) |
| GET | `/v1/admin/news/{newsId}` | Get news by ID (admin view) |

---

## 📝 Request Examples

### 1. Get Published News List (Public)

**Endpoint**: `GET /v1/news`

**Query Parameters**:
- `page` (optional): Page number, 1-based (default: 1)
- `size` (optional): Items per page (default: 20)
- `category` (optional): Filter by category (NEWS, BLOG, MARKET_TREND, GUIDE, ANNOUNCEMENT)
- `tag` (optional): Filter by tag
- `keyword` (optional): Search keyword (searches in title and summary)

**Example Requests**:

```bash
# Get first page of all published news
curl -X GET "https://api.smartrent.com/v1/news?page=1&size=20"

# Filter by category
curl -X GET "https://api.smartrent.com/v1/news?category=GUIDE&page=1&size=10"

# Search by keyword
curl -X GET "https://api.smartrent.com/v1/news?keyword=rental%20tips&page=1"

# Filter by tag
curl -X GET "https://api.smartrent.com/v1/news?tag=housing&page=1"

# Combine filters
curl -X GET "https://api.smartrent.com/v1/news?category=BLOG&keyword=apartment&page=1&size=15"
```

**JavaScript/TypeScript Example**:

```typescript
// Fetch news list with filters
async function fetchNewsList(params: {
  page?: number;
  size?: number;
  category?: string;
  tag?: string;
  keyword?: string;
}) {
  const queryParams = new URLSearchParams();
  
  if (params.page) queryParams.append('page', params.page.toString());
  if (params.size) queryParams.append('size', params.size.toString());
  if (params.category) queryParams.append('category', params.category);
  if (params.tag) queryParams.append('tag', params.tag);
  if (params.keyword) queryParams.append('keyword', params.keyword);
  
  const response = await fetch(
    `https://api.smartrent.com/v1/news?${queryParams.toString()}`
  );
  
  return await response.json();
}

// Usage
const newsData = await fetchNewsList({
  page: 1,
  size: 20,
  category: 'GUIDE'
});
```

### 2. Get News Detail by Slug (Public)

**Endpoint**: `GET /v1/news/{slug}`

**Example Requests**:

```bash
# Get news detail
curl -X GET "https://api.smartrent.com/v1/news/top-10-tips-for-finding-the-perfect-rental"
```

**JavaScript/TypeScript Example**:

```typescript
async function fetchNewsDetail(slug: string) {
  const response = await fetch(`https://api.smartrent.com/v1/news/${slug}`);
  
  if (!response.ok) {
    if (response.status === 404) {
      throw new Error('News not found');
    }
    if (response.status === 400) {
      throw new Error('News is not published');
    }
    throw new Error('Failed to fetch news');
  }
  
  return await response.json();
}

// Usage
const newsDetail = await fetchNewsDetail('top-10-tips-for-finding-the-perfect-rental');
```

### 3. Create News (Admin)

**Endpoint**: `POST /v1/admin/news`

**Headers**:
- `Authorization: Bearer {access_token}`
- `Content-Type: application/json`

**Request Body**:

```json
{
  "title": "Top 10 Tips for Finding the Perfect Rental",
  "summary": "Discover the best strategies for finding your ideal rental property",
  "content": "<h1>Introduction</h1><p>Finding the perfect rental can be challenging...</p>",
  "category": "GUIDE",
  "tags": "rental,tips,guide,housing",
  "thumbnailUrl": "https://example.com/images/rental-tips.jpg",
  "metaTitle": "Top 10 Rental Tips - SmartRent Guide",
  "metaDescription": "Expert tips for finding your perfect rental property",
  "metaKeywords": "rental tips, apartment hunting, housing guide"
}
```

**Example Request**:

```bash
curl -X POST "https://api.smartrent.com/v1/admin/news" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Top 10 Tips for Finding the Perfect Rental",
    "summary": "Discover the best strategies",
    "content": "<p>Full content here...</p>",
    "category": "GUIDE",
    "tags": "rental,tips,guide"
  }'
```

**TypeScript Example**:

```typescript
async function createNews(newsData: NewsCreateRequest, accessToken: string) {
  const response = await fetch('https://api.smartrent.com/v1/admin/news', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(newsData)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }

  return await response.json();
}
```

### 4. Update News (Admin)

**Endpoint**: `PUT /v1/admin/news/{newsId}`

**Request Body** (all fields optional):

```json
{
  "title": "Updated Title",
  "summary": "Updated summary",
  "content": "<p>Updated content...</p>",
  "category": "BLOG",
  "tags": "updated,tags",
  "thumbnailUrl": "https://example.com/new-image.jpg"
}
```

### 5. Publish News (Admin)

**Endpoint**: `POST /v1/admin/news/{newsId}/publish`

```bash
curl -X POST "https://api.smartrent.com/v1/admin/news/1/publish" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 6. Delete News (Admin)

**Endpoint**: `DELETE /v1/admin/news/{newsId}`

```bash
curl -X DELETE "https://api.smartrent.com/v1/admin/news/1" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 📤 Response Examples

### News List Response

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "news": [
      {
        "newsId": 1,
        "title": "Top 10 Tips for Finding the Perfect Rental",
        "slug": "top-10-tips-for-finding-the-perfect-rental",
        "summary": "Discover the best strategies for finding your ideal rental property",
        "category": "GUIDE",
        "tags": ["rental", "tips", "guide", "housing"],
        "thumbnailUrl": "https://example.com/image.jpg",
        "publishedAt": "2024-01-15T10:30:00",
        "authorName": "Admin User",
        "viewCount": 1250,
        "createdAt": "2024-01-15T09:00:00"
      }
    ],
    "totalItems": 150,
    "currentPage": 1,
    "pageSize": 20,
    "totalPages": 8
  }
}
```

### News Detail Response

```json
{
  "code": "999999",
  "message": null,
  "data": {
    "newsId": 1,
    "title": "Top 10 Tips for Finding the Perfect Rental",
    "slug": "top-10-tips-for-finding-the-perfect-rental",
    "summary": "Discover the best strategies for finding your ideal rental property",
    "content": "<h1>Introduction</h1><p>Full HTML content here...</p>",
    "category": "GUIDE",
    "tags": ["rental", "tips", "guide", "housing"],
    "thumbnailUrl": "https://example.com/image.jpg",
    "status": "PUBLISHED",
    "publishedAt": "2024-01-15T10:30:00",
    "authorId": "admin-123",
    "authorName": "Admin User",
    "viewCount": 1251,
    "metaTitle": "Top 10 Rental Tips - SmartRent Guide",
    "metaDescription": "Expert tips for finding your perfect rental property",
    "metaKeywords": "rental tips, apartment hunting, housing guide",
    "createdAt": "2024-01-15T09:00:00",
    "updatedAt": "2024-01-15T10:30:00",
    "relatedNews": [
      {
        "newsId": 2,
        "title": "Understanding Rental Contracts",
        "slug": "understanding-rental-contracts",
        "summary": "Everything you need to know about rental agreements",
        "category": "GUIDE",
        "tags": ["rental", "contracts", "legal"],
        "thumbnailUrl": "https://example.com/contracts.jpg",
        "publishedAt": "2024-01-14T15:00:00",
        "authorName": "Admin User",
        "viewCount": 890,
        "createdAt": "2024-01-14T14:00:00"
      }
    ]
  }
}
```

---

## 🎯 Pagination & Filtering Guide

### Understanding Pagination

The API uses **1-based page numbering** for easier frontend integration:

- `page=1` returns the first page
- `page=2` returns the second page
- etc.

### Implementing Pagination in Frontend

**React Example with State Management**:

```typescript
import { useState, useEffect } from 'react';

interface NewsListState {
  news: NewsItem[];
  totalItems: number;
  currentPage: number;
  pageSize: number;
  totalPages: number;
  loading: boolean;
}

function NewsListPage() {
  const [state, setState] = useState<NewsListState>({
    news: [],
    totalItems: 0,
    currentPage: 1,
    pageSize: 20,
    totalPages: 0,
    loading: false
  });

  const [filters, setFilters] = useState({
    category: '',
    tag: '',
    keyword: ''
  });

  useEffect(() => {
    loadNews();
  }, [state.currentPage, filters]);

  async function loadNews() {
    setState(prev => ({ ...prev, loading: true }));

    try {
      const response = await fetchNewsList({
        page: state.currentPage,
        size: state.pageSize,
        ...filters
      });

      setState(prev => ({
        ...prev,
        news: response.data.news,
        totalItems: response.data.totalItems,
        totalPages: response.data.totalPages,
        loading: false
      }));
    } catch (error) {
      console.error('Failed to load news:', error);
      setState(prev => ({ ...prev, loading: false }));
    }
  }

  function handlePageChange(newPage: number) {
    setState(prev => ({ ...prev, currentPage: newPage }));
  }

  function handleFilterChange(newFilters: Partial<typeof filters>) {
    setFilters(prev => ({ ...prev, ...newFilters }));
    setState(prev => ({ ...prev, currentPage: 1 })); // Reset to first page
  }

  return (
    <div>
      {/* Filter UI */}
      <NewsFilters onFilterChange={handleFilterChange} />

      {/* News List */}
      {state.loading ? (
        <LoadingSpinner />
      ) : (
        <NewsList items={state.news} />
      )}

      {/* Pagination */}
      <Pagination
        currentPage={state.currentPage}
        totalPages={state.totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  );
}
```

### Infinite Scroll Implementation

**React Example**:

```typescript
import { useState, useEffect, useRef } from 'react';

function InfiniteScrollNewsList() {
  const [news, setNews] = useState<NewsItem[]>([]);
  const [page, setPage] = useState(1);
  const [hasMore, setHasMore] = useState(true);
  const [loading, setLoading] = useState(false);
  const observerTarget = useRef(null);

  useEffect(() => {
    loadMore();
  }, [page]);

  useEffect(() => {
    const observer = new IntersectionObserver(
      entries => {
        if (entries[0].isIntersecting && hasMore && !loading) {
          setPage(prev => prev + 1);
        }
      },
      { threshold: 1 }
    );

    if (observerTarget.current) {
      observer.observe(observerTarget.current);
    }

    return () => observer.disconnect();
  }, [hasMore, loading]);

  async function loadMore() {
    if (loading) return;

    setLoading(true);
    try {
      const response = await fetchNewsList({ page, size: 20 });

      setNews(prev => [...prev, ...response.data.news]);
      setHasMore(page < response.data.totalPages);
    } catch (error) {
      console.error('Failed to load more news:', error);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div>
      <NewsList items={news} />
      {loading && <LoadingSpinner />}
      <div ref={observerTarget} />
    </div>
  );
}
```

---

## ⚠️ Common Errors

### Error Response Format

All errors follow this structure:

```json
{
  "code": "ERROR_CODE",
  "message": "Error description",
  "data": null
}
```

### Error Codes

| Code | HTTP Status | Description | Solution |
|------|-------------|-------------|----------|
| `15001` | 404 | News not found | Check if the slug/ID is correct |
| `15002` | 409 | News slug already exists | Use a different title or manually set a unique slug |
| `15003` | 400 | News is not published | Only published news are accessible via public endpoints |
| `2009` | 400 | Invalid page | Page number must be >= 1 |
| `2010` | 400 | Invalid page size | Page size must be > 0 |
| `5001` | 401 | Unauthenticated | Provide valid JWT token for admin endpoints |
| `6001` | 403 | Unauthorized | User doesn't have admin permissions |

### Error Handling Examples

**TypeScript**:

```typescript
async function fetchNewsWithErrorHandling(slug: string) {
  try {
    const response = await fetch(`https://api.smartrent.com/v1/news/${slug}`);
    const data = await response.json();

    if (!response.ok) {
      switch (data.code) {
        case '15001':
          // News not found - show 404 page
          throw new NotFoundError('This article does not exist');
        case '15003':
          // News not published - show message
          throw new Error('This article is not yet published');
        default:
          throw new Error(data.message || 'Failed to load news');
      }
    }

    return data;
  } catch (error) {
    console.error('Error fetching news:', error);
    throw error;
  }
}
```

---

## 🎨 Best Practices for Frontend

### 1. Caching Strategy

**Recommended Approach**:

```typescript
// Use React Query for automatic caching
import { useQuery } from '@tanstack/react-query';

function useNewsList(filters: NewsFilters) {
  return useQuery({
    queryKey: ['news', filters],
    queryFn: () => fetchNewsList(filters),
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 30 * 60 * 1000, // 30 minutes
  });
}

function useNewsDetail(slug: string) {
  return useQuery({
    queryKey: ['news', slug],
    queryFn: () => fetchNewsDetail(slug),
    staleTime: 10 * 60 * 1000, // 10 minutes
    cacheTime: 60 * 60 * 1000, // 1 hour
  });
}
```

**Benefits**:
- Reduces unnecessary API calls
- Improves page load performance
- Better user experience with instant navigation

### 2. SEO Optimization

**Slug-based Routing**:

```typescript
// Next.js example
// pages/news/[slug].tsx
export async function getStaticPaths() {
  // Pre-render popular news pages at build time
  const popularNews = await fetchNewsList({ page: 1, size: 50 });

  return {
    paths: popularNews.data.news.map(item => ({
      params: { slug: item.slug }
    })),
    fallback: 'blocking' // Generate other pages on-demand
  };
}

export async function getStaticProps({ params }) {
  const newsDetail = await fetchNewsDetail(params.slug);

  return {
    props: { newsDetail },
    revalidate: 3600 // Revalidate every hour
  };
}
```

**Meta Tags**:

```tsx
import Head from 'next/head';

function NewsDetailPage({ newsDetail }) {
  const news = newsDetail.data;

  return (
    <>
      <Head>
        <title>{news.metaTitle || news.title}</title>
        <meta name="description" content={news.metaDescription || news.summary} />
        <meta name="keywords" content={news.metaKeywords || news.tags.join(', ')} />

        {/* Open Graph tags for social sharing */}
        <meta property="og:title" content={news.title} />
        <meta property="og:description" content={news.summary} />
        <meta property="og:image" content={news.thumbnailUrl} />
        <meta property="og:type" content="article" />
        <meta property="article:published_time" content={news.publishedAt} />
        <meta property="article:author" content={news.authorName} />

        {/* Twitter Card tags */}
        <meta name="twitter:card" content="summary_large_image" />
        <meta name="twitter:title" content={news.title} />
        <meta name="twitter:description" content={news.summary} />
        <meta name="twitter:image" content={news.thumbnailUrl} />
      </Head>

      <article>
        <h1>{news.title}</h1>
        <div dangerouslySetInnerHTML={{ __html: news.content }} />
      </article>
    </>
  );
}
```

### 3. Infinite Scroll vs Pagination

**When to use Infinite Scroll**:
- Mobile-first design
- Social media-like feed
- Continuous browsing experience

**When to use Pagination**:
- Desktop-focused design
- Users need to find specific content
- Better for SEO (each page has unique URL)

**Hybrid Approach** (Recommended):
```typescript
// Use pagination on desktop, infinite scroll on mobile
const isMobile = useMediaQuery('(max-width: 768px)');

return isMobile ? (
  <InfiniteScrollNewsList />
) : (
  <PaginatedNewsList />
);
```

### 4. Loading States

**Skeleton Screens**:

```tsx
function NewsListSkeleton() {
  return (
    <div className="news-list">
      {[1, 2, 3, 4, 5].map(i => (
        <div key={i} className="news-card skeleton">
          <div className="skeleton-image" />
          <div className="skeleton-title" />
          <div className="skeleton-text" />
          <div className="skeleton-text short" />
        </div>
      ))}
    </div>
  );
}

function NewsListPage() {
  const { data, isLoading } = useNewsList(filters);

  if (isLoading) return <NewsListSkeleton />;

  return <NewsList items={data.news} />;
}
```

### 5. Content Sanitization

**Important**: Always sanitize HTML content before rendering:

```typescript
import DOMPurify from 'dompurify';

function NewsContent({ htmlContent }: { htmlContent: string }) {
  const sanitizedContent = DOMPurify.sanitize(htmlContent, {
    ALLOWED_TAGS: ['h1', 'h2', 'h3', 'p', 'a', 'img', 'ul', 'ol', 'li', 'strong', 'em'],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'title', 'class']
  });

  return (
    <div
      className="news-content"
      dangerouslySetInnerHTML={{ __html: sanitizedContent }}
    />
  );
}
```

### 6. Related News Display

```tsx
function RelatedNews({ relatedNews }: { relatedNews: NewsItem[] }) {
  if (!relatedNews || relatedNews.length === 0) return null;

  return (
    <section className="related-news">
      <h2>Related Articles</h2>
      <div className="related-news-grid">
        {relatedNews.map(news => (
          <Link key={news.newsId} href={`/news/${news.slug}`}>
            <a className="related-news-card">
              <img src={news.thumbnailUrl} alt={news.title} />
              <h3>{news.title}</h3>
              <p>{news.summary}</p>
            </a>
          </Link>
        ))}
      </div>
    </section>
  );
}
```

---

## 📊 Data Models

### NewsCategory Enum

```typescript
enum NewsCategory {
  NEWS = 'NEWS',
  BLOG = 'BLOG',
  MARKET_TREND = 'MARKET_TREND',
  GUIDE = 'GUIDE',
  ANNOUNCEMENT = 'ANNOUNCEMENT'
}
```

### NewsStatus Enum

```typescript
enum NewsStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED'
}
```

### TypeScript Interfaces

```typescript
interface NewsItem {
  newsId: number;
  title: string;
  slug: string;
  summary: string;
  category: NewsCategory;
  tags: string[];
  thumbnailUrl: string;
  publishedAt: string;
  authorName: string;
  viewCount: number;
  createdAt: string;
}

interface NewsDetail extends NewsItem {
  content: string;
  status: NewsStatus;
  authorId: string;
  metaTitle?: string;
  metaDescription?: string;
  metaKeywords?: string;
  updatedAt: string;
  relatedNews: NewsItem[];
}

interface NewsListResponse {
  news: NewsItem[];
  totalItems: number;
  currentPage: number;
  pageSize: number;
  totalPages: number;
}

interface ApiResponse<T> {
  code: string;
  message: string | null;
  data: T;
}
```

---

## 🚀 Quick Start Checklist

- [ ] Set up API base URL in environment variables
- [ ] Implement authentication for admin endpoints
- [ ] Create news list page with pagination
- [ ] Create news detail page with slug routing
- [ ] Implement category filtering
- [ ] Add search functionality
- [ ] Set up proper SEO meta tags
- [ ] Implement error handling
- [ ] Add loading states and skeletons
- [ ] Sanitize HTML content before rendering
- [ ] Test on both desktop and mobile
- [ ] Implement caching strategy
- [ ] Add related news section
- [ ] Test admin CRUD operations

---

## 📞 Support

For questions or issues with the News & Blog API integration, please contact the backend team or refer to the Swagger documentation at `/swagger-ui.html`.

---

**Last Updated**: 2024-01-15
**API Version**: v1
**Document Version**: 1.0

