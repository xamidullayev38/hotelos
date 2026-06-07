---
title: "HotelOS: Real Vaqtli Mehmonxona Boshqaruv Tizimini Qurish"
author: "Saidkamol Xamidullayev"
date: "2026-yil 7-iyun"
lang: uz
---

\newpage

# Muqova sahifasi

**Talabaning to'liq ismi:** Saidkamol Xamidullayev

**Talaba ID:** [TALABA_ID]

**Malaka:** Pearson BTEC Raqamli Texnologiyalar bo'yicha Oliy Milliy Diplomlar (HND)

**Modul nomi:** 4-Modul: Dasturlash (H/618/7388, 4-daraja, 15 kredit)

**Vazifa nomi:** HotelOS — Real Vaqtli Mehmonxona Boshqaruv Tizimini Qurish

**Baholovchi:** [BAHOLOVCHI_ISMI]

**Topshirish sanasi:** 2026-yil 7-iyun

**Manba kod:** https://github.com/xamidullayev38/hotelos

\newpage

# Mundarija

1. 1-Vazifa — Algoritmlar va Kod Jarayoni
2. 2-Vazifa — HotelOSda Dasturlash Paradigmalari
3. 3-Vazifa — HotelOSni Qurish
4. 4-Vazifa — Disk Raskadrovka va Kodlash Standartlari
5. Adabiyotlar ro'yxati
6. Qo'shimchalar

\newpage

# 1-Vazifa — Algoritmlar va Kod Jarayoni

## 1.1 Xona Tayinlash Algoritmi

### Bosqichma-bosqich tavsif

Xona tayinlash algoritmi `RoomAssignmentService.assign(roomType, floorPreference, proximityPreference)` metodida amalga oshirilgan. Algoritm besh ketma-ket bosqichdan iborat va har biri kandidatlar to'plamini bir oz qisqartiradi yoki tartiblaydi.

**1-bosqich — turi bo'yicha filtr.** Tizim barcha xonalar ro'yxatini Reception bazasidan oladi (`RoomRepository.findAll()`) va Stream API yordamida xona turi mehmon bron qilgan turga teng bo'lmagan har bir xonani chiqarib tashlaydi. Bu qadam qattiq cheklov sifatida ishlaydi: agar mehmon SUITE so'ragan bo'lsa, hatto DOUBLE xonasi mavjud bo'lsa ham, u tanlanmaydi.

**2-bosqich — tozalik holati bo'yicha filtr.** Birinchi filtrdan o'tgan xonalardan faqat `RoomStatus.CLEAN` holatidagilari qoladi. `DIRTY` (tozalanmagan), `CLEANING` (tozalanmoqda), `MAINTENANCE` (texnik xizmatda) va `OCCUPIED` (band) holatidagi xonalar chiqarib tashlanadi. Bu xavfsizlik kafolati: tizim hech qachon tozalanmagan yoki nosoz xonaga mehmonni joylashtirmasligini ta'minlaydi.

**3-bosqich — bo'shliq tekshiruvi.** Agar bu nuqtada kandidatlar ro'yxati bo'sh bo'lsa, algoritm `Optional.empty()` qaytaradi va kontroller mehmonga `ROOMS_UNAVAILABLE` xatoligini (HTTP 409) qaytaradi. Tizim ishdan chiqmaydi; mehmonga kutish ro'yxati yoki muqobil xona turi taklif etiladi.

**4-bosqich — qavat afzalligi (yumshoq bias).** Agar mehmon `floorPreference` ko'rsatgan bo'lsa, algoritm o'sha qavatdagi xonalarni saralab oladi. Agar bunday xonalar mavjud bo'lsa, ular bilan ishlaydi. Aks holda, **kandidatlar ro'yxatini o'zgartirmasdan qoldiradi** — qavat qattiq cheklov emas, balki afzallikdir. Bu loyihaning muhim qarorlaridan biri: agar 3-qavatda DOUBLE xona bo'lmasa-yu, 2-qavatda bo'lsa, mehmon bo'ron etib tashlanmaydi.

**5-bosqich — eng uzoq toza birinchi.** Qolgan kandidatlar `Comparator.comparing(Room::getLastCleanedAt)` bo'yicha o'sish tartibida tartiblanadi. Eng erta tozalangan xona ro'yxat boshida joylashadi. Bu adolatli aylanishni ta'minlaydi: hech qaysi xona bir necha hafta band bo'lmasdan, boshqalari esa hech qachon ishlatilmasligi mumkin emas.

**6-bosqich — yaqinlik tie-break.** Faqat mehmon `proximityPreference` (LIFT yoki STAIRS) ko'rsatgan bo'lsa, komparator zanjiriga ikkinchi mezon qo'shiladi. LIFT uchun `metresFromLift` ASC, STAIRS uchun esa DESC. Bir xil tozalik vaqtidagi xonalar orasidan eng yaqinroq yoki eng uzog'i tanlanadi.

Algoritm `candidates.stream().min(comparator)` orqali yakuniy tanlovni qaytaradi.

### Blok-sxema

`1-Rasm` da xona tayinlash algoritmining to'liq blok-sxemasi keltirilgan. Har bir romb qaror nuqtasini, to'rtburchak jarayonni, yumaloqlangan to'rtburchak esa boshlanish/tugatish nuqtalarini ifodalaydi.

![1-Rasm: Xona tayinlash algoritmi blok-sxemasi](../algorithms/room-assignment-flowchart.md)

### Asoslash va alternativalar

Tanlangan kaskadli filtr yondashuvi uchta sababga ko'ra eng yaxshi tanlov edi:

**Birinchidan, izchillik.** Har bir qadam aniq mantiqiy ma'noga ega: turi mos kelmaydigan xona bron qilingan turdagi xona emas. Tozalanmagan xonaga mehmon joylashtirilmaydi. Bu qoidalar bizning real biznes qoidalarimizga to'g'ridan-to'g'ri mos keladi. **Vaznlangan ball yondashuvi** (har bir mezonga vazn berish va yig'ish) rad etildi, chunki ballarni asoslash qiyin ("Nima uchun tozalik 0.7, qavat 0.3?"). Filtr yondashuvi tushuntirish va disk raskadrovka qilish osonroq.

**Ikkinchidan, samaradorlik.** 10 ta xona uchun esa farq sezilmaydi, lekin 120 xonali real mehmonxonada qattiq filtrlar avval qo'llanilsa, sortlash uchun kandidatlar soni dramatik ravishda kamayadi. Hozirgi amalga oshirishda O(n log n) sortlash faqat tozalik filtri o'tgandan keyin ishga tushadi.

**Uchinchidan, kengaytirish mumkinligi.** Yangi mezon qo'shish (masalan, qarshi yotgan xonalar afzalligi) faqat filtr yoki komparator zanjiriga qo'shimcha bosqich qo'shishni talab qiladi. Mavjud kod o'zgartirilmaydi.

**Tasodifiy tanlov** alternativasi ham ko'rib chiqildi (mos xonalardan tasodifiy birini tanlash), lekin u BTEC topshirig'idagi "eng uzoq toza birinchi" talabini buzar edi.

