package com.smartrent;

import com.smartrent.utility.PaymentUtil;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VNPaySignatureTest {

    @Test
    public void testVNPaySignature() {
        String secretKey = "O7A33CE2QF0B3MIACK2IYWWT85E37FG4";
        String data = "vnp_Amount=1000000&vnp_Command=pay&vnp_CreateDate=20240101120000&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Test+Payment&vnp_OrderType=other&vnp_ReturnUrl=http%3A%2F%2Flocalhost%2Fcallback&vnp_TmnCode=TEA5LGP4&vnp_TxnRef=123456&vnp_Version=2.1.0";
        
        String expectedHash = PaymentUtil.hmacSHA512(secretKey, data);
        System.out.println("Generated Hash: " + expectedHash);
        
        // This is a placeholder test. In a real scenario, you'd compare with a known valid hash from VNPay dashboard/docs.
    }
}
