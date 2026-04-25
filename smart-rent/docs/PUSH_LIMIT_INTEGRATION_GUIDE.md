# SmartRent Push Limit Integration Guide (Next.js)

## 📌 Overview
To maintain a high-quality user experience and prevent listings from being immediately pushed down by other users, SmartRent enforces a **Global Listing Push Limit**. 

The system allows a maximum of **10 distinct listings** to hold the "Top Pushed" status at any given time (rolling 1-hour window). If the platform is currently at full capacity (10 listings are actively pushed), any new push attempts will be blocked, and the backend will calculate the exact remaining wait time until a slot becomes available.

This document outlines how the Next.js frontend should handle this specific scenario to provide a smooth "vibe coding" UI/UX.

---

## 🚨 Backend Error Response

When a user attempts to push a listing (either via membership quota or direct payment) and the global limit is reached, the backend will return an HTTP `429 Too Many Requests` status code.

### Expected JSON Response:
```json
{
  "code": "18001",
  "message": "Hệ thống đang quá tải yêu cầu đẩy tin. Vui lòng chờ 15 phút nữa để tiếp tục đẩy tin."
}
```
*Note: The number of minutes (`15` in the example above) is dynamically calculated by the backend based on the exact expiration time of the oldest pushed listing currently occupying a slot.*

---

## 💻 Next.js Integration Guide

### 1. Handling the Error (API Layer)

If you are using `axios` or standard `fetch` in your Next.js application, you should catch the `429` status code and specifically look for the `18001` error code.

```typescript
// utils/apiClient.ts (Example Axios Setup)
import axios from 'axios';
import { toast } from 'react-hot-toast'; // or your preferred toast library

const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;

      // Intercept the Push Limit Error globally
      if (status === 429 && data?.code === '18001') {
        // You can dispatch a global event, show a toast, or trigger a modal here
        toast.error(data.message, {
          duration: 5000,
          icon: '⏳',
          style: {
            borderRadius: '10px',
            background: '#333',
            color: '#fff',
          },
        });
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

### 2. Handling the Error (Component Level)

If you prefer to handle the popup strictly inside the Push/Boost Component (e.g., inside a Modal or specific Page):

```tsx
// components/PushListingButton.tsx
import { useState } from 'react';
import apiClient from '@/utils/apiClient';
import PushLimitModal from '@/components/modals/PushLimitModal';

export default function PushListingButton({ listingId }) {
  const [isLoading, setIsLoading] = useState(false);
  const [limitMessage, setLimitMessage] = useState<string | null>(null);

  const handlePushListing = async () => {
    setIsLoading(true);
    setLimitMessage(null);

    try {
      const response = await apiClient.post('/v1/push', {
        listingId,
        useMembershipQuota: true // or false for direct payment
      });
      
      // Handle Success
      alert('Đẩy tin thành công!');
      
    } catch (error: any) {
      // Specifically catch the Push Limit exception
      if (error.response?.status === 429 && error.response?.data?.code === '18001') {
        // Extract the exact message sent by the backend which contains the wait time
        setLimitMessage(error.response.data.message);
      } else {
        // Handle other errors
        alert('Đã có lỗi xảy ra.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <>
      <button 
        onClick={handlePushListing} 
        disabled={isLoading}
        className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
      >
        {isLoading ? 'Đang xử lý...' : 'Đẩy tin ngay'}
      </button>

      {/* Vibe UI Pop-up for the user */}
      {limitMessage && (
        <PushLimitModal 
          message={limitMessage} 
          onClose={() => setLimitMessage(null)} 
        />
      )}
    </>
  );
}
```

### 3. Recommended UI/UX

For the best user experience, instead of a standard boring alert, consider creating a beautiful popup (`PushLimitModal`) that conveys the message gracefully. 

**Suggested UI Elements:**
- **Icon:** A sandglass (⏳) or a queue/waiting icon.
- **Title:** "Danh sách đẩy tin đang đầy" (Push list is full)
- **Body:** Render the dynamic `message` returned from the API (e.g., *"Hệ thống đang quá tải yêu cầu đẩy tin. Vui lòng chờ 15 phút nữa để tiếp tục đẩy tin."*)
- **Action:** A simple "Đã hiểu" (Got it) button to close the modal.

---

## 🛠️ Testing Locally

While developing the frontend, you don't need to manually push 10 listings to trigger this error. You can ask the backend team to temporarily lower the limits in the backend `application-local.yaml` file to:
```yaml
app:
  push:
    limit:
      enabled: true
      max-listings: 1  # Set to 1 for quick testing
      window-minutes: 5 # Set to 5 minutes
```
This allows you to test the `18001` error behavior and ensure your UI popup functions beautifully after just 1 successful push!