## 1.2 Qo'shimcha Algoritmlar

### Hisob-kitob algoritmi (`BillingService.computeBill`)

Hisob-kitob algoritmi mehmon check-out qilganida ishga tushadi va to'rtta tarkibiy qismdan iborat yakuniy hisobni hisoblaydi.

**1-bosqich — haqiqiy tunlar.** `Duration.between(checkInAt, checkOutMoment).toHours() / 24` yuqori yaxlitlanadi. Erta check-out holatida (mehmon 3 tun bron qilib 2 tundan keyin chiqsa) faqat haqiqiy tunlar haqlanadi. Algoritm `Math.max(1, ...)` bilan minimal bir tunni majburlaydi — bir kun ichida check-in va check-out bo'lsa ham, mehmonga bepul qolish bermaydi.

**2-bosqich — xona summasi.** `nightlyRate × actualNights`, `BigDecimal` arifmetikasida, `HALF_UP` yaxlitlash bilan ikki kasr raqamiga. `double` o'rniga `BigDecimal` ishlatish — pul masalasida juda muhim qaror: `double` 260.00 × 3 ni 780.0000001 sifatida ko'rsatishi mumkin, bu esa hisobotlarda noaniqlik keltirib chiqaradi.

**3-bosqich — buyurtma to'lovlari.** `RoomChargeRepository.findByRoomNumber(roomNumber)` orqali xonadagi barcha xona xizmati buyurtmalari yig'iladi. Bu to'lovlar Room Service `order.created` hodisasini nashr etganda Reception tomonidan avtomatik tarzda qayd etiladi.

**4-bosqich — qo'shimcha to'lovlar va chegirma.** `guest.extraCharges` (minibar, kech check-out) qo'shiladi. Chegirma `min(guest.discount, subtotal)` bilan cheklanadi — chegirma hech qachon hisob summasidan oshmasligi kerak, aks holda mehmonxona mehmonga pul to'lashi kerak bo'ladi.

Yakuniy hisob: `grandTotal = roomTotal + chargesTotal + extras − discount`.

Chegaraviy holatlar:

| Holat | Xatti-harakat |
|---|---|
| Erta check-out (bron 3, qoldi 1) | `actualNights = 1` |
| Bir kunda kelish va ketish | `actualNights = 1` (majburiy minimum) |
| Buyurtma yo'q | `chargesTotal = 0.00` |
| Chegirma > subtotal | `discount = subtotal`, `grandTotal = 0.00` |

### Texnik xizmat uchun ustuvorlik navbat algoritmi

Ustuvorlik navbati `MaintenancePriorityQueue` klassida amalga oshirilgan va `java.util.PriorityQueue<MaintenanceIssue>` (ikkilik heap) ustida qurilgan. Komparator zanjir ikki kalitdan iborat:

1. **Asosiy kalit:** `Urgency.ordinal()` ASC. Enum `CRITICAL, HIGH, NORMAL, LOW` tartibida e'lon qilingan, shuning uchun `CRITICAL` ning ordinali 0 va heap tepasiga ko'tariladi.
2. **Tie-break:** `submittedAt` ASC. Bir xil shoshilinchlikdagi muammolar orasida birinchi topshirilgan birinchi xizmat ko'rsatiladi — bu topshiriqning aniq talabi.

`offer(issue)` va `poll()` operatsiyalari O(log n) bajariladi. Sortlangan ArrayList alternativi rad etildi: kiritish O(n) bo'lar edi va texnik xizmat hisobotlari uzluksiz keladigan tizimda bu og'irlik orttirardi.

Texniklar **round-robin** usulida tayinlanadi: `ArrayDeque<String>` ichida texniklar ro'yxati saqlanadi, har safar `poll()` keyin `offer()` chaqiriladi. Bu yukni texniklar orasida tekis taqsimlaydi.

## 1.3 Koddan Bajarilishgacha

HotelOS Java tilida yozilgan, demak kompilyatsiya qilinadigan tildir. Manba kodning bajariluvchi mashina ko'rsatmalariga aylanishi to'rt bosqichdan iborat.

**Oldindan qayta ishlash (preprocessing) — Java'da rasmiy oldindan qayta ishlovchi yo'q.** Bu C/C++ dan farqli ravishda Java'ning xususiyatidir. Annotatsiyalar protsessorlari (`@SpringBootApplication`, `@Entity` kabi) kompilyatsiya bosqichida ishga tushadi va qo'shimcha kod yaratishi mumkin. Loyihada Lombok ishlatilmagan, lekin Spring Boot va JPA annotatsiyalari `javac` ga ulanadigan annotatsiyalar protsessorlari orqali metadata yaratadi.

