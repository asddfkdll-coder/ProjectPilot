# تقرير الإصلاح: معالجة JSON غير الكاملة في `NodeProjectDetector`

## اسم المشكلة

معالجة JSON غير الكاملة في `NodeProjectDetector` قد تؤدي إلى معلومات غير دقيقة للمستخدم.

## مكانها

`app/src/main/java/com/projectpilot/app/data/scanner/Detectors.kt`، الأسطر 19-27.

## سببها

في السابق، إذا فشل تحليل `package.json` بالكامل، كان يتم إرجاع `DetectionResult` افتراضي على الفور. هذا يعني أن أي معلومات جزئية قد تكون موجودة في ملف `package.json` (حتى لو كان معطوبًا جزئيًا) لم يتم استخلاصها، مما يقلل من دقة الاكتشاف التلقائي.

## الكود القديم

```kotlin
        val obj = runCatching { LENIENT_JSON.parseToJsonElement(raw).jsonObject }.getOrNull()
        // If JSON parsing fails, we still try to extract what we can, or return a basic detection.
        if (obj == null) return DetectionResult(ProjectType.NODE, runCommand = "npm start")

        val deps = (obj["dependencies"] as? JsonObject)?.keys.orEmpty() +
                (obj["devDependencies"] as? JsonObject)?.keys.orEmpty()
        val scripts = (obj["scripts"] as? JsonObject)
```

## الكود الجديد

```kotlin
        val obj = runCatching { LENIENT_JSON.parseToJsonElement(raw).jsonObject }.getOrNull()

        val deps = if (obj != null) {
            (obj["dependencies"] as? JsonObject)?.keys.orEmpty() +
            (obj["devDependencies"] as? JsonObject)?.keys.orEmpty()
        } else {
            emptySet()
        }
        val scripts = (obj?.get("scripts") as? JsonObject)
```

## سبب اختيار الحل

الحل الجديد يسمح بمحاولة استخلاص `deps` و `scripts` حتى لو كان `obj` (الناتج عن تحليل JSON) فارغًا. في هذه الحالة، سيتم تهيئة `deps` إلى مجموعة فارغة و `scripts` إلى `null`. هذا يضمن أن باقي منطق الكاشف سيستمر في العمل، مما يسمح باكتشاف نوع المشروع بشكل أكثر مرونة، حتى لو كان ملف `package.json` معطوبًا جزئيًا. إذا لم يتم العثور على أي معلومات مفيدة، فسيتم استخدام القيم الافتراضية كما كان من قبل.

## هل يوجد تأثير جانبي؟

لا يتوقع وجود تأثير جانبي سلبي. هذا التعديل يجعل الكاشف أكثر تسامحًا مع الأخطاء في ملفات `package.json`، مما يحسن من دقة الاكتشاف التلقائي.

## هل يحتاج اختبار إضافي؟

نعم، يحتاج إلى اختبار إضافي مع ملفات `package.json` معطوبة جزئيًا للتأكد من أن الكاشف لا يزال يعمل بشكل صحيح ويستخلص المعلومات المتاحة.
