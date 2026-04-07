# ZaloPay Integration Guide for Frontend

This guide describes how to integrate ZaloPay payment into the SmartRent frontend application.

## 1. Overview of Payment Flow

1.  **Initiate Payment**: Frontend calls the backend API to create a membership transaction and obtain a ZaloPay payment URL.
2.  **Redirect**: Frontend redirects the user to the `paymentUrl` provided in the response.
3.  **Payment on ZaloPay**: The user completes the payment on the ZaloPay sandbox gateway.
4.  **Redirect Back**: ZaloPay redirects the user back to the `return_url` (configured in the backend as `https://www.smartrent.io.vn/payment/result`).
5.  **IPN Callback**: ZaloPay sends a server-to-server notification (IPN) to the backend to confirm the payment status.

---

## 2. API Endpoints

### Initiate Membership Purchase

Create a new membership transaction and get the ZaloPay payment URL.

*   **URL**: `/v1/memberships/initiate-purchase`
*   **Method**: `POST`
*   **Authentication**: Bearer Token required
*   **Request Body**:
    ```json
    {
      "membershipId": 1,
      "paymentProvider": "ZALOPAY"
    }
    ```

*   **Success Response (200 OK)**:
    ```json
    {
      "code": "0000",
      "message": "Success",
      "data": {
        "transactionRef": "cf2ba785-fe91-412b-9ed1-1e32773e9166",
        "paymentUrl": "https://sb-openapi.zalopay.vn/v2/create/order/...",
        "provider": "ZALOPAY",
        "amount": 700000,
        "currency": "VND",
        "createdAt": "2026-04-05T23:20:40",
        "expiresAt": "2026-04-05T23:35:40"
      }
    }
    ```

---

## 3. Frontend Implementation Steps

1.  **Call API**: Execute the `POST` request to `/v1/memberships/initiate-purchase`.
2.  **Handle Response**: Extract the `paymentUrl` from the response.
3.  **Redirect**:
    ```javascript
    if (response.data.paymentUrl) {
      window.location.href = response.data.paymentUrl;
    }
    ```
4.  **Handle Result Page**: The user will be redirected to `https://www.smartrent.io.vn/payment/result?orderId=...&amount=...&resultCode=...`
    *   `resultCode = 1`: Success
    *   `resultCode = 2`: Failed
    *   Frontend should show a success/failure message based on these parameters and can call a backend API to verify the transaction status if needed.

---

## 4. Sandbox Testing

To test ZaloPay in the sandbox environment:

1.  **App**: Download the **ZaloPay Sandbox** app on your phone (or use the web interface provided by the redirect).
2.  **Test Account**: Use the official ZaloPay sandbox test accounts or cards.
    *   **Card Number**: `970433` (then any 10-13 digits)
    *   **OTP**: `123456`
3.  **Environment Variables**: Ensure your `.env` contains:
    *   `ZALOPAY_APP_ID=2554`
    *   `ZALOPAY_KEY1=sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn`
    *   `ZALOPAY_KEY2=trMrHtvjo6myautxDUiAcYsVtaeQ8nhf`

---

## 5. Troubleshooting (Backend Logs)

If you encounter errors during testing, check the backend logs for:
- `ZaloPay MAC source`: The raw data being signed.
- `return_code`: 
    - `1`: Success
    - `2`: Transaction failed (usually due to invalid parameters or MAC)
- `app_trans_id`: Must be unique and format `yyMMdd_XXXX` (limit 40 chars).