**Kompilyatsiya — `javac` bayt-kod ishlab chiqaradi.** `mvn clean install` buyrug'i ishga tushganda, Maven har bir modul uchun `maven-compiler-plugin` ni chaqiradi. Bu o'z navbatida `javac` ni `.java` fayllarni `.class` fayllariga aylantirish uchun chaqiradi. Bu bosqichda topiladigan xatolarga sintaksis xatolari (qavs yopilmagan), tip nomuvofiqliklari (`String` ga `int` o'zlashtirish) va o'zgaruvchining e'lon qilinmagani holatlari kiradi. Hisobotda keltirilgan BUG-02 ko'pgina tip xatolari aynan shu bosqichda ushlanadi, lekin ba'zilari ish vaqtigacha qoladi.

**Bog'lash — Java'da klassik bog'lash yo'q.** C tilidan farqli o'laroq, Java bajarilish vaqtida `ClassLoader` orqali sinflarni dinamik yuklaydi. Spring Boot bizning fat JAR (BOOT-INF/ ostida bog'liqliklar bo'lgan) uchun maxsus `JarLauncher` ishlatadi. Bu klass-yo'lda topilmagan klasslar uchun `ClassNotFoundException` ish vaqtida sodir bo'lishi mumkinligini bildiradi — kompilyatsiya vaqtida emas. Maven `pom.xml` orqali bog'liqliklarni hal qiladi va ularni `target/*-1.0.0.jar` fat JAR ichiga jamlaydi.

**Bajarish — JVM interpret qiladi va JIT optimizatsiya qiladi.** `java -jar reception-service/target/reception-service-1.0.0.jar` buyrug'i JVM ni ishga tushiradi. JVM avval bayt-kodni interpretatsiya qiladi, keyin "issiq" metodlar (tez-tez chaqiriladiganlar) JIT (Just-In-Time) kompilyator tomonidan to'g'ridan-to'g'ri native mashina koduga o'tkaziladi. Bizning xona tayinlash algoritmimiz birinchi bir necha o'nlab marta interpretatsiya qilinadi, keyin esa JIT uni native ko'rsatmalarga kompilyatsiya qiladi va keyingi bajarilishlar tezroq bo'ladi.

JVM xotirani ham boshqaradi: heap'da obyektlar saqlanadi, garbage collector ulardan keyin tozalaydi. Bu C++ dan katta farq — biz hech qachon `free()` chaqirmaymiz va xotira oqishi bilan kurashish kerak emas.

## 1.4 Texnologiya Stekini Asoslash

| Komponent | Tanlov | Asoslash | Cheklovlar |
|---|---|---|---|
| Til | **Java 17** | Korxona sinfidagi mustahkamlik, kuchli tip tizimi, OOP namunasi | Tezligi Go/Rust dan past |
| Build | **Maven multi-module** | Sanoat standarti, deklarativ konfiguratsiya | XML mukammallashtirishni qiyinlashtiradi |
| Web freymvork | **Spring Boot 3** | Auto-konfiguratsiya, embedded Tomcat, JPA integratsiyasi | Yuqori ishga tushish vaqti (~3 sek) |
| Broker | **Custom WebSocket pub/sub** | Tashqi xizmat shart emas; pub/sub semantikasi ko'rsatilgan | Disk persistensiyasi yo'q (qayta ishga tushishda yo'qoladi) |
| Saqlash | **H2 (file-mode)** | JDBC, JPA bilan ishlaydi, o'rnatish kerak emas | Bitta jarayonga bog'lanadi (file lock) |
| WebSocket | **Spring WebSocket** | Spring ekotizimi bilan to'liq integratsiyalashgan | Spring'ga bog'liqlik |
| Dashboard | **Vanilla HTML+JS** | Build step yo'q, brauzerda darhol ochiladi | Murakkab UI uchun chegaralar |

Cheklovlarni hal qilish: broker'ning persistensiya yo'qligi qabul qilingan (demo tizim); H2'ning bitta-jarayon cheklovi reception faqat bitta nusxada ishlashi bilan hal qilingan (boshqa servislar JPA ishlatmaydi).

\newpage

# 2-Vazifa — HotelOSda Dasturlash Paradigmalari

## 2.1 Uch Paradigma Tushuntirildi

**Protsedural dasturlash** kompyuter ko'rsatmalarini tartiblangan ketma-ketlik sifatida ifodalovchi paradigma. Kod funktsiyalar (yoki protseduralarga) bo'linadi, ular ma'lumotlar ustida operatsiyalar bajaradi va boshqa funksiyalarni chaqiradi. Asosiy xususiyat — **ma'lumotlar va xatti-harakatlar alohida**: o'zgaruvchilar bir tomonda, ularni qayta ishlovchi protseduralar boshqa tomonda. Mos vazifalar: hisob-kitoblar, qadamlardan iborat algoritmlar, ma'lumotlarni qayta ishlash quvurlari.

**Ob'ektga yo'naltirilgan dasturlash (OOP)** ma'lumotlarni va ularni boshqaruvchi xatti-harakatlarni bitta birlikka — **klassga** — birlashtiradi. Klassdan yaratilgan ob'ektlar holatga va metodlarga ega. To'rt ustun: **inkapsulyatsiya** (ichki ma'lumotlarni tashqi kirishdan himoyalash), **meros olish** (bola klass ota klass xususiyatlarini oladi), **polimorfizm** (bitta interfeys, ko'p amalga oshirish) va **abstraksiya** (murakkablikni soddaroq interfeys orqasida yashirish). Mos vazifalar: domen modellashtirish, GUI tizimlari, foydalanuvchi profillari kabi ko'p sonli ob'ektlar bilan ishlash.

**Hodisaga asoslangan dasturlash** dastur oqimi tashqi hodisalar bilan boshqariladigan paradigma. Hodisalar — bu xabarlar, foydalanuvchi harakatlari, taymerlar yoki tarmoq paketlari bo'lishi mumkin. Kod **hodisa ishlovchilarini** ro'yxatdan o'tkazadi va asosiy oqim shunchaki hodisalarni kutadi. Mos vazifalar: UI, WebSocket serverlari, xabar brokerlari, real vaqtli tizimlar.

Bu paradigmalar **o'zaro eksklyuziv emas**. HotelOS uchchasini ham bir vaqtning o'zida ishlatadi: protsedural usulda yozilgan hisob-kitob funktsiyasi OOP `BillingService` klassi ichida joylashgan, bu klass esa hodisa ishlovchisi tomonidan chaqirilishi mumkin. Real dunyo dasturiy ta'minoti deyarli har doim ko'p paradigmali — chunki har bir paradigma o'zining kuchli tomoniga ega va to'g'ri tanlash dizayn qarori, mafkura masalasi emas.

## 2.2 HotelOSda Protsedural Dasturlash

`BillingService.computeBill()` metodi protsedural dasturlash misolidir. Metod ma'lumotlarni (Guest, Room, charges ro'yxati) qabul qiladi va bosqichma-bosqich ularni qayta ishlaydi.

```java
public CheckOutResponse computeBill(Guest guest, Room room, Instant checkOutMoment) {
    int actualNights = Math.max(1, nightsBetween(guest.getCheckInAt(), checkOutMoment));
    BigDecimal roomTotal = room.getNightlyRate()
            .multiply(BigDecimal.valueOf(actualNights))
            .setScale(2, RoundingMode.HALF_UP);
    List<RoomCharge> roomCharges = charges.findByRoomNumber(room.getNumber());
    BigDecimal chargesTotal = roomCharges.stream()
            .map(RoomCharge::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal extras = guest.getExtraCharges();
    BigDecimal subtotal = roomTotal.add(chargesTotal).add(extras);
    BigDecimal discount = guest.getDiscount().min(subtotal);
    BigDecimal grandTotal = subtotal.subtract(discount);
    // ... response construction ...
}
```

Bu kod **protsedural**, chunki u **birin-ketinki bosqichlar ketma-ketligi**: tunlarni hisoblash, xona summasini hisoblash, to'lovlarni jamlash, chegirmani qo'llash. Hech qanday ob'ekt yaratilmaydi; bo'lmagan boshqa "qisman tugatilgan" holatlar yo'q. Metod o'z ichida tartibsizdir va xuddi shu kirish bilan har doim bir xil chiqishni qaytaradi (deterministik).

Nima uchun bu yerda protsedural to'g'ri tanlov edi? Hisob-kitob — bu matematik formula bo'lib, uni ob'ekt sifatida modellashtirish ortiqcha ko'rinadi. "Bill" klassi yaratish, unga `addCharge()`, `applyDiscount()` metodlari berish foydaliroq emas — biz oxirgi natijaga emas, oraliq qadamlarga e'tibor bermayapmiz. To'g'ri keladigan vositani tanlash dizayn qarori; bu yerda u — protsedural funktsiya.

Ikkinchi misol — `RoomAssignmentService.assign()` metodida Stream API zanjiri:

```java
List<Room> candidates = rooms.findAll().stream()
        .filter(r -> r.getType() == type)
        .filter(r -> r.getStatus() == RoomStatus.CLEAN)
        .toList();
```

Bu funktsional uslubdagi protsedural kod — quvur ma'lumotlar oqimini bir qadamdan keyingisiga uzatadi.

## 2.3 HotelOSda Ob'ektga Yo'naltirilgan Dasturlash

### Inkapsulyatsiya

`Order` klassi inkapsulyatsiyaning yaqqol misoli. Uning maydonlari `private final`, ya'ni tashqaridan o'zgartirib bo'lmaydi:

```java
public class Order {
    private final long id;
    private final String roomNumber;
    private final List<OrderItem> items;
    private final BigDecimal total;
    private OrderStatus status;

    public boolean advance() {
        OrderStatus next = status.next();
        if (next == null) return false;
        this.status = next;
        return true;
    }
}
```

Holat o'zgarishi faqat `advance()` metodi orqali sodir bo'ladi. Bu **invariantni himoyalaydi**: status faqat oldinga harakatlanishi mumkin (RECEIVED → PREPARING → DELIVERING → DELIVERED), tasodifan teskari yo'l bilan yoki o'zboshimchalik bilan o'rnatib bo'lmaydi. Agar `status` `public` bo'lganida, har qanday kod uni qayta yozishi mumkin edi va lifecycle invariantni saqlash imkonsiz bo'lar edi.

### Meros olish

`BrokerClient` ichidagi `InternalHandler` Spring'ning `AbstractWebSocketHandler` klassidan meros oladi:

```java
private class InternalHandler extends AbstractWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage message) {
        // ...
    }
}
```

Meros olish bizga Spring'ning umumiy WebSocket boshqaruv mantig'ini (ulanishni saqlash, xatolarni boshqarish, sinxronlik) qayta yozmasdan ishlatish imkonini beradi. Biz faqat o'zimizning `handleTextMessage` ni amalga oshiramiz; qolganlari ota klassdan keladi.

### Polimorfizm

`Comparator` interfeysi polimorfizmni namoyish etadi. `MaintenancePriorityQueue` ichida:

```java
private static final Comparator<MaintenanceIssue> ORDER = Comparator
        .comparingInt((MaintenanceIssue i) -> i.getUrgency().ordinal())
        .thenComparing(MaintenanceIssue::getSubmittedAt);
```

`PriorityQueue` o'z konstruktorida `Comparator<E>` qabul qiladi. Bu yerda biz uchun ishlab chiqilgan **lambda asoslangan** komparator yuboriladi. `PriorityQueue` qaysi maxsus komparator berilganligini bilmaydi — u faqat interfeysni biladi va `compare()` metodini chaqiradi. Bu polimorfizm: bitta interfeys, har xil amalga oshirishlar.

### Abstraksiya

`BrokerClient` abstraksiyaning yaxshi misoli. Servislar uni shunday ishlatadi:

```java
broker.subscribe(Topics.ROOM_VACATED, payload -> {
    String roomNumber = String.valueOf(payload.get("roomNumber"));
    cleaningQueue.enqueue(roomNumber);
});
```

Bir qator kod ortida WebSocket ulanish, JSON serializatsiya, qayta ulanish, ko'p tarmoqli xabar tarqatish yashiringan. Foydalanuvchi (qabul qiluvchi servis) faqat **"shu mavzuda xabar kelganda mana shu kodni ishga tushir"** deydi. Murakkablikni yashirish — bu dasturiy injenerlikning markaziy qiymatlaridan biridir.

## 2.4 HotelOSda Hodisaga Asoslangan Dasturlash

HotelOS asosan hodisaga asoslangan tizimdir. Servislarning xatti-harakatlari to'g'ridan-to'g'ri chaqiriqlar emas, balki **hodisalar oqimi** bilan boshqariladi.

### Misol 1 — Brokerdagi `room.vacated` hodisasi

**Ishga tushiruvchi:** Reception kontroleri `POST /checkout/204` ni qabul qiladi va checkout mantig'ini ishga tushiradi. Bill hisoblangach, xona DIRTY holatiga o'tadi.

**Nashr etish:**

```java
broker.publish(Topics.ROOM_VACATED, Map.of(
        "roomNumber", roomNumber,
        "vacatedAt", now.toString()
));
```

`BrokerClient.publish()` JSON xabarni broker WebSocketga yuboradi.

**Brokerda qayta ishlash:** `PubSubWebSocketHandler` xabarni qabul qiladi, mavzuga obuna bo'lgan barcha sessiyalarni topadi va har biriga `deliver` harakati bilan jo'natadi:

```java
for (WebSocketSession sub : subscribers) {
    if (sub.isOpen()) {
        synchronized (sub) {
            sub.sendMessage(delivery);
        }
    }
}
```

**Qabul qilish va harakat:** Housekeeping `BrokerClient` orqali `Topics.ROOM_VACATED` ga obuna bo'lgan:

```java
broker.subscribe(Topics.ROOM_VACATED, payload -> {
    String roomNumber = String.valueOf(payload.get("roomNumber"));
    enqueue(roomNumber);
});
```

Xabar kelishi bilan lambda chaqiriladi va xona tozalash navbatiga qo'shiladi. Eng muhimi: **Reception kim tinglayotganini bilmaydi.** Agar ertaga yangi `Reporting` servisi qo'shilsa va u ham `room.vacated` ga obuna bo'lsa, Reception kodi o'zgarmaydi.

### Misol 2 — Dashboardga WebSocket hodisa uzatish

**Ishga tushiruvchi:** Brauzerdagi operator dashboard sahifasini ochadi.

**Ulanish:** JavaScript `new WebSocket('ws://localhost:4000/live')` chaqiradi. Dashboard servisi `DashboardWebSocketHandler.afterConnectionEstablished()` orqali ulanishni qabul qiladi va sessiyani saqlaydi.

**Server tomon hodisalar:** Har safar broker'da xabar e'lon qilinsa (masalan, xona holati o'zgarsa), `BrokerBridge` uni qabul qiladi va `dashboard.broadcast()` chaqiradi:

```java
public void broadcast(BrokerMessage delivery) {
    for (WebSocketSession s : sessions) {
        synchronized (s) {
            s.sendMessage(msg);
        }
    }
}
```

**Brauzerdagi qabul:** JavaScript `ws.onmessage` ishlovchisi chaqiriladi, JSON parsing qilinadi va UI yangilanadi:

```javascript
ws.onmessage = (evt) => {
    const msg = JSON.parse(evt.data);
    handleEvent(msg.topic, msg.payload || {});
};
```

Foydalanuvchi sahifani yangilamaydi, hech qanday so'rov yubormaydi — server o'z tashabbusi bilan brauzerga "shu xabar keldi" deb aytadi. Bu klassik **server-tomonidan ishga tushiriluvchi (server-push)** hodisaga asoslangan o'zaro ta'sirdir.

## 2.5 Foydalanilgan Asosiy IDE Komponentlari

HotelOS IntelliJ IDEA Community Edition'da ishlab chiqilgan.

**Kod muharriri** — sintaksis ajratib ko'rsatish Java tilini "anglar" va xatolarni qizil chiziq bilan ko'rsatdi. Avto-to'ldirish (Ctrl+Space) Spring annotatsiyalarini, JPA metodlarini va broker mavzularini yozayotganda taklif qildi. Kod yig'ish (Ctrl+−) uzun klasslarni ko'rib chiqishni osonlashtirdi.

**Disk raskadrovka vositasi** — `RoomAssignmentService.assign()` ichida to'xtash nuqtalari qo'yib, BUG-01 (race condition) ni topish jarayonida o'zgaruvchilarni real vaqtda kuzatish imkonini berdi. **Chaqiriq stekini** ko'rish HTTP so'rov qaysi yo'l bilan kontrollerga, undan servisga yetib kelganini tushunishga yordam berdi.

**Terminal** — IDE ichidagi terminal `mvn clean install` va `./start.sh` ni ishga tushirish, `tail -f logs/reception.log` orqali servis loglarini kuzatish uchun ishlatildi.

**Versiya nazoratining integratsiyasi** — IDE git oynasi har bir o'zgarish uchun farqlarni vizual ko'rsatdi. 12 ta mazmunli commit aynan shu interfeys orqali sodir etildi. Branch tarmoqlash kerak bo'lmadi, chunki loyiha bitta ishlab chiquvchi tomonidan amalga oshirildi.

**O'rnatilgan plaginlar** — Spring Boot plagini Maven konfiguratsiyasini avtomatik aniqlaydi va har bir servisni "Run" tugmasi bilan ishga tushirish imkonini beradi. Lombok plagini ishlatilmadi (biz qisqartirilgan annotatsiyalardan qochdik), ammo Database Tools plagini H2 fayllarini bevosita ko'rishga yordam berdi.

\newpage

# 3-Vazifa — HotelOSni Qurish

## 3.1 Nima Qurildi

HotelOS to'liq ishlaydigan tizim sifatida amalga oshirildi va `https://github.com/xamidullayev38/hotelos` da joylashtirildi. U quyidagi komponentlardan iborat:

**To'rtta mikroservis:**

- **Reception (4001-port)** — xona tayinlash va billing algoritmlari, check-in/check-out endpointlari
- **Housekeeping (4002-port)** — tozalash navbati, xona holati o'tishlari
- **Room Service (4003-port)** — buyurtma navbati va lifecycle
- **Maintenance (4004-port)** — ustuvorlik navbati, texniklarga round-robin tayinlash

**Xabar brokeri (4005-port):** Custom WebSocket pub/sub server. 6 ta mavzu (`room.vacated`, `room.status_changed`, `order.created`, `order.status_changed`, `maintenance.reported`, `maintenance.resolved`) bo'yicha xabarlarni yuvib beradi. Servislar bir-birini bilmaydi — faqat broker orqali muloqot qiladi.

**Operatsiyalar paneli (4000-port):** Brauzer asoslangan dashboard, vanilla HTML + JS. WebSocket `ws://localhost:4000/live` orqali ulanadi. Auth gate `demo-token` ni talab qiladi. 10 xona, faol buyurtmalar va ochiq texnik muammolar real vaqtda ko'rsatiladi.

**Ma'lumotlar tuzilmalari (jadval 1):**

| Ma'lumot | Tuzilma | Joyi |
|---|---|---|
| Xona inventari | `List<Room>` + JPA | Reception |
| Mehmonlar | `JpaRepository` | Reception |
| Tozalash navbati | `ConcurrentLinkedDeque<String>` (FIFO) | Housekeeping |
| Buyurtma navbati | `ConcurrentHashMap<Long, Order>` (FIFO) | Room Service |
| Texnik xizmat | `PriorityQueue<MaintenanceIssue>` (heap) | Maintenance |
| Texniklar roteri | `ArrayDeque<String>` (round-robin) | Maintenance |

## 3.2 Xavfsizlik Mulohazalari

**Kiritishni tekshirish.** Har bir kontroler DTO Jakarta Bean Validation ishlatadi. `CheckInRequest.fullName` `@NotBlank`, `nights` `@Min(1)`, `roomNumber` regex `^[1-2][0-9]{2}$`. Validatsiya boundary'da — controller metod tanasiga kirishdan oldin — bo'lib o'tadi. Yana validatsiya `HttpMessageNotReadableException` orqali yaroqsiz JSON yoki noma'lum enum qiymati uchun ham bajariladi.

**Autentifikatsiya.** Dashboard `demo-token` ni talab qiladi. Login sahifasi token kiritilmaguncha live ma'lumotlarni ko'rsatmaydi. Production tizimda bu JWT yoki OAuth2 ga almashtirilishi kerak; demo uchun bitta token yetarli.

**Ma'lumotlarni oshkor qilish.** Broker xabarlarining tarkibi qattiq nazorat ostida. Reception `room.vacated` ni nashr etganida faqat `{roomNumber, vacatedAt}` yuboradi — mehmonning ismi, to'lov ma'lumotlari yoki passport raqami yuborilmaydi. Bu **whitelist** yondashuvi: faqat aniq ruxsat berilgan maydonlar tarmoqdan o'tadi.

**XSS oldini olish.** Dashboard server-tomondan kelgan har qanday matnni DOM ga kiritishdan oldin `esc()` yordamchisi orqali HTML-escape qiladi:

```javascript
function esc(v) {
  return String(v)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
```

Bu juda muhim, chunki `MaintenanceIssue.description` mehmondan keladigan erkin matnli maydondir. Hujumchi `<script>` kiritsa, escape qilmaganda kelajakdagi operator brauzerida bajariladi.

**Xatolarni boshqarish.** Har bir servis `@ControllerAdvice` orqali global xato boshqaruviga ega. Hech qachon stek izi mijozga uzatilmaydi. Xatolar `{error: "TYPE", message: "..."}` JSON formatida qaytariladi, ichki holat esa server logiga yoziladi.

## 3.3 IDE Ishlab Chiqish Jarayoni Dalili

12 ta mazmunli git commit ishlab chiqish bosqichlarini ko'rsatadi:

```
0afbfea docs: design spec, algorithm flowcharts, test results
e5fbd6b feat: process scripts, BTEC test scenarios, README
eabee0b feat(dashboard): live operations panel with WebSocket bridge
1cb10e8 feat(maintenance): priority queue algorithm + round-robin technicians
87f3cbc feat(roomservice): order queue with lifecycle and charging
3f12b26 feat(housekeeping): FIFO cleaning queue and status transitions
2544804 feat(reception): billing algorithm, check-in/check-out flows
c46cb87 feat(reception): multi-criteria room assignment algorithm
1f895f6 feat(reception): Room/Guest/RoomCharge JPA entities + seed
e88f244 feat(broker): custom WebSocket pub/sub server
0dde44a feat(common): shared events, BrokerMessage DTO, BrokerClient
9905df2 init: parent POM and module skeleton for HotelOS
```

Har bir commit xabari **nima** o'zgargani va **nima uchun** ekanligini tasvirlaydi. "fix bug" yoki "update code" kabi mavhum xabarlar ishlatilmadi.

IDE refaktoring vositasi ishlatilgan misol: `ReceptionController.checkIn()` ichidagi mantiq dastlab bitta katta metod edi. BUG-01 ni tuzatish jarayonida "Extract Method" refaktoringi qo'llanildi — eski mantiq `doCheckIn()` ga ko'chirildi, asl metod esa shunchaki `synchronized` blok ichida uni chaqirdi. Bu IDE'ning avtomatik nomlarni qayta yozishi va parametr ko'rsatkichlarini saqlashi bilan xavfsiz amalga oshirildi.

Kod navigatsiya misoli: `Topics.ROOM_VACATED` konstantasiga "Find All Usages" qo'llanilib, uning nashr etilgan (Reception) va obuna bo'lingan (Housekeeping, Dashboard) joylari topildi. Bu broker hodisalari oqimini tushunishni juda osonlashtirdi.

## 3.4 Test Stsenariylari

8 ta BTEC test stsenariysi `run-tests.sh` skripti orqali avtomatik bajariladi va natijalar `docs/report/test-results.md` ga yoziladi. Quyidagi jadval qisqacha xulosani ko'rsatadi:

| ID | Stsenariy | Natija |
|---|---|---|
| TS-01 | DOUBLE 3-qavatda → fallback | ✅ 202 raqamiga tayinlandi (eng uzoq toza DOUBLE) |
| TS-02 | 204 dan check-out | ✅ Bill: 260.00 USD, room.vacated nashr etildi |
| TS-03 | 204 ni toza deb belgilash | ✅ DIRTY → CLEANING → CLEAN o'tishlari |
| TS-04 | 105 ga 2 qahva + 1 sandvich | ✅ Buyurtma: 21.00 USD, 4 lifecycle bosqichi |
| TS-05 | 105 dushi singan, CRITICAL | ✅ Heap boshida, Aziz Karimov ga tayinlandi |
| TS-06 | Bir vaqtda 2 ta DOUBLE check-in | ✅ 102 va 203 (ikki xil xona, race tuzatildi) |
| TS-07 | Barcha SUITE band → uchinchi so'rov | ✅ HTTP 409 ROOMS_UNAVAILABLE |
| TS-08 | Yaroqsiz kirishlar (3 xil) | ✅ Har biri VALIDATION_FAILED bilan rad etildi |

To'liq xom natijalar `docs/report/test-results.md` (285 satr) ga jo'natilgan. Hozir tizim ishlab turibdi va istalgan paytda `./run-tests.sh` orqali qayta sinab ko'rish mumkin.

\newpage

# 4-Vazifa — Disk Raskadrovka va Kodlash Standartlari

## 4.1 Disk Raskadrovka Jarayoni

Disk raskadrovka — bu dastur ish vaqtidagi noto'g'ri xatti-harakat sababini tizimli ravishda topish va tuzatish jarayoni. U ixtiyoriy emas: har qanday katta tizim aniq qoidalar bo'yicha ishlab chiqilishi bilan birga, har doim xatolar bo'ladi — ular cheklov yoki vaqt muammosi, mualliflarning sodir bo'lmagan taxminlari, yoki ikki tomonlama kodda kelishilmagan invariantlardan kelib chiqadi.

**Sintaksis xatolari** kompilyator tomonidan aniqlanadi. `javac` "missing semicolon" yoki "cannot resolve symbol" deydi. Bu xatolar arzon — bir necha soniya ichida tuzatiladi. Misol: dastlabki `BrokerClient.java` da `Consumer<Map<String, Object>>` o'rniga `Consumer<Map>` yozilgandi, kompilyator generic tip tekshiruvi orqali ushlab oldi.

**Ishlash xatolari (runtime errors)** kod kompilyatsiyadan o'tadi, lekin ish vaqtida muvaffaqiyatsizlikka uchraydi. `NullPointerException`, `IndexOutOfBoundsException` shu turdagi xatolardir. Misol: dastlab Reception ning `ORDER_CREATED` ishlovchisi `Number totalNum = (Number) payload.get("total")` ni `null` mumkinligini tekshirmasdan qildi. Sinov vaqtida buyurtma tarkibi mavjud bo'lmagan formatda kelganida `NullPointerException` paydo bo'ldi.

**Mantiq xatolari** eng qiyini. Dastur ishdan chiqmaydi, kompilyator hech narsa demaydi, lekin natija noto'g'ri. BUG-01 (race condition) aynan shunday: kod ishlaydi, lekin parallel bajarilganda noto'g'ri xona tayinlaydi.

**IDE disk raskadrovka vositalari:**

- **To'xtash nuqtalari** — kod ma'lum satrda to'xtaydi va dasturchi o'zgaruvchilarni tekshira oladi. BUG-01 ni tuzatish jarayonida `assign()` metodida to'xtash nuqtasi qo'yildi va parallel ikkita ipni bir vaqtda kuzatildi.
- **Kuzatuv ifodalari** — istalgan ifodaning (masalan, `rooms.findById("204").get().getStatus()`) qiymati real vaqtda ko'rsatiladi. Bu kontekst o'zgarishlarini tezda tushunish imkonini beradi.
- **Chaqiriq steki** — joriy metod qaysi metodlardan chaqirilganini ko'rsatadi. WebSocket hodisalarining qaysi obuna ishlovchisidan ishga tushganini kuzatish uchun foydali.
- **Bosqichma-bosqich bajarish** (F8) — keyingi satrga o'tadi; F7 ichkariga kiradi (chaqiriqlarga kiradi).

**Umumiy ish jarayoni:** muammo qayd etiladi (qaytariladigan stsenariy); to'xtash nuqtasi qo'yiladi; bajarish kuzatiladi; o'zgaruvchilar tekshiriladi; gipotezalar tuziladi va sinovdan o'tkaziladi; tuzatish kiritiladi; muammo qaytadan kuzatilmasligi tekshiriladi.

## 4.2 Disk Raskadrovka Jurnali

### XATO-01 — Concurrent Check-in Race Condition

| Maydon | Tafsilot |
|---|---|
| **ID** | XATO-01 |
| **Tavsif** | TS-06 stsenariyida ikki mehmon bir vaqtda DOUBLE so'raganda, har ikkalasiga ham 102 raqami berildi. Birinchi mehmon 102, ikkinchisi ham 102 — Reception logida ikki marta `OCCUPIED` qayd etilgan. |
| **Turi** | Mantiq xatosi (concurrency/race condition) |
| **Qanday aniqlandi** | TS-06 test stsenariysi muvaffaqiyatsiz bo'ldi. `run-tests.sh` chiqishida `{"guestId":3,"roomNumber":"102",...}{"guestId":4,"roomNumber":"102",...}` ko'rindi. |
| **Disk raskadrovka qadamlari** | 1. Stsenariyni qaytaruvchi minimal `bash` skripti yozildi (2 ta `curl` & bilan parallel). 2. `RoomAssignmentService.assign()` ga to'xtash nuqtasi qo'yildi. 3. Parallel ikkita ipni IntelliJ Threads paneli orqali kuzatildi. 4. Ikkala ip ham `OCCUPIED` bo'lgunga qadar 102 ni `CLEAN` deb ko'rgani aniqlandi. |
| **Asosiy sabab** | `assign()` va sirti orqasidagi `rooms.save()` orasida lock yo'q edi. JPA `@Transactional` faqat bitta bog'liq tranzaksiya doirasini boshqaradi, lekin ikkita HTTP so'rov ikkita alohida tranzaksiyada parallel ishlaydi va ularning `findAll() + save()` ketma-ketligi o'zaro qisqartiriladi (interleaved). |
| **Qo'llanilgan tuzatish** | `ReceptionController.checkIn()` ga `synchronized(checkInLock)` blok qo'shildi va asl mantiq `doCheckIn()` ga ko'chirildi. Endi har bir check-in ketma-ket ishlaydi. Bitta Reception nusxasi uchun bu yetarli — agar ko'p nusxa kerak bo'lsa, distributed lock kerak bo'ladi. |
| **Oldini olish** | Har qanday "tekshir-keyin-yangila" operatsiyasi parallel kontekstda atomik bo'lishi kerak. Kelajakda barcha shu turdagi operatsiyalar uchun lock yoki database-level pessimistic locking (`SELECT ... FOR UPDATE`) ishlatiladi. |

### XATO-02 — Yaroqsiz enum INTERNAL_ERROR ni qaytarmoqda

| Maydon | Tafsilot |
|---|---|
| **ID** | XATO-02 |
| **Tavsif** | TS-08 stsenariyida `POST /report` ga `urgency: "NUCLEAR"` (yaroqsiz enum qiymati) yuborilganida, javob `VALIDATION_FAILED` o'rniga `INTERNAL_ERROR` bo'lib qaytdi. Bu noto'g'ri — mijoz xatosi sifatida ko'rsatilishi kerak edi. |
| **Turi** | Mantiq xatosi (xato boshqarishda) |
| **Qanday aniqlandi** | TS-08 chiqishida `{"error":"INTERNAL_ERROR","message":"Something went wrong."}` ko'rindi. Kutilgan qiymat `VALIDATION_FAILED` edi. |
| **Disk raskadrovka qadamlari** | 1. `ErrorHandler.java` da `@ExceptionHandler(Exception.class)` umumiy ishlovchisi ushlab olganligi tahmin qilindi. 2. Server logiga qaraldi: aslida `HttpMessageNotReadableException` tashlangan ekan (Jackson enum'ni deserializatsiya qila olmadi). 3. Bu istisno generic `Exception.class` ishlovchisi tomonidan tutib olingani aniqlandi. |
| **Asosiy sabab** | Jackson deserializatsiya bosqichi Bean Validation dan oldin ishlaydi. Yaroqsiz enum sintaktik xato emas — JSON to'g'ri shaklda — lekin Java tip darajasida o'zgartirilmaydi. Tegishli istisnoni ushlash uchun maxsus ishlovchi yo'q edi. |
| **Qo'llanilgan tuzatish** | `ErrorHandler.java` ga `@ExceptionHandler(HttpMessageNotReadableException.class)` qo'shildi. U xabarning birinchi qatorini ajratib oladi va `VALIDATION_FAILED` xato kodi bilan HTTP 400 ni qaytaradi. |
| **Oldini olish** | Yangi kontroler qo'shilganda har doim yangi istisno turlari haqida o'ylash kerak. Validatsiya jarayonining qatlamlari (Jackson → Bean Validation → biznes mantig'i) tushunilishi muhim. |

### XATO-03 — Dashboard XSS zaifligi

| Maydon | Tafsilot |
|---|---|
| **ID** | XATO-03 |
| **Tavsif** | `dashboard/app.js` ning birinchi versiyasi xabar payload'idan kelgan matnni `innerHTML` orqali DOM ga to'g'ridan-to'g'ri kiritardi. Maintenance hisobotidagi `description` maydoni mehmondan keladigan erkin matn bo'lib, hujumchi `<script>` kiritsa, dashboard ochgan operator brauzerida bajarilishi mumkin edi. |
| **Turi** | Xavfsizlik xatosi (XSS, injection) |
| **Qanday aniqlandi** | IDE'da kod sharhi vaqtida `innerHTML` ishlatilishi ogohlantirish bayrog'i bilan ko'rsatildi. Linterning xavfsizlik qoidalari `${someValue}` shaklidagi shablonlar `innerHTML` ga o'tkazilganida ogohlantiradi. |
| **Disk raskadrovka qadamlari** | 1. `app.js` da `innerHTML = ...` ifodalarining barchasi qidirildi (4 ta topildi). 2. Har biri uchun qaysi maydonlar foydalanuvchidan kelganligi aniqlandi. 3. `description`, `assignedTo` foydalanuvchi kiritmasi orqali yetib kelishi mumkinligi tasdiqlandi. 4. Tasdiqlash: brauzerda `<script>alert(1)</script>` ni description sifatida yuborildi va dashboard ochildi — alert paydo bo'ldi. |
| **Asosiy sabab** | JavaScript `${value}` template literal'lari matnni avtomatik escape qilmaydi. `innerHTML` qabul qiladigan har qanday HTML ni tushunadi va bajaradi. |
| **Qo'llanilgan tuzatish** | `esc()` yordamchi funksiyasi qo'shildi va har bir `${value}` o'rniga `${esc(value)}` ishlatildi. Eng nozik joy — log oqimi — `innerHTML` o'rniga `document.createElement` va `textContent` ishlatdi (DOM API'lari avtomatik escape qiladi). |
| **Oldini olish** | Frontend kod sharhida `innerHTML` ishlatilishini har doim shubhali deb hisoblash. Mumkin bo'lgan joyda `textContent` yoki framework reaktivlik qatlamiga tayanish (React, Vue avtomatik escape qiladi). |

## 4.3 Xavfsizlik uchun Disk Raskadrovka

Disk raskadrovka HotelOS ni xavfsizroq qildi. XATO-03 (XSS) bunga eng yorqin misol: kod yozilganda himoyaviy yondashuv yetarli ko'rinardi (broker xabar formati nazorat ostida), lekin disk raskadrovka jarayoni ko'rsatdiki, **boundary qayerda joylashganligi haqidagi taxmin noto'g'ri edi**. Hujum vektori brokerdan emas, balki maintenance hisobotidan keladi — uni mehmon to'g'ridan-to'g'ri yuboradi. Disk raskadrovka jarayoni butun ishonchsiz ma'lumotlar yo'lini kuzatishni majbur qildi va `description` maydoni hujum vektori sifatida aniqlandi.

Disk raskadrovka **ishlab chiqish bosqichida** bo'lishi joylashtirishdan keyin kashf etilishidan ko'ra ko'p marotaba arzon. XSS zaifligi production ga chiqsa, allaqachon brauzerda bajarilgan zararli skriptlar haqida xabar berilishi mumkin emas. Mehmon shaxsiy ma'lumotlarini boshqaradigan tizim uchun bu xavf juda yuqori — passport raqamlari, to'lov tafsilotlari, qolish tarixi mexnatkash hujum manbai bo'lishi mumkin.

Disk raskadrovka shuningdek **ishonchni oshirdi**. XATO-01 dan keyin, men aslida mikroservis tizimidagi yarish holatlarining nazariy emas, balki amaliy ekanligini bildim. Bu kelajakdagi har bir "find then update" operatsiyasini lock yoki tranzaksion himoya bilan qoplash zarurligini meni eslatib turadi.

## 4.4 Kodlash Standarti

HotelOS quyidagi standartni izchil ravishda qo'llaydi:

**Nomlash konventsiyalari:**

- Klasslar: `PascalCase` (`RoomAssignmentService`, `BrokerMessage`)
- Metodlar va o'zgaruvchilar: `camelCase` (`assignRoom()`, `lastCleanedAt`)
- Konstantalar: `UPPER_SNAKE_CASE` (`ROOM_VACATED`, `KNOWN_STATUSES`)
- Paketlar: barcha kichik harf, nuqta bilan (`com.hotelos.reception.service`)
- Test fayllari: `Xxx + Test.java` (loyihada test yozilmagan)

**Izohlar va hujjatlash:**

Izohlar **nima** o'rniga **nima uchun** ni izohlaydi. Aniq nom yetadigan joyda izoh yo'q. Murakkab qarorlar oldidan bir nechta qatorli javadoc beriladi:

```java
/**
 * Lock for the check-in path. The original code raced under concurrent
 * requests... Found via TS-06 (see docs/report/debug-log.md, BUG-01).
 */
private final Object checkInLock = new Object();
```

**Chekinish va formatlash:**

- 4 ta bo'shliq, hech qachon tab emas (IntelliJ default).
- Maksimal qator uzunligi 120 belgi. Uzun zanjirlar mantiqiy joydan ko'chiriladi.
- Ochuvchi qavslar bir xil qatorda (Java standart).

**Funktsiya va klass uzunligi:**

- Maksimal qabul qilinadigan funktsiya uzunligi ~40 qator. Undan oshganida funksiya ikkiga bo'linadi.
- Klasslar bir mas'uliyatga ega bo'lishi kerak. `RoomAssignmentService` faqat tayinlash qiladi; `BillingService` faqat hisob-kitob.

**Xatolarni boshqarish:**

- Boundary'da (kontroler) barcha istisnolar `@ControllerAdvice` orqali ushlanadi.
- Biznes mantig'ida `Optional<>` ishlatiladi `null` o'rniga.
- Hech qachon `catch (Exception e) {}` (bo'sh blok) yozilmaydi.

**Sehrli raqamlar va konstantalar:**

- Hardcoded raqamlar konstantaga ko'chiriladi. Topiklar, urgency tartibi, regex patternlar — barchasi nomlangan.

```java
private static final String ROOM_PATTERN = "^[1-2][0-9]{2}$"; // 100-299
```

## 4.5 Jamoada Kodlash Standartlari Nima Uchun Muhim

Kodlash standarti yakka dasturchi uchun yaxshi amaliyot, lekin jamoa uchun u **majburiy** infratuzilmadir.

**Standart yo'qligida kod bazasi ostida nima sodir bo'ladi:** har bir dasturchi o'z uslubini yozadi — biri 2 bo'shliq, ikkinchisi 4; biri o'zgaruvchilarni qisqartiradi (`accNum`), ikkinchisi to'liq yozadi (`accountNumber`); biri try/catch ichida hammasini turtib qo'yadi, ikkinchisi hech qaerda istisnolarni boshqarmaydi. Olti oydan keyin kod baza fragmentlangan, "kim yozgan?" deb so'rashning bir necha xil javobi bor va yangi xato qaysi uslub bo'yicha tuzatilishi kerakligi haqida bahslar boshlanadi.

**Yangi a'zolar uchun kirishish vaqti:** standart bo'lganida, yangi dasturchi `assignRoom()` ko'rganda `RoomAssignmentService.assignRoom()` ekanligini taxmin qila oladi. Konstantalar `Topics.ROOM_VACATED` shaklida ekanligini biladi. Bu intuitiv tushunish — ishga tushishni haftalardan kunlarga qisqartiradi.

**Avtomatlashtirilgan vositalar:** linterlar (Checkstyle, SonarQube) va formatlaydichilar (google-java-format, Spotless) standartni intizomga tayanmasdan tatbiq etadi. CI quvurida `mvn checkstyle:check` qo'shilganida, standartga zid kod commit qilingunigacha xatolik bilan to'xtatiladi. Bu juda muhim, chunki **odam dasturchilar charchaganda standart yozilmaslikni unutadi**, lekin CI hech qachon charchamaydi.

**HotelOS standartini kengaytirish:** agar bu jamoa loyihasi bo'lganida, men quyidagilarni qo'shgan bo'lardim:

- **`.editorconfig` fayli** har bir IDE ni avtomatik 4-bo'shliq + UTF-8 + LF chiziq tugatishlari bilan sozlashi uchun.
- **`.mvn/extensions.xml` da Spotless plugini** har bir `mvn install` da kodni formatlash uchun.
- **Pre-commit hook** Spotless va Checkstyle ni ishga tushiradigan.
- **Pull request shablon** har bir PR uchun: nima qilingan? Nima uchun? Qaysi testlar yozilgan?
- **Branch himoyasi** main branchga to'g'ridan-to'g'ri push qilishni taqiqlash. Hamma narsa pull request orqali.

Bu qo'shimchalar individual loyiha uchun ortiqcha, lekin jamoa uchun ular **standartning bajarilishini kafolatlaydi**, intizomga tayanmasdan.

\newpage

# Adabiyotlar

Aho, A. V., Hopcroft, J. E. and Ullman, J. D. (1987) *Data Structures and Algorithms*. 1st ed. Reading, MA: Addison-Wesley.

Bloch, J. (2018) *Effective Java*. 3rd ed. Boston: Addison-Wesley.

Fowler, M. (2018) *Refactoring: Improving the Design of Existing Code*. 2nd ed. Boston: Addison-Wesley.

Gamma, E., Helm, R., Johnson, R. and Vlissides, J. (1994) *Design Patterns: Elements of Reusable Object-Oriented Software*. Reading, MA: Addison-Wesley.

Hunt, A. and Thomas, D. (2000) *The Pragmatic Programmer: From Journeyman to Master*. 1st ed. Reading, MA: Addison-Wesley.

McConnell, S. (2004) *Code Complete: A Practical Handbook of Software Construction*. 2nd ed. Redmond, WA: Microsoft Press.

Newman, S. (2021) *Building Microservices*. 2nd ed. Sebastopol, CA: O'Reilly Media.

Pivotal Software (2024) *Spring Framework Documentation, Version 6.1*. Available at: https://docs.spring.io/spring-framework/reference/ (Accessed: 7 June 2026).

MDN Web Docs (2024) *WebSocket API*. Available at: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API (Accessed: 7 June 2026).

Adkins, H. et al. (2020) *Building Secure and Reliable Systems: Best Practices for Designing, Implementing, and Maintaining Systems*. Sebastopol, CA: O'Reilly Media.

\newpage

# Qo'shimchalar

## A. Manba kod

To'liq manba kod GitHub'da joylashtirilgan: https://github.com/xamidullayev38/hotelos

## B. Test natijalari

To'liq test stsenariy chiqishi `docs/report/test-results.md` faylida (285 satr).

## C. Algoritm blok-sxemalari

Mermaid formatida saqlangan blok-sxemalar:

- `docs/algorithms/room-assignment-flowchart.md`
- `docs/algorithms/billing-flowchart.md`
- `docs/algorithms/maintenance-priority-queue-flowchart.md`

## D. Ishga tushirish ko'rsatmalari

```bash
git clone https://github.com/xamidullayev38/hotelos.git
cd hotelos
mvn clean install -DskipTests
./start.sh
# Brauzerda http://localhost:4000 ni oching
# Sign-in token: demo-token
```
