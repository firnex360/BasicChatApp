const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
admin.initializeApp();

// Firestore-triggered function: send FCM on new chat message
exports.sendChatNotification = functions.firestore
    .document("chats/{chatRoomId}/messages/{messageId}")
    .onCreate(async (snapshot, context) => {
      console.log("Function trigger for chatRId:", context.params.chatRoomId);
      console.log("Message ID:", context.params.messageId);
      const message = snapshot.data();

      // Enhanced logging of the message data
      console.log("📨 Full message data:", JSON.stringify(message, null, 2));

      // Safety check
      if (!message) {
        console.log("❌ No message data found in snapshot.");
        return;
      }

      const receiverUid = message.receiver;
      const senderUid = message.sender;
      const senderName = message.senderName || "Someone";
      let text = message.text || "";

      console.log("👤 Sender UID:", senderUid);
      console.log("👤 Receiver UID:", receiverUid);
      console.log("👤 Sender Name:", senderName);
      console.log("💬 Message Text:", text);

      // Check if the message is an image and adjust the body
      if (text === "[Image]" && message.imageUrl) {
        text = `${senderName} sent an image.`;
        console.log("🖼️ Image message detected, updated text:", text);
      }

      if (!receiverUid) {
        console.log("❌ Message is missing receiverUid.");
        return;
      }

      // Don't send notification to self
      if (senderUid === receiverUid) {
        console.log("Sender and receiver are the same, skipping notification");
        return;
      }

      try {
        console.log("🔍 Looking up user document for receiver:", receiverUid);
        // Get the receiver's FCM token
        const userDoc = await admin.firestore()
            .collection("users")
            .doc(receiverUid)
            .get();

        if (!userDoc.exists) {
          console.log(`❌ User document for UID ${receiverUid} does not exist.`);
          return;
        }

        const userData = userDoc.data();
        console.log("👤 User data:", JSON.stringify(userData, null, 2));
        const fcmToken = userDoc.get("fcmToken");
        console.log("🔑 FCM Token found:", fcmToken ? "✅ Yes" : "❌ No");
        if (fcmToken) {
          console.log("FCM Token:", fcmToken.substring(0, 20) + "...");
        }

        if (!fcmToken) {
          console.log(`❌ No FCM token for user ${receiverUid}`);
          return;
        }

        // Updated message payload using v2 API
        const messagePayload = {
          token: fcmToken,
          notification: {
            title: senderName,
            body: text,
          },
          data: {
            chatRoomId: context.params.chatRoomId,
            senderUid: message.sender || "",
            receiverUid: receiverUid,
            messageType: "chat",
          },
          android: {
            notification: {
              channelId: "chat_notifications",
              priority: "high",
              defaultSound: true,
              defaultVibrateTimings: true,
            },
          },
        };

        // Send notification using v2 API
        console.log(`Send n w pylod:`, JSON.stringify(messagePayload, null, 2));
        const response = await admin.messaging().send(messagePayload);
        console.log("✅ Notification sent successfully!");
        console.log("📊 Response:", response);
      } catch (err) {
        console.error("💥 Error sending notification:", err);
        // Enhanced error logging
        if (err.code) {
          console.error("💥 Error code:", err.code);
        }
        if (err.message) {
          console.error("💥 Error message:", err.message);
        }
        if (err.details) {
          console.error("💥Error detail:", JSON.stringify(err.details, null, 2));
        }
        // Check for common FCM errors
        if (err.code === "messaging/registration-token-not-registered") {
          console.error("🔑 FCM token is invalid or expired");
        } else if (err.code === "messaging/invalid-registration-token") {
          console.error("🔑 FCM token format is invalid");
        } else if (err.code === "messaging/mismatched-credential") {
          console.error("🔐 Firebase project credentials mismatch");
        }
      }
    });
