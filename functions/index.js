const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

// ⚠️ باید با محل (Location) دیتابیس Firestore هم‌خوان باشد.
// مثلاً nam5 → "us-central1" ، eur3 → "europe-west1"
const REGION = "us-central1";

/** ارسال اعلان به یک کاربر (توکن از users/{id}.fcmToken خوانده می‌شود) */
async function notify(userId, title, body, data = {}) {
  if (!userId) return;
  const snap = await db.collection("users").doc(userId).get();
  const token = snap.get("fcmToken");
  if (!token) return;

  try {
    await getMessaging().send({
      token,
      data: { title, body, ...data }, // مقادیر data باید رشته باشند
      android: { priority: "high" },
    });
  } catch (e) {
    if (
      e.code === "messaging/registration-token-not-registered" ||
      e.code === "messaging/invalid-registration-token"
    ) {
      await snap.ref.update({ fcmToken: "" }); // توکن منقضی
    } else {
      console.error("FCM error", e);
    }
  }
}

// ۱) بیمار درخواست ثبت کرد → اعلان به پزشک
exports.onRequestCreated = onDocumentCreated(
  { document: "requests/{rid}", region: REGION },
  async (event) => {
    const r = event.data.data();
    await notify(
      r.doctorId,
      "درخواست جدید در کارتابل",
      `${r.patientName} برای شما درخواست مشاوره ثبت کرد`,
      { requestId: event.params.rid }
    );
  }
);

// ۲) پزشک وضعیت را تغییر داد → اعلان به بیمار
exports.onRequestUpdated = onDocumentUpdated(
  { document: "requests/{rid}", region: REGION },
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();
    if (before.status === after.status) return;

    const texts = {
      ACCEPTED: `${after.doctorName} درخواست شما را پذیرفت. می‌توانید گفتگو را شروع کنید.`,
      REJECTED: `${after.doctorName} درخواست شما را رد کرد.`,
      COMPLETED: `مشاوره‌ی شما با ${after.doctorName} تکمیل شد.`,
    };
    const body = texts[after.status];
    if (!body) return;

    await notify(after.patientId, "وضعیت درخواست شما", body, {
      requestId: event.params.rid,
    });
  }
);

// ۳) پیام جدید در چت → اعلان به طرف مقابل
// (به‌دلیل حساسیت اطلاعات پزشکی، متن پیام در اعلان نمایش داده نمی‌شود)
exports.onMessageCreated = onDocumentCreated(
  { document: "requests/{rid}/messages/{mid}", region: REGION },
  async (event) => {
    const m = event.data.data();
    const reqSnap = await db.collection("requests").doc(event.params.rid).get();
    const r = reqSnap.data();
    if (!r) return;

    const fromDoctor = m.senderId === r.doctorId;
    const target = fromDoctor ? r.patientId : r.doctorId;
    const senderName = fromDoctor ? r.doctorName : r.patientName;

    await notify(target, senderName, "پیام جدید ارسال کرد", {
      requestId: event.params.rid,
    });
  }
);
