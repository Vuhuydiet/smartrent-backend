const crypto = require('crypto');

function generateVNPaySignature(secret, data) {
    const hmac = crypto.createHmac('sha512', secret);
    const signed = hmac.update(Buffer.from(data, 'utf-8')).digest('hex');
    return signed;
}

const vnpaySecret = "O7A33CE2QF0B3MIACK2IYWWT85E37FG4";
const vnpayData = "vnp_Amount=1000000&vnp_Command=pay&vnp_CreateDate=20240101120000&vnp_CurrCode=VND&vnp_IpAddr=127.0.0.1&vnp_Locale=vn&vnp_OrderInfo=Test+Payment&vnp_OrderType=other&vnp_ReturnUrl=http%3A%2F%2Flocalhost%2Fcallback&vnp_TmnCode=TEA5LGP4&vnp_TxnRef=123456&vnp_Version=2.1.0";

const vnpayHash = generateVNPaySignature(vnpaySecret, vnpayData);
console.log("VNPay Hash (Node.js):", vnpayHash);
