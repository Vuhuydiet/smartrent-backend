const crypto = require('crypto');

async function testZaloPay() {
  const appId = "2554";
  const key1 = "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn";
  
  const appTransId = "240405_12345678"; // yyMMdd_xxxxxxxx
  const appUser = "guest";
  const amount = 700000;
  const appTime = Date.now();
  const embedDataStr = "{}";
  const item = "[]";
  const description = "Thanh toan don hang #12345678";
  
  // MAC formula: app_id|app_trans_id|app_user|amount|app_time|embed_data|item
  const dataForMac = `${appId}|${appTransId}|${appUser}|${amount}|${appTime}|${embedDataStr}|${item}`;
  console.log("Data for MAC:", dataForMac);
  
  const hmac = crypto.createHmac('sha256', key1);
  hmac.update(dataForMac);
  const mac = hmac.digest('hex');
  console.log("MAC:", mac);
  
  const orderReq = {
      app_id: 2554, // Must be Number
      app_trans_id: appTransId,
      app_user: appUser,
      app_time: appTime,
      item: item,
      embed_data: embedDataStr,
      amount: amount,
      description: description,
      bank_code: "",
      mac: mac
  };
  
  console.log("Request Payload:", JSON.stringify(orderReq, null, 2));
  
  try {
      const resp = await fetch("https://sb-openapi.zalopay.vn/v2/create", {
          method: "POST",
          headers: {
              "Content-Type": "application/json"
          },
          body: JSON.stringify(orderReq)
      });
      const result = await resp.json();
      console.log("Response:", result);
  } catch (e) {
      console.error("Error:", e);
  }
}

testZaloPay();
