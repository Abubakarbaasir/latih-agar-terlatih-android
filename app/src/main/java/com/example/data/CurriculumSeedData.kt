package com.example.data

import com.example.domain.engine.ArabicHarakat
import com.example.domain.model.*

object CurriculumSeedData {

    data class SeedCategory(
        val id: String,
        val code: String,
        val name: String,
        val description: String,
        val sortOrder: Long
    )

    val initialCategories = listOf(
        SeedCategory("cat_mub", "MUB", "Mubtada dan Khabar", "Melatih struktur Jumlah Ismiyah, penentuan Mubtada', dan kesesuaian Khabar.", 1),
        SeedCategory("cat_fai", "FAI", "Fa'il dan Naibul Fa'il", "Melatih pelaku kata kerja aktif (Fa'il) dan pengganti pelaku pada fi'il majhul.", 2),
        SeedCategory("cat_maf", "MAF", "Maf'ul Bih", "Melatih objek langsung dari fi'il muta'addi dan tanda nashab asli maupun far'i.", 3),
        SeedCategory("cat_mud", "MUD", "Mudhaf dan Mudhaf Ilaih", "Melatih penyandaran kata (Idhafah) dan hukum jar pada Mudhaf Ilaih.", 4),
        SeedCategory("cat_naa", "NAA", "Na'at dan Man'ut", "Melatih kesesuaian sifat (Na'at) mengikuti kata yang disifati (Man'ut) dalam i'rab.", 5),
        SeedCategory("cat_jar", "JAR", "Jar dan Majrur", "Melatih huruf jar dan pengaruhnya terhadap isim majrur serta ta'alluq-nya.", 6),
        SeedCategory("cat_ism", "ISM", "Jumlah Ismiyah", "Melatih kalimat nominal lengkap dengan Inna/Kana beserta saudara-saudaranya.", 7),
        SeedCategory("cat_fil", "FIL", "Jumlah Fi'liyah", "Melatih kalimat verbal dengan berbagai bentuk fi'il madhi, mudhari', dan amr.", 8),
        SeedCategory("cat_tam", "TAM", "Tamyiz", "Melatih isim nakirah manshub yang memperjelas kesamaran zat atau nisbah.", 9),
        SeedCategory("cat_hal", "HAL", "Hal", "Melatih isim manshub yang menjelaskan keadaan shahibul hal saat peristiwa terjadi.", 10),
        SeedCategory("cat_ist", "IST", "Istitsna", "Melatih pengecualian dengan illa dan adawatul istitsna beserta hukum i'rab mustatsna.", 11)
    )

    private fun tokensFromSentence(unvoweledWords: List<String>, qKey: String): List<QuestionToken> {
        require(unvoweledWords.size == 10) { "Soal harus memiliki tepat 10 token, ditemukan ${unvoweledWords.size}" }
        return unvoweledWords.mapIndexed { idx, word ->
            QuestionToken(
                tokenId = "${qKey}_t${idx + 1}",
                position = idx + 1,
                baseText = ArabicHarakat.stripHarakat(word),
                sharafNotes = "",
                allowedExtraLetters = emptyList()
            )
        }
    }

    private fun buildTokenAnalysis(
        qKey: String,
        pos: Int,
        baseText: String,
        voweledArabic: String,
        meaning: String,
        wordType: String,
        nahwuPos: String,
        status: String,
        hukum: String,
        sign: String,
        signType: String,
        manifestation: String,
        reason: String,
        amil: String,
        whyNotOther: String,
        relation: String,
        binaType: String = "",
        binaReason: String = "",
        mahalIrab: String = "",
        acceptedForms: List<String> = emptyList()
    ): AnswerPatternToken {
        return AnswerPatternToken(
            tokenId = "${qKey}_t$pos",
            position = pos,
            baseText = ArabicHarakat.stripHarakat(baseText),
            acceptedForms = acceptedForms,
            voweledArabic = voweledArabic,
            indonesianMeaning = meaning,
            wordType = wordType,
            nahwuPosition = nahwuPos,
            irabOrBinaStatus = status,
            irabHukum = hukum,
            irabSign = sign,
            signType = signType,
            manifestation = manifestation,
            nahwuReason = reason,
            amil = amil,
            whyNotOtherSign = whyNotOther,
            binaType = binaType,
            binaReason = binaReason,
            mahalIrab = mahalIrab,
            relationToOtherWords = relation,
            letterRules = ArabicHarakat.buildDefaultLetterRules(voweledArabic, reason)
        )
    }

    data class SeedQuestionSpec(
        val id: String,
        val code: String,
        val categoryId: String,
        val categoryCode: String,
        val categoryName: String,
        val title: String,
        val instruction: String,
        val difficulty: QuestionDifficulty,
        val status: QuestionStatus,
        val versionContent: QuestionVersionContent
    )

    fun buildInitialQuestions(): List<SeedQuestionSpec> {
        // 1. MUBTADA & KHABAR (Beginner - Published)
        // العلم نور يضيء طريق الطالب المجتهد في طلب المعرفة النافعة (10 kata)
        val q1Key = "q_mub_001"
        val q1Base = listOf("العلم", "نور", "يضيء", "طريق", "الطالب", "المجتهد", "في", "طلب", "المعرفة", "النافعة")
        val q1Tokens = tokensFromSentence(q1Base, q1Key)
        val q1Analyses = listOf(
            buildTokenAnalysis(q1Key, 1, "العلم", "الْعِلْمُ", "Ilmu", "Isim", "Mubtada'", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi Mubtada' (pokok kalimat isim) di awal jumlah ismiyah.", "Amil ma'nawi ibtida' (posisi di awal kalimat).", "Bukan Fathah atau Kasrah karena Mubtada' wajib rafa', dan kata ini isim mufrad sehingga tanda rafa'-nya Dhammah.", "Menjadi pokok pembicaraan yang dijelaskan oleh khabar 'نُورٌ'."),
            buildTokenAnalysis(q1Key, 2, "نور", "نُورٌ", "Cahaya", "Isim", "Khabar Mubtada'", "Mu'rab", "Marfu'", "Dhammah (Tanwin Dhammah)", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi Khabar yang menyempurnakan makna Mubtada' 'الْعِلْمُ', dan bebas dari tanwin hanya jika beralif-lam/mudhaf.", "Mubtada' ('الْعِلْمُ').", "Bukan manshub atau majrur karena khabar mubtada' berhukum rafa'.", "Menerangkan keadaan dan predikat bagi Mubtada' 'الْعِلْمُ'."),
            buildTokenAnalysis(q1Key, 3, "يضيء", "يُضِيءُ", "Menerangi", "Fi'il Mudhari'", "Fi'il Mudhari' (awal jumlah shilah/sifat)", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi (tampak)", "Fi'il mudhari' yang tidak didahului amil nashab maupun amil jazam.", "Amil ma'nawi tajarrud (terbebas dari nashib dan jazim).", "Bukan Fathah atau Sukun karena tidak ada huruf nashab (seperti أَنْ/لَنْ) maupun huruf jazam (seperti لَمْ).", "Menjadi kata kerja yang menerangkan fungsi 'نُورٌ' dan berfa'il dhomir mustatir 'هُوَ'."),
            buildTokenAnalysis(q1Key, 4, "طريق", "طَرِيقَ", "Jalan", "Isim", "Maf'ul Bih & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi objek sasaran (Maf'ul Bih) dari fi'il 'يُضِيءُ', dan tidak ditanwin karena menjadi Mudhaf.", "Fi'il muta'addi 'يُضِيءُ'.", "Bukan Dhammah karena bukan pelaku; bukan Kasrah karena bukan isim majrur.", "Menjadi objek dari 'يُضِيءُ' sekaligus bersandar (Mudhaf) kepada 'الطَّالِبِ'."),
            buildTokenAnalysis(q1Key, 5, "الطالب", "الطَّالِبِ", "Pelajar", "Isim", "Mudhaf Ilaih & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena disandari oleh kata sebelumnya ('طَرِيقَ') sebagai Mudhaf Ilaih.", "Mudhaf ('طَرِيقَ') / idhafah.", "Bukan Dhammah atau Fathah karena setiap Mudhaf Ilaih wajib berhukum jar.", "Melengkapi makna kepemilikan/spesifikasi kata 'طَرِيقَ' dan disifati oleh 'الْمُجْتَهِدِ'."),
            buildTokenAnalysis(q1Key, 6, "المجتهد", "الْمُجْتَهِدِ", "Yang bersungguh-sungguh", "Isim", "Na'at (Sifat)", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi Na'at yang mengikuti Man'ut-nya ('الطَّالِبِ') yang berhukum majrur.", "Mengikuti i'rab Man'ut ('الطَّالِبِ').", "Bukan rafa' atau nashab karena Na'at harus selaras dengan Man'ut dalam i'rab, ma'rifat, jenis, dan bilangan.", "Menjelaskan sifat pelajar yang dimaksud pada kata 'الطَّالِبِ'."),
            buildTokenAnalysis(q1Key, 7, "في", "فِي", "Di dalam / dalam", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun (pada ya mad)", "Bina", "Mahalli (tidak berubah)", "Semua huruf dalam bahasa Arab hukumnya mabni selamanya.", "Tidak dipengaruhi amil; justru menjadi amil jar.", "Huruf tidak menerima perubahan i'rab akhir sehingga tetap sukun.", "Menjadi penghubung makna (ta'alluq) antara 'يُضِيءُ' dengan 'طَلَبِ'.", "Mabni 'ala as-Sukun", "Seluruh harf hukum asalnya mabni.", "Tidak memiliki mahal i'rab"),
            buildTokenAnalysis(q1Key, 8, "طلب", "طَلَبِ", "Mencari / menuntut", "Isim", "Isim Majrur & Mudhaf", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena didahului oleh huruf jar 'فِي', serta tidak ditanwin karena menjadi Mudhaf.", "Harf Jar 'فِي'.", "Bukan Dhammah atau Fathah karena huruf jar mewajibkan i'rab jar pada isim setelahnya.", "Dipengaruhi oleh huruf jar 'فِي' dan menyandar pada 'الْمَعْرِفَةِ'."),
            buildTokenAnalysis(q1Key, 9, "المعرفة", "الْمَعْرِفَةِ", "Pengetahuan", "Isim", "Mudhaf Ilaih & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi Mudhaf Ilaih bagi kata 'طَلَبِ'.", "Idhafah ('طَلَبِ').", "Bukan rafa' atau nashab karena posisi Mudhaf Ilaih selalu majrur.", "Menjelaskan apa yang dicari pada kata 'طَلَبِ' dan disifati oleh 'النَّافِعَةِ'."),
            buildTokenAnalysis(q1Key, 10, "النافعة", "النَّافِعَةِ", "Yang bermanfaat", "Isim", "Na'at (Sifat)", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi Na'at yang mengikuti i'rab jar dari Man'ut-nya ('الْمَعْرِفَةِ').", "Mengikuti Man'ut ('الْمَعْرِفَةِ').", "Bukan Dhammah atau Fathah karena Man'ut-nya majrur.", "Memberikan sifat 'bermanfaat' terhadap pengetahuan ('الْمَعْرِفَةِ').")
        )
        val q1Pattern = AnswerPattern(
            patternId = "pat_mub_1",
            primary = true,
            label = "Pola Utama: Mubtada' + Khabar + Jumlah Na'tiyah",
            voweledSentence = "الْعِلْمُ نُورٌ يُضِيءُ طَرِيقَ الطَّالِبِ الْمُجْتَهِدِ فِي طَلَبِ الْمَعْرِفَةِ النَّافِعَةِ",
            translationNatural = "Ilmu adalah cahaya yang menerangi jalan pelajar yang bersungguh-sungguh dalam menuntut pengetahuan yang bermanfaat.",
            translationGandul = "Utawi ilmu iku pepadhang kang madhangi ing dalane murid kang temen-temen ing dalem golek kawruh kang migunani.",
            sentenceMeaningExplanation = "Kalimat ini menegaskan kedudukan ilmu sebagai penerang jalan bagi penuntut ilmu yang tekun agar sampai pada pengetahuan yang memberi manfaat.",
            structureDifferenceNote = "Dalam bahasa Arab, kata sifat (Na'at) seperti 'الْمُجْتَهِدِ' dan 'النَّافِعَةِ' diletakkan setelah kata benda yang disifati, sedangkan dalam bahasa Indonesia dihubungkan dengan kata 'yang'.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Ismiyah (diawali Isim 'الْعِلْمُ')",
                mainElements = "Mubtada' (الْعِلْمُ) + Khabar (نُورٌ) + Jumlah Fi'liyah sebagai Na'at (يُضِيءُ طَرِيقَ...)",
                structureHierarchy = listOf(
                    "Jumlah Ismiyah Kubra",
                    "├── Mubtada': الْعِلْمُ (Marfu', Dhammah)",
                    "└── Khabar: نُورٌ (Marfu', Dhammah)",
                    "    └── Jumlah Na'at bagi نُورٌ: يُضِيءُ طَرِيقَ الطَّالِبِ...",
                    "        ├── Fi'il Mudhari': يُضِيءُ (Fa'il: dhomir mustatir هُوَ)",
                    "        ├── Maf'ul Bih & Mudhaf: طَرِيقَ",
                    "        │   └── Mudhaf Ilaih: الطَّالِبِ + Na'at: الْمُجْتَهِدِ",
                    "        └── Jar Majrur (ta'alluq pada يُضِيءُ): فِي طَلَبِ",
                    "            └── Mudhaf Ilaih: الْمَعْرِفَةِ + Na'at: النَّافِعَةِ"
                ),
                elementRelationships = "Mubtada' menuntut Khabar untuk menyempurnakan makna kalimat; kata nakirah 'نُورٌ' kemudian dijelaskan oleh jumlah fi'liyah 'يُضِيءُ...' yang menjadi sifatnya.",
                meaningConstruction = "Pokok kalimat ('Ilmu') diberi predikat ('cahaya'), lalu diperluas maknanya dengan sifat tindakan ('yang menerangi jalan pelajar...')."
            ),
            wordRelations = listOf(
                WordRelation(1, 2, "الْعِلْمُ ↔ نُورٌ", "Mubtada' dan Khabar", "Keduanya wajib Marfu'", "Khabar menyempurnakan berita tentang Mubtada'", "Membentuk inti kalimat nominal 'Ilmu itu cahaya'."),
                WordRelation(3, 4, "يُضِيءُ ↔ طَرِيقَ", "Fi'il dan Maf'ul Bih", "Fi'il menashabkan Maf'ul Bih", "Kata kerja aktif membutuhkan objek sasaran", "Menjelaskan apa yang diterangi (jalan)."),
                WordRelation(4, 5, "طَرِيقَ ↔ الطَّالِبِ", "Mudhaf dan Mudhaf Ilaih", "Mudhaf Ilaih wajib Majrur", "Penyandaran untuk menentukan milik siapa jalan tersebut", "Mengkhususkan arti 'jalan' menjadi 'jalan pelajar'."),
                WordRelation(5, 6, "الطَّالِبِ ↔ الْمُجْتَهِدِ", "Man'ut dan Na'at", "Na'at mengikuti i'rab Jar dari Man'ut", "Sifat harus selaras i'rab dengan yang disifati", "Menambahkan karakter pelajar yang tekun."),
                WordRelation(7, 8, "فِي ↔ طَلَبِ", "Harf Jar dan Isim Majrur", "Huruf jar mewajibkan Kasrah pada isim", "فِي berfungsi sebagai amil jar", "Menunjukkan konteks lokasi maknawi 'dalam menuntut'.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "الْعِلْمُ", "Mubtada'", "Utawi ilmu", "Pokok kalimat (Mubtada' ditandai 'utawi')"),
                GandulEntry(2, "نُورٌ", "Khabar", "iku pepadhang / cahaya", "Predikat kalimat (Khabar ditandai 'iku')"),
                GandulEntry(3, "يُضِيءُ", "Fi'il Na'at", "kang madhangi (ia)", "Jumlah fi'liyah mensifati isim nakirah نُورٌ"),
                GandulEntry(4, "طَرِيقَ", "Maf'ul Bih", "ing dalane", "Objek penderita (ditandai 'ing')"),
                GandulEntry(5, "الطَّالِبِ", "Mudhaf Ilaih", "murid", "Disandari kata طَرِيقَ"),
                GandulEntry(6, "الْمُجْتَهِدِ", "Na'at", "kang temen-temen", "Sifat dari murid (ditandai 'kang')"),
                GandulEntry(7, "فِي", "Harf Jar", "ing dalem", "Keterangan tempat/keadaan"),
                GandulEntry(8, "طَلَبِ", "Majrur", "golek / mencari", "Dimajrurkan oleh فِي"),
                GandulEntry(9, "الْمَعْرِفَةِ", "Mudhaf Ilaih", "kawruh / pengetahuan", "Disandari kata طَلَبِ"),
                GandulEntry(10, "النَّافِعَةِ", "Na'at", "kang migunani", "Sifat bagi pengetahuan")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Mubtada' dan Khabar",
                    definition = "Mubtada' adalah isim marfu' yang berada di awal jumlah ismiyah sebagai pokok pembicaraan, sedangkan Khabar adalah penyempurna makna Mubtada'.",
                    formula = "Mubtada' (Marfu') + Khabar (Marfu')",
                    conditions = "Mubtada' umumnya ma'rifat dan Khabar umumnya nakirah yang sesuai dalam bilangan dan jenis.",
                    irabEffect = "Keduanya wajib berhukum Marfu' (tanda aslinya Dhammah untuk isim mufrad).",
                    applicationInSentence = "Kata 'الْعِلْمُ' (ma'rifat dengan alif-lam) menjadi Mubtada' marfu', dan 'نُورٌ' (nakirah) menjadi Khabar marfu'.",
                    extraExamples = listOf("الْقُرْآنُ هُدًى", "الصَّبْرُ ضِيَاءٌ")
                ),
                KaidahRule(
                    name = "Jumlah setelah Nakirah adalah Sifat (Na'at)",
                    definition = "Kalimat (jumlah ismiyah atau fi'liyah) yang terletak setelah isim nakirah berkedudukan sebagai Na'at (sifat) bagi isim nakirah tersebut.",
                    formula = "Isim Nakirah + Jumlah Fi'liyah = Man'ut + Na'at",
                    conditions = "Kata yang dijelaskan harus berbentuk nakirah murni seperti 'نُورٌ'.",
                    irabEffect = "Jumlah tersebut menempati mahal i'rab sesuai Man'ut-nya (Mahal Rafa').",
                    applicationInSentence = "Jumlah 'يُضِيءُ طَرِيقَ...' berkedudukan di mahal rafa' sebagai sifat bagi 'نُورٌ'.",
                    extraExamples = listOf("رَأَيْتُ رَجُلًا يَكْتُبُ", "هَذَا طَالِبٌ يَجْتَهِدُ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Mubtada' dan Khabar selalu berhukum Marfu'.",
                    "Mudhaf tidak boleh diberi tanwin meskipun tidak beralif-lam (طَرِيقَ dan طَلَبِ).",
                    "Na'at selalu mengikuti i'rab Man'ut-nya."
                ),
                structureConclusion = "Kalimat nominal (Jumlah Ismiyah) yang diperluas dengan jumlah fi'liyah sebagai sifat khabar serta rantai Idhafah dan Jar-Majrur.",
                keyHarakatReasons = listOf(
                    "الْعِلْمُ (Dhammah) = Mubtada' marfu'.",
                    "نُورٌ (Tanwin Dhammah) = Khabar marfu' nakirah.",
                    "طَرِيقَ (Fathah tanpa tanwin) = Maf'ul Bih manshub sekaligus Mudhaf."
                )
            ),
            tokenAnalyses = q1Analyses
        )

        // 2. FA'IL & NAIBUL FA'IL (Beginner - Published)
        // ذهب الطالب المجتهد إلى المدرسة الكبيرة صباحا مع صديقه الكريم (10 kata)
        val q2Key = "q_fai_001"
        val q2Base = listOf("ذهب", "الطالب", "المجتهد", "إلى", "المدرسة", "الكبيرة", "صباحا", "مع", "صديقه", "الكريم")
        val q2Tokens = tokensFromSentence(q2Base, q2Key)
        val q2Analyses = listOf(
            buildTokenAnalysis(q2Key, 1, "ذهب", "ذَهَبَ", "Telah pergi", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi (tetap fathah)", "Fi'il madhi yang tidak bersambung dengan wawu jama' atau dhomir rafa' mutaharrik selalu mabni 'ala al-fath.", "Tidak ada (fi'il madhi tidak terkena amil i'rab).", "Huruf akhirnya bukan alamat rafa' melainkan bina tetap.", "Membutuhkan pelaku (Fa'il) yaitu 'الطَّالِبُ'.", "Mabni 'ala al-Fath", "Hukum asal seluruh fi'il madhi adalah mabni.", "Tidak memiliki mahal i'rab"),
            buildTokenAnalysis(q2Key, 2, "الطالب", "الطَّالِبُ", "Pelajar", "Isim", "Fa'il (Pelaku) & Man'ut", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi (tampak)", "Karena menjadi pelaku (Fa'il) dari kata kerja aktif 'ذَهَبَ'.", "Fi'il ma'lum 'ذَهَبَ'.", "Bukan Fathah atau Kasrah karena Fa'il wajib berhukum Marfu'.", "Menjadi pelaku perbuatan pergi dan disifati oleh 'الْمُجْتَهِدُ'."),
            buildTokenAnalysis(q2Key, 3, "المجتهد", "الْمُجْتَهِدُ", "Yang tekun", "Isim", "Na'at (Sifat)", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi (tampak)", "Mengikuti i'rab rafa' dari Man'ut-nya ('الطَّالِبُ').", "Mengikuti Man'ut ('الطَّالِبُ').", "Bukan Fathah atau Kasrah karena Man'ut-nya marfu'.", "Menjelaskan sifat pelaku 'الطَّالِبُ'."),
            buildTokenAnalysis(q2Key, 4, "إلى", "إِلَى", "Ke", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun (Alif Maqshurah)", "Bina", "Taqdiri / Mahalli", "Seluruh harf hukumnya mabni.", "Amil jar bagi kata sesudahnya.", "Tidak dapat di-i'rab karena golongan huruf.", "Menghubungkan arah tujuan kepergian menuju 'الْمَدْرَسَةِ'.", "Mabni 'ala as-Sukun", "Huruf jar selalu mabni.", "Tidak memiliki mahal i'rab"),
            buildTokenAnalysis(q2Key, 5, "المدرسة", "الْمَدْرَسَةِ", "Sekolah", "Isim", "Isim Majrur & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena didahului oleh huruf jar 'إِلَى'.", "Harf Jar 'إِلَى'.", "Bukan Dhammah atau Fathah karena huruf jar mewajibkan jar.", "Menjadi tujuan dari 'ذَهَبَ' dan disifati oleh 'الْكَبِيرَةِ'."),
            buildTokenAnalysis(q2Key, 6, "الكبيرة", "الْكَبِيرَةِ", "Yang besar", "Isim", "Na'at (Sifat)", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Karena mensifati 'الْمَدْرَسَةِ' yang berhukum majrur.", "Mengikuti Man'ut ('الْمَدْرَسَةِ').", "Bukan rafa' atau nashab karena sifat mengikuti yang disifati.", "Memberikan keterangan ukuran sekolah."),
            buildTokenAnalysis(q2Key, 7, "صباحا", "صَبَاحًا", "Pada pagi hari", "Isim", "Zharaf Zaman (Maf'ul Fih)", "Mu'rab", "Manshub", "Fathah (Tanwin Fathah)", "Tanda Asli", "Lafzhi (tampak)", "Menunjukkan waktu terjadinya perbuatan 'ذَهَبَ' (mengandung makna 'فِي').", "Fi'il 'ذَهَبَ'.", "Bukan Dhammah atau Kasrah karena Zharaf Zaman berhukum Manshub.", "Menjelaskan waktu pelaksanaan kata kerja 'ذَهَبَ'."),
            buildTokenAnalysis(q2Key, 8, "مع", "مَعَ", "Bersama", "Isim", "Zharaf Makan & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi (tampak)", "Menunjukkan kebersamaan tempat/keadaan terjadinya perbuatan dan menjadi Mudhaf.", "Fi'il 'ذَهَبَ'.", "Bukan sukunkan mim atau kasrah pada 'ع' karena posisinya Zharaf manshub.", "Menyandar sebagai Mudhaf kepada 'صَدِيقِهِ'."),
            buildTokenAnalysis(q2Key, 9, "صديقه", "صَدِيقِهِ", "Temannya", "Isim + Dhomir", "Mudhaf Ilaih & Mudhaf", "Mu'rab (Isim) + Mabni (Dhomir)", "Majrur", "Kasrah pada huruf ق", "Tanda Asli", "Lafzhi (pada ق)", "Kata 'صَدِيقِ' menjadi Mudhaf Ilaih bagi 'مَعَ', lalu menjadi Mudhaf bagi dhomir 'ـهِ'.", "Idhafah ('مَعَ').", "Huruf ق wajib kasrah karena majrur; dhomir 'ـهِ' mabni kasrah di mahal jar.", "Menjadi Mudhaf Ilaih bagi 'مَعَ' sekaligus disifati oleh 'الْكَرِيمِ'."),
            buildTokenAnalysis(q2Key, 10, "الكريم", "الْكَرِيمِ", "Yang mulia", "Isim", "Na'at (Sifat)", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi (tampak)", "Mensifati kata 'صَدِيقِ' yang berstatus ma'rifat (karena disandarkan pada dhomir) dan berhukum majrur.", "Mengikuti Man'ut ('صَدِيقِ').", "Bukan rafa' atau nashab karena mengikuti i'rab jar dari 'صَدِيقِ'.", "Menjelaskan akhlak mulia teman yang menemani pelajar tersebut.")
        )
        val q2Pattern = AnswerPattern(
            patternId = "pat_fai_1",
            primary = true,
            label = "Pola Utama: Jumlah Fi'liyah (Fi'il + Fa'il + Jar Majrur + Zharaf)",
            voweledSentence = "ذَهَبَ الطَّالِبُ الْمُجْتَهِدُ إِلَى الْمَدْرَسَةِ الْكَبِيرَةِ صَبَاحًا مَعَ صَدِيقِهِ الْكَرِيمِ",
            translationNatural = "Pelajar yang tekun itu pergi ke sekolah yang besar pada pagi hari bersama temannya yang mulia.",
            translationGandul = "Wis lunga sopo murid kang temen-temen maring sekolah kang gedhe ing dalem wektu esuk sarta kancane murid kang mulya.",
            sentenceMeaningExplanation = "Menggambarkan aktivitas berangkat menuntut ilmu di pagi hari dengan kesungguhan dan ditemani sahabat yang baik.",
            structureDifferenceNote = "Dalam Jumlah Fi'liyah Arab, kata kerja ('ذَهَبَ') diletakkan di awal kalimat mendahului subjek ('الطَّالِبُ').",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah (diawali Fi'il 'ذَهَبَ')",
                mainElements = "Fi'il Madhi (ذَهَبَ) + Fa'il (الطَّالِبُ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il Madhi: ذَهَبَ (Mabni 'ala al-Fath)",
                    "├── Fa'il: الطَّالِبُ (Marfu', Dhammah) + Na'at: الْمُجْتَهِدُ",
                    "├── Jar Majrur: إِلَى الْمَدْرَسَةِ + Na'at: الْكَبِيرَةِ",
                    "├── Maf'ul Fih (Zharaf Zaman): صَبَاحًا (Manshub)",
                    "└── Zharaf Makan & Mudhaf: مَعَ",
                    "    └── Mudhaf Ilaih: صَدِيقِ + Dhomir: هِ + Na'at: الْكَرِيمِ"
                ),
                elementRelationships = "Fi'il membutuhkan Fa'il sebagai rukun inti, lalu dilengkapi oleh keterangan tujuan (Jar Majrur), keterangan waktu (Zharaf Zaman), dan keterangan penyerta (Zharaf 'مَعَ').",
                meaningConstruction = "Tindakan pergi disandarkan pada pelajar tekun dengan detail tujuan, waktu, dan teman."
            ),
            wordRelations = listOf(
                WordRelation(1, 2, "ذَهَبَ ↔ الطَّالِبُ", "Fi'il dan Fa'il", "Fi'il merafa'kan Fa'il", "Setiap fi'il ma'lum membutuhkan pelaku", "Menunjukkan siapa yang melakukan perjalanan."),
                WordRelation(2, 3, "الطَّالِبُ ↔ الْمُجْتَهِدُ", "Man'ut dan Na'at", "Na'at mengikuti i'rab Rafa' dari Man'ut", "Kesesuaian sifat dan yang disifati", "Memperjelas karakter pelajar."),
                WordRelation(4, 5, "إِلَى ↔ الْمَدْرَسَةِ", "Jar dan Majrur", "Harf jar mengkasrahkan isim", "إِلَى menunjukkan arah tujuan", "Menentukan tujuan kepergian."),
                WordRelation(8, 9, "مَعَ ↔ صَدِيقِهِ", "Mudhaf dan Mudhaf Ilaih", "Mudhaf Ilaih berhukum Jar", "Penyandaran keterangan kebersamaan", "Menunjukkan siapa yang menyertai."),
                WordRelation(9, 10, "صَدِيقِهِ ↔ الْكَرِيمِ", "Man'ut dan Na'at", "Na'at mengikuti i'rab Jar dari صَدِيقِ", "صَدِيقِ menjadi ma'rifat karena idhafah pada dhomir sehingga boleh disifati oleh yang beralif-lam", "Menjelaskan sifat teman.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "ذَهَبَ", "Fi'il Madhi", "Wis lunga", "Kata kerja lampau"),
                GandulEntry(2, "الطَّالِبُ", "Fa'il", "sopo murid", "Subjek pelaku (ditandai 'sopo')"),
                GandulEntry(3, "الْمُجْتَهِدُ", "Na'at", "kang temen-temen", "Sifat pelaku"),
                GandulEntry(4, "إِلَى", "Harf Jar", "maring", "Menuju ke"),
                GandulEntry(5, "الْمَدْرَسَةِ", "Majrur", "sekolah", "Tujuan"),
                GandulEntry(6, "الْكَبِيرَةِ", "Na'at", "kang gedhe", "Sifat sekolah"),
                GandulEntry(7, "صَبَاحًا", "Zharaf Zaman", "ing dalem wektu esuk", "Keterangan waktu"),
                GandulEntry(8, "مَعَ", "Zharaf / Mudhaf", "sarta / bebarengan", "Kebersamaan"),
                GandulEntry(9, "صَدِيقِهِ", "Mudhaf Ilaih", "kancane murid", "Kata صَدِيقِ majrur + dhomir هِ"),
                GandulEntry(10, "الْكَرِيمِ", "Na'at", "kang mulya", "Sifat dari teman")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Kaidah Fa'il",
                    definition = "Fa'il adalah isim marfu' yang disebutkan setelah fi'il ma'lum untuk menunjukkan pelaku perbuatan.",
                    formula = "Fi'il Ma'lum + Fa'il (Marfu')",
                    conditions = "Fa'il wajib diletakkan setelah fi'il dan berhukum marfu'.",
                    irabEffect = "Membuat kata pelaku wajib Marfu' (tanda Dhammah pada isim mufrad).",
                    applicationInSentence = "'الطَّالِبُ' terletak tepat setelah 'ذَهَبَ' sehingga wajib dibaca Dhammah sebagai Fa'il.",
                    extraExamples = listOf("جَلَسَ الْمُعَلِّمُ", "نَجَحَ الْمُجْتَهِدُ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Fa'il selalu Marfu' setelah Fi'il Ma'lum.",
                    "Pada kata bersambung dhomir seperti 'صَدِيقِهِ', mahalul i'rab kata dasar berada di huruf akhir kata dasar ('قِ') sebelum dhomir."
                ),
                structureConclusion = "Jumlah Fi'liyah sempurna dengan rukun Fi'il–Fa'il serta pelengkap Jar–Majrur dan Zharaf.",
                keyHarakatReasons = listOf(
                    "الطَّالِبُ (Dhammah) = Fa'il marfu'.",
                    "صَبَاحًا (Tanwin Fathah) = Zharaf Zaman manshub.",
                    "صَدِيقِهِ (Kasrah pada ق) = Mudhaf Ilaih majrur."
                )
            ),
            tokenAnalyses = q2Analyses
        )

        // 3. MAF'UL BIH - MULTI-PATTERN QUESTION! (Intermediate - Published)
        // قرأ التلميذ النشيط كتاب النحو المفيد في مكتبة المعهد الكبيرة (10 kata)
        // Token 6 "المفيد": Pattern 1 (Primary) -> الْمُفِيدَ (Na'at for كِتَابَ, Manshub), Pattern 2 (Alternative) -> الْمُفِيدِ (Na'at for النَّحْوِ, Majrur)
        val q3Key = "q_maf_001"
        val q3Base = listOf("قرأ", "التلميذ", "النشيط", "كتاب", "النحو", "المفيد", "في", "مكتبة", "المعهد", "الكبيرة")
        val q3Tokens = tokensFromSentence(q3Base, q3Key)
        val q3CommonPrefix = listOf(
            buildTokenAnalysis(q3Key, 1, "قرأ", "قَرَأَ", "Telah membaca", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi", "Fi'il madhi tsulatsi mujarrad yang tidak bersambung dhomir rafa'.", "Tidak ada", "Fi'il madhi mabni selamanya.", "Menuntut Fa'il 'التِّلْمِيذُ' dan Maf'ul Bih 'كِتَابَ'.", "Mabni 'ala al-Fath", "Hukum asal fi'il madhi", "Tidak ada mahal"),
            buildTokenAnalysis(q3Key, 2, "التلميذ", "التِّلْمِيذُ", "Siswa", "Isim", "Fa'il & Man'ut", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Menjadi pelaku perbuatan membaca.", "Fi'il 'قَرَأَ'.", "Bukan manshub karena merupakan subjek aktif.", "Pelaku bagi 'قَرَأَ' dan man'ut bagi 'النَّشِيطُ'."),
            buildTokenAnalysis(q3Key, 3, "النشيط", "النَّشِيطُ", "Yang rajin", "Isim", "Na'at (Sifat)", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mengikuti i'rab rafa' dari 'التِّلْمِيذُ'.", "Man'ut ('التِّلْمِيذُ').", "Sifat tidak boleh berbeda i'rab dari yang disifati.", "Mensifati siswa."),
            buildTokenAnalysis(q3Key, 4, "كتاب", "كِتَابَ", "Buku / Kitab", "Isim", "Maf'ul Bih & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Menjadi objek yang dibaca (Maf'ul Bih) dan menjadi Mudhaf.", "Fi'il 'قَرَأَ'.", "Bukan Dhammah karena menjadi objek, dan tanpa tanwin karena mudhaf.", "Objek dari 'قَرَأَ' dan disandarkan pada 'النَّحْوِ'."),
            buildTokenAnalysis(q3Key, 5, "النحو", "النَّحْوِ", "Ilmu Nahwu", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Menjadi Mudhaf Ilaih dari 'كِتَابَ'.", "Idhafah ('كِتَابَ').", "Mudhaf Ilaih wajib majrur.", "Menjelaskan jenis buku tersebut.")
        )
        val q3CommonSuffix = listOf(
            buildTokenAnalysis(q3Key, 7, "في", "فِي", "Di", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Mahalli", "Huruf jar mabni sukun.", "Amil jar.", "Huruf tidak berubah akhir.", "Menghubungkan lokasi pembacaan.", "Mabni 'ala as-Sukun", "Harf selalu mabni", "Tidak ada mahal"),
            buildTokenAnalysis(q3Key, 8, "مكتبة", "مَكْتَبَةِ", "Perpustakaan", "Isim", "Isim Majrur & Mudhaf", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Didahului huruf jar 'فِي' dan menjadi Mudhaf bagi 'الْمَعْهَدِ'.", "Harf Jar 'فِي'.", "Bukan fathah/dhammah karena isim majrur.", "Tempat terjadinya aktivitas membaca."),
            buildTokenAnalysis(q3Key, 9, "المعهد", "الْمَعْهَدِ", "Pesantren / Lembaga", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih dari 'مَكْتَبَةِ'.", "Idhafah ('مَكْتَبَةِ').", "Wajib majrur.", "Menentukan milik siapa perpustakaan tersebut."),
            buildTokenAnalysis(q3Key, 10, "الكبيرة", "الْكَبِيرَةِ", "Yang besar", "Isim", "Na'at bagi مَكْتَبَةِ", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mensifati kata muannats 'مَكْتَبَةِ' yang berhukum majrur.", "Man'ut ('مَكْتَبَةِ').", "Mengikuti jenis muannats dan i'rab jar dari 'مَكْتَبَةِ' (bukan 'الْمَعْهَدِ' karena الْمَعْهَدِ mudzakkar).", "Menjelaskan ukuran perpustakaan.")
        )
        val q3PatternPrimary = AnswerPattern(
            patternId = "pat_maf_primary",
            primary = true,
            label = "Pola Utama: الْمُفِيدَ (Fathah) sebagai Na'at bagi كِتَابَ",
            voweledSentence = "قَرَأَ التِّلْمِيذُ النَّشِيطُ كِتَابَ النَّحْوِ الْمُفِيدَ فِي مَكْتَبَةِ الْمَعْهَدِ الْكَبِيرَةِ",
            translationNatural = "Siswa yang rajin itu membaca buku Nahwu yang bermanfaat di perpustakaan pesantren yang besar.",
            translationGandul = "Wis maca sopo murid kang sregep ing kitab Nahwu kang migunani ing dalem perpustakaane pondok kang gedhe.",
            sentenceMeaningExplanation = "Pada pola utama ini, kata 'الْمُفِيدَ' mensifati kata 'كِتَابَ' (buku yang bermanfaat), sehingga berhukum Manshub mengikuti 'كِتَابَ'.",
            structureDifferenceNote = "Kata sifat dapat merujuk kepada Mudhaf ('كِتَابَ' -> Manshub) atau kepada Mudhaf Ilaih ('النَّحْوِ' -> Majrur) karena keduanya sama-sama mudzakkar dan ma'rifat.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah",
                mainElements = "Fi'il (قَرَأَ) + Fa'il (التِّلْمِيذُ) + Maf'ul Bih (كِتَابَ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il Madhi: قَرَأَ",
                    "├── Fa'il: التِّلْمِيذُ + Na'at: النَّشِيطُ",
                    "├── Maf'ul Bih & Mudhaf: كِتَابَ",
                    "│   ├── Mudhaf Ilaih: النَّحْوِ",
                    "│   └── Na'at bagi كِتَابَ: الْمُفِيدَ (Manshub, Fathah)",
                    "└── Jar Majrur: فِي مَكْتَبَةِ الْمَعْهَدِ الْكَبِيرَةِ"
                ),
                elementRelationships = "Na'at 'الْمُفِيدَ' melompati Mudhaf Ilaih 'النَّحْوِ' untuk mensifati Mudhaf 'كِتَابَ' (karena Mudhaf menjadi ma'rifat lewat idhafah).",
                meaningConstruction = "Yang disifati 'bermanfaat' adalah bukunya."
            ),
            wordRelations = listOf(
                WordRelation(1, 4, "قَرَأَ ↔ كِتَابَ", "Fi'il dan Maf'ul Bih", "Fi'il menashabkan Maf'ul Bih", "Objek bacaan", "Menunjukkan sasaran bacaan."),
                WordRelation(4, 6, "كِتَابَ ↔ الْمُفِيدَ", "Man'ut dan Na'at", "Na'at ikut Manshub (Fathah)", "كِتَابَ ma'rifat karena idhafah sehingga bisa menerima Na'at beralif-lam", "Menyatakan bukunya bermanfaat.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "قَرَأَ", "Fi'il Madhi", "Wis maca", "Kata kerja lampau"),
                GandulEntry(2, "التِّلْمِيذُ", "Fa'il", "sopo murid", "Pelaku"),
                GandulEntry(3, "النَّشِيطُ", "Na'at", "kang sregep", "Sifat murid"),
                GandulEntry(4, "كِتَابَ", "Maf'ul Bih", "ing kitab", "Objek"),
                GandulEntry(5, "النَّحْوِ", "Mudhaf Ilaih", "ilmu Nahwu", "Penyandaran"),
                GandulEntry(6, "الْمُفِيدَ", "Na'at bagi كِتَابَ", "kang migunani", "Sifat bagi kitab (manshub)"),
                GandulEntry(7, "فِي", "Harf Jar", "ing dalem", "Tempat"),
                GandulEntry(8, "مَكْتَبَةِ", "Majrur", "perpustakaane", "Isim majrur"),
                GandulEntry(9, "الْمَعْهَدِ", "Mudhaf Ilaih", "pondok", "Pemilik perpustakaan"),
                GandulEntry(10, "الْكَبِيرَةِ", "Na'at", "kang gedhe", "Sifat perpustakaan")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Na'at bagi Mudhaf vs Mudhaf Ilaih",
                    definition = "Apabila suatu kata menjadi Mudhaf kepada kata ma'rifat, maka Mudhaf tersebut ikut menjadi ma'rifat. Jika datang Na'at setelah Mudhaf Ilaih yang cocok jenis & bilangannya dengan keduanya, maka kedua pembacaan sah secara Nahwu.",
                    formula = "Mudhaf (Manshub) + Mudhaf Ilaih (Majrur) + Na'at (Manshub ATAU Majrur)",
                    conditions = "Kesesuaian mudzakkar/muannats dan mufrad/mutsanna/jamak antara Na'at dan kata yang disifati.",
                    irabEffect = "Dibaca Fathah jika mensifati Mudhaf 'كِتَابَ', dan dibaca Kasrah jika mensifati Mudhaf Ilaih 'النَّحْوِ'.",
                    applicationInSentence = "Pada pola ini, 'الْمُفِيدَ' dibaca Fathah karena mensifati 'كِتَابَ'.",
                    extraExamples = listOf("رَأَيْتُ بَابَ الْبَيْتِ الْجَمِيلَ / الْجَمِيلِ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Maf'ul Bih selalu Manshub.",
                    "Sebuah kalimat dapat memiliki lebih dari satu pola analisis Nahwu yang sama-sama sah."
                ),
                structureConclusion = "Jumlah Fi'liyah dengan Maf'ul Bih berbentuk Idhafah dan Na'at yang memiliki kemungkinan rujukan ganda.",
                keyHarakatReasons = listOf(
                    "كِتَابَ (Fathah) = Maf'ul Bih manshub.",
                    "الْمُفِيدَ (Fathah) = Na'at mengikuti كِتَابَ."
                )
            ),
            tokenAnalyses = q3CommonPrefix + listOf(
                buildTokenAnalysis(q3Key, 6, "المفيد", "الْمُفِيدَ", "Yang bermanfaat", "Isim", "Na'at bagi كِتَابَ (Maf'ul Bih)", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Menjadi Na'at bagi kata 'كِتَابَ' yang manshub sebagai Maf'ul Bih.", "Mengikuti Man'ut ('كِتَابَ').", "Bukan Dhammah karena tidak mensifati Fa'il; boleh pula dibaca Kasrah jika mensifati 'النَّحْوِ' (pola alternatif).", "Mensifati 'كِتَابَ'.")
            ) + q3CommonSuffix
        )

        val q3PatternAlt = AnswerPattern(
            patternId = "pat_maf_alt_majrur",
            primary = false,
            label = "Pola Alternatif Sah: الْمُفِيدِ (Kasrah) sebagai Na'at bagi النَّحْوِ",
            voweledSentence = "قَرَأَ التِّلْمِيذُ النَّشِيطُ كِتَابَ النَّحْوِ الْمُفِيدِ فِي مَكْتَبَةِ الْمَعْهَدِ الْكَبِيرَةِ",
            translationNatural = "Siswa yang rajin itu membaca buku ilmu Nahwu yang bermanfaat di perpustakaan pesantren yang besar.",
            translationGandul = "Wis maca sopo murid kang sregep ing kitabe ilmu Nahwu kang migunani ing dalem perpustakaane pondok kang gedhe.",
            sentenceMeaningExplanation = "Pada pola alternatif yang sah ini, kata 'الْمُفِيدِ' mensifati kata terdekatnya yaitu 'النَّحْوِ' (ilmu Nahwu yang bermanfaat), sehingga berhukum Majrur.",
            structureDifferenceNote = "Baik membaca الْمُفِيدَ (sifat bagi kitab) maupun الْمُفِيدِ (sifat bagi ilmu Nahwu) keduanya sah dalam kaidah Nahwu.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah",
                mainElements = "Fi'il (قَرَأَ) + Fa'il (التِّلْمِيذُ) + Maf'ul Bih (كِتَابَ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il Madhi: قَرَأَ",
                    "├── Fa'il: التِّلْمِيذُ + Na'at: النَّشِيطُ",
                    "├── Maf'ul Bih & Mudhaf: كِتَابَ",
                    "│   └── Mudhaf Ilaih: النَّحْوِ + Na'at bagi النَّحْوِ: الْمُفِيدِ (Majrur, Kasrah)",
                    "└── Jar Majrur: فِي مَكْتَبَةِ الْمَعْهَدِ الْكَبِيرَةِ"
                ),
                elementRelationships = "Na'at 'الْمُفِيدِ' mengikuti Mudhaf Ilaih 'النَّحْوِ' yang tepat berada di depannya.",
                meaningConstruction = "Yang disifati 'bermanfaat' adalah ilmu Nahwunya."
            ),
            wordRelations = listOf(
                WordRelation(5, 6, "النَّحْوِ ↔ الْمُفِيدِ", "Man'ut dan Na'at", "Na'at ikut Majrur (Kasrah)", "النَّحْوِ adalah isim ma'rifat mudzakkar majrur", "Menyatakan ilmu Nahwu itu bermanfaat.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "قَرَأَ", "Fi'il Madhi", "Wis maca", "Kata kerja lampau"),
                GandulEntry(2, "التِّلْمِيذُ", "Fa'il", "sopo murid", "Pelaku"),
                GandulEntry(3, "النَّشِيطُ", "Na'at", "kang sregep", "Sifat murid"),
                GandulEntry(4, "كِتَابَ", "Maf'ul Bih", "ing kitabe", "Objek"),
                GandulEntry(5, "النَّحْوِ", "Mudhaf Ilaih", "ilmu Nahwu", "Penyandaran"),
                GandulEntry(6, "الْمُفِيدِ", "Na'at bagi النَّحْوِ", "kang migunani", "Sifat bagi Nahwu (majrur)"),
                GandulEntry(7, "فِي", "Harf Jar", "ing dalem", "Tempat"),
                GandulEntry(8, "مَكْتَبَةِ", "Majrur", "perpustakaane", "Isim majrur"),
                GandulEntry(9, "الْمَعْهَدِ", "Mudhaf Ilaih", "pondok", "Pemilik perpustakaan"),
                GandulEntry(10, "الْكَبِيرَةِ", "Na'at", "kang gedhe", "Sifat perpustakaan")
            ),
            kaidahList = q3PatternPrimary.kaidahList,
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Na'at dapat merujuk kepada Mudhaf Ilaih terdekat apabila sesuai jenis dan bilangannya.",
                    "Sistem menerima kedua pola analisis Nahwu yang sah ini sebagai jawaban benar."
                ),
                structureConclusion = "Jumlah Fi'liyah dengan pola Na'at pada Mudhaf Ilaih.",
                keyHarakatReasons = listOf("الْمُفِيدِ (Kasrah) = Na'at mengikuti Mudhaf Ilaih النَّحْوِ.")
            ),
            tokenAnalyses = q3CommonPrefix + listOf(
                buildTokenAnalysis(q3Key, 6, "المفيد", "الْمُفِيدِ", "Yang bermanfaat", "Isim", "Na'at bagi النَّحْوِ (Mudhaf Ilaih)", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Menjadi Na'at bagi kata 'النَّحْوِ' yang majrur sebagai Mudhaf Ilaih.", "Mengikuti Man'ut ('النَّحْوِ').", "Dibaca Kasrah mengikuti Man'ut terdekatnya; juga sah dibaca Fathah jika mensifati 'كِتَابَ'.", "Mensifati 'النَّحْوِ'.")
            ) + q3CommonSuffix
        )

        // 4. NA'AT & MAN'UT dengan Tanda I'rab Far'i (Ya pada Jamak Mudzakkar Salim) (Intermediate - Published)
        // يكرم الأستاذ الفاضل الطلاب المجتهدين في الدرس الصباحي كل يوم (10 kata)
        val q4Key = "q_naa_001"
        val q4Base = listOf("يكرم", "الأستاذ", "الفاضل", "الطلاب", "المجتهدين", "في", "الدرس", "الصباحي", "كل", "يوم")
        val q4Tokens = tokensFromSentence(q4Base, q4Key)
        val q4Analyses = listOf(
            buildTokenAnalysis(q4Key, 1, "يكرم", "يُكْرِمُ", "Memuliakan", "Fi'il Mudhari'", "Fi'il Mudhari'", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Fi'il mudhari' terbebas dari nashib dan jazim.", "Amil tajarrud.", "Bukan Fathah atau Sukun.", "Memerlukan Fa'il 'الأُسْتَاذُ' dan Maf'ul Bih 'الطُّلَّابَ'."),
            buildTokenAnalysis(q4Key, 2, "الأستاذ", "الأُسْتَاذُ", "Ustadz / Guru", "Isim", "Fa'il & Man'ut", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Pelaku yang memuliakan.", "Fi'il 'يُكْرِمُ'.", "Fa'il tidak boleh manshub.", "Disifati oleh 'الْفَاضِلُ'."),
            buildTokenAnalysis(q4Key, 3, "الفاضل", "الْفَاضِلُ", "Yang utama / mulia", "Isim", "Na'at", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mengikuti Man'ut 'الأُسْتَاذُ' dalam keadaan rafa'.", "Man'ut ('الأُسْتَاذُ').", "Mengikuti rafa' bukan nashab.", "Mensifati ustadz."),
            buildTokenAnalysis(q4Key, 4, "الطلاب", "الطُّلَّابَ", "Para pelajar", "Isim (Jamak Taksir)", "Maf'ul Bih & Man'ut", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Jamak taksir tanda nashabnya Fathah seperti isim mufrad.", "Fi'il 'يُكْرِمُ'.", "Bukan Dhammah karena menjadi objek.", "Disifati oleh 'الْمُجْتَهِدِينَ'."),
            buildTokenAnalysis(q4Key, 5, "المجتهدين", "الْمُجْتَهِدِينَ", "Yang bersungguh-sungguh", "Isim (Jamak Mudzakkar Salim)", "Na'at", "Mu'rab", "Manshub", "Ya (ي)", "Tanda Pengganti (Far'i)", "Lafzhi (huruf ي sebelum ن)", "Mengikuti Man'ut 'الطُّلَّابَ' yang manshub; karena termasuk Jamak Mudzakkar Salim maka tanda nashabnya adalah Ya (dan nun berharakat Fathah).", "Man'ut ('الطُّلَّابَ').", "Bukan Wawu ('الْمُجْتَهِدُونَ') karena Wawu adalah tanda rafa', sedangkan kata ini mengikuti objek yang manshub.", "Mensifati para pelajar; huruf akhir sebelum tanda jamak menunjukkan nashab dengan Ya dan nun difathah."),
            buildTokenAnalysis(q4Key, 6, "في", "فِي", "Dalam / di", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Mahalli", "Harf jar mabni sukun.", "Amil jar.", "Huruf tetap mabni.", "Menghubungkan dengan 'الدَّرْسِ'.", "Mabni 'ala as-Sukun", "Harf mabni", "Tidak ada mahal"),
            buildTokenAnalysis(q4Key, 7, "الدرس", "الدَّرْسِ", "Pelajaran", "Isim", "Isim Majrur & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Didahului huruf jar 'فِي'.", "Harf Jar 'فِي'.", "Wajib jar.", "Disifati oleh 'الصَّبَاحِيِّ'."),
            buildTokenAnalysis(q4Key, 8, "الصباحي", "الصَّبَاحِيِّ", "Pagi hari", "Isim Nisbah", "Na'at", "Mu'rab", "Majrur", "Kasrah (dengan Tasydid pada ي)", "Tanda Asli", "Lafzhi", "Mengikuti i'rab jar dari 'الدَّرْسِ', dengan ya nisbah bertasydid.", "Man'ut ('الدَّرْسِ').", "Ya nisbah wajib bertasydid dan berharakat kasrah di sini.", "Mensifati waktu pelajaran."),
            buildTokenAnalysis(q4Key, 9, "كل", "كُلَّ", "Setiap", "Isim", "Na'ib 'an Zharaf Zaman & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Kata 'كُلَّ' ketika disandarkan pada isim waktu ('يَوْمٍ') menggantikan posisi Zharaf Zaman sehingga berhukum Manshub.", "Fi'il 'يُكْرِمُ'.", "Bukan Dhammah atau Kasrah karena menempati kedudukan Maf'ul Fih.", "Menyandar sebagai Mudhaf kepada 'يَوْمٍ'."),
            buildTokenAnalysis(q4Key, 10, "يوم", "يَوْمٍ", "Hari", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah (Tanwin Kasrah)", "Tanda Asli", "Lafzhi", "Menjadi Mudhaf Ilaih bagi kata 'كُلَّ'.", "Idhafah ('كُلَّ').", "Mudhaf Ilaih wajib majrur.", "Menunjukkan satuan waktu yang diulang setiap hari.")
        )
        val q4Pattern = AnswerPattern(
            patternId = "pat_naa_1",
            primary = true,
            label = "Pola Utama: Na'at Jamak Mudzakkar Salim mengikuti Jamak Taksir",
            voweledSentence = "يُكْرِمُ الأُسْتَاذُ الْفَاضِلُ الطُّلَّابَ الْمُجْتَهِدِينَ فِي الدَّرْسِ الصَّبَاحِيِّ كُلَّ يَوْمٍ",
            translationNatural = "Ustadz yang mulia itu memuliakan para pelajar yang bersungguh-sungguh dalam pelajaran pagi setiap hari.",
            translationGandul = "Mulyakake sopo guru kang utama ing pira-pira murid kang temen-temen ing dalem wulangan wektu esuk ing saben-saben dina.",
            sentenceMeaningExplanation = "Menunjukkan perbedaan tanda i'rab antara Jamak Taksir (الطُّلَّابَ dengan Fathah) dan Jamak Mudzakkar Salim (الْمُجْتَهِدِينَ dengan huruf Ya).",
            structureDifferenceNote = "Meskipun tanda lahiriah الطُّلَّابَ (Fathah) dan الْمُجْتَهِدِينَ (Ya) berbeda bentuk, keduanya sama-sama berada dalam hukum i'rab Manshub!",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah (Fi'il Mudhari')",
                mainElements = "Fi'il (يُكْرِمُ) + Fa'il (الأُسْتَاذُ) + Maf'ul Bih (الطُّلَّابَ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il Mudhari': يُكْرِمُ",
                    "├── Fa'il: الأُسْتَاذُ + Na'at: الْفَاضِلُ",
                    "├── Maf'ul Bih: الطُّلَّابَ (Jamak Taksir, Manshub Fathah)",
                    "│   └── Na'at: الْمُجْتَهِدِينَ (Jamak Mudzakkar Salim, Manshub Ya)",
                    "├── Jar Majrur: فِي الدَّرْسِ + Na'at: الصَّبَاحِيِّ",
                    "└── Na'ib 'an Zharaf: كُلَّ + Mudhaf Ilaih: يَوْمٍ"
                ),
                elementRelationships = "Na'at mengikuti hukum i'rab Man'ut (Manshub), bukan selalu harus sama huruf akhirnya jika jenis katanya berbeda (Jamak Taksir vs Jamak Mudzakkar Salim).",
                meaningConstruction = "Menjelaskan penghargaan berkelanjutan setiap hari kepada siswa berprestasi."
            ),
            wordRelations = listOf(
                WordRelation(4, 5, "الطُّلَّابَ ↔ الْمُجْتَهِدِينَ", "Man'ut dan Na'at", "Sama-sama Manshub (Fathah vs Ya)", "Jamak taksir tanda nashabnya Fathah, Jamak Mudzakkar Salim tanda nashabnya Ya", "Menyatukan makna 'para pelajar yang tekun'."),
                WordRelation(9, 10, "كُلَّ ↔ يَوْمٍ", "Na'ib Zharaf dan Mudhaf Ilaih", "كُلَّ manshub, يَوْمٍ majrur", "Idhafah pada isim waktu", "Membentuk keterangan frekuensi 'setiap hari'.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "يُكْرِمُ", "Fi'il Mudhari'", "Mulyakake", "Sedang/selalu memuliakan"),
                GandulEntry(2, "الأُسْتَاذُ", "Fa'il", "sopo guru", "Pelaku"),
                GandulEntry(3, "الْفَاضِلُ", "Na'at", "kang utama", "Sifat guru"),
                GandulEntry(4, "الطُّلَّابَ", "Maf'ul Bih", "ing pira-pira murid", "Objek jamak"),
                GandulEntry(5, "الْمُجْتَهِدِينَ", "Na'at", "kang temen-temen", "Sifat murid (manshub dengan Ya)"),
                GandulEntry(6, "فِي", "Harf Jar", "ing dalem", "Konteks"),
                GandulEntry(7, "الدَّرْسِ", "Majrur", "wulangan / pelajaran", "Isim majrur"),
                GandulEntry(8, "الصَّبَاحِيِّ", "Na'at", "kang bangsa esuk", "Sifat pelajaran"),
                GandulEntry(9, "كُلَّ", "Na'ib Zharaf", "ing saben-saben", "Pengganti zharaf"),
                GandulEntry(10, "يَوْمٍ", "Mudhaf Ilaih", "dina", "Isim waktu")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Tanda I'rab Pengganti (Far'i) pada Jamak Mudzakkar Salim",
                    definition = "Jamak Mudzakkar Salim di-rafa'-kan dengan Wawu, dan di-nashab-kan serta di-jar-kan dengan Ya yang huruf sebelumnya kasrah dan nun-nya fathah.",
                    formula = "Rafa': ـُونَ | Nashab & Jar: ـِينَ",
                    conditions = "Mengikuti hukum i'rab tempatnya berada (sebagai Na'at dari Maf'ul Bih).",
                    irabEffect = "Menggunakan Ya sebagai tanda pengganti dari Fathah.",
                    applicationInSentence = "Kata 'الْمُجْتَهِدِينَ' mensifati 'الطُّلَّابَ' yang manshub sehingga wajib diakhiri Ya+Nun berfathah.",
                    extraExamples = listOf("أَحْبَبْتُ الْمُؤْمِنِينَ", "رَضِيَ اللهُ عَنِ الصَّابِرِينَ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Kesesuaian Na'at adalah pada hukum i'rabnya (Manshub), walaupun tandanya berbeda (Fathah vs Ya).",
                    "Kata 'كُلَّ' yang disandarkan pada kata waktu menjadi Na'ib 'an Zharaf (Manshub)."
                ),
                structureConclusion = "Jumlah Fi'liyah yang memadukan tanda i'rab asli dan far'i serta Na'ib 'an Zharaf.",
                keyHarakatReasons = listOf(
                    "الْمُجْتَهِدِينَ (Nun difathah, huruf sebelum Ya dikasrah) = Tanda nashab jamak mudzakkar salim.",
                    "كُلَّ (Fathah) = Na'ib 'an Zharaf manshub."
                )
            ),
            tokenAnalyses = q4Analyses
        )

        // 5. MUDHAF & MUDHAF ILAIH (Intermediate - Published)
        // فتح حارس بوابة المسجد الكبير أبواب القاعة الواسعة أمام الضيوف (10 kata)
        val q5Key = "q_mud_001"
        val q5Base = listOf("فتح", "حارس", "بوابة", "المسجد", "الكبير", "أبواب", "القاعة", "الواسعة", "أمام", "الضيوف")
        val q5Tokens = tokensFromSentence(q5Base, q5Key)
        val q5Analyses = listOf(
            buildTokenAnalysis(q5Key, 1, "فتح", "فَتَحَ", "Telah membuka", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi", "Fi'il madhi mabni 'ala al-fath.", "Tidak ada", "Bukan mu'rab.", "Membutuhkan Fa'il 'حَارِسُ'.", "Mabni 'ala al-Fath", "Asal fi'il madhi", "Tidak ada"),
            buildTokenAnalysis(q5Key, 2, "حارس", "حَارِسُ", "Penjaga", "Isim", "Fa'il & Mudhaf", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Pelaku perbuatan membuka dan tidak ditanwin karena menjadi Mudhaf.", "Fi'il 'فَتَحَ'.", "Tanpa tanwin karena idhafah.", "Bersandar pada 'بَوَّابَةِ'."),
            buildTokenAnalysis(q5Key, 3, "بوابة", "بَوَّابَةِ", "Gerbang", "Isim", "Mudhaf Ilaih & Mudhaf", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Menjadi Mudhaf Ilaih bagi 'حَارِسُ' sekaligus menjadi Mudhaf bagi 'الْمَسْجِدِ'.", "Idhafah ('حَارِسُ').", "Tidak ditanwin karena menjadi mudhaf.", "Menghubungkan penjaga dengan masjid."),
            buildTokenAnalysis(q5Key, 4, "المسجد", "الْمَسْجِدِ", "Masjid", "Isim", "Mudhaf Ilaih & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih dari 'بَوَّابَةِ'.", "Idhafah ('بَوَّابَةِ').", "Wajib majrur.", "Disifati oleh 'الْكَبِيرِ'."),
            buildTokenAnalysis(q5Key, 5, "الكبير", "الْكَبِيرِ", "Yang besar", "Isim", "Na'at bagi الْمَسْجِدِ", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mensifati kata mudzakkar 'الْمَسْجِدِ' yang majrur.", "Man'ut ('الْمَسْجِدِ').", "Tidak mungkin mensifati 'بَوَّابَةِ' karena 'بَوَّابَةِ' muannats.", "Menjelaskan sifat masjid."),
            buildTokenAnalysis(q5Key, 6, "أبواب", "أَبْوَابَ", "Pintu-pintu", "Isim (Jamak Taksir)", "Maf'ul Bih & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Objek yang dibuka oleh penjaga dan menjadi Mudhaf.", "Fi'il 'فَتَحَ'.", "Bukan Dhammah karena menjadi Maf'ul Bih.", "Disandarkan pada 'الْقَاعَةِ'."),
            buildTokenAnalysis(q5Key, 7, "القاعة", "الْقَاعَةِ", "Aula", "Isim", "Mudhaf Ilaih & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih bagi 'أَبْوَابَ'.", "Idhafah ('أَبْوَابَ').", "Wajib kasrah.", "Disifati oleh 'الْوَاسِعَةِ'."),
            buildTokenAnalysis(q5Key, 8, "الواسعة", "الْوَاسِعَةِ", "Yang luas", "Isim", "Na'at bagi الْقَاعَةِ", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mensifati kata mufrad muannats 'الْقَاعَةِ' yang majrur.", "Man'ut ('الْقَاعَةِ').", "Mengikuti i'rab jar.", "Menjelaskan luasnya aula."),
            buildTokenAnalysis(q5Key, 9, "أمام", "أَمَامَ", "Di depan", "Isim", "Zharaf Makan & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Keterangan tempat terjadinya pembukaan pintu dan menjadi Mudhaf.", "Fi'il 'فَتَحَ'.", "Zharaf Makan berhukum Manshub.", "Disandarkan kepada 'الضُّيُوفِ'."),
            buildTokenAnalysis(q5Key, 10, "الضيوف", "الضُّيُوفِ", "Para tamu", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih setelah Zharaf 'أَمَامَ'.", "Idhafah ('أَمَامَ').", "Setiap isim yang menyandar setelah zharaf umumnya majrur sebagai Mudhaf Ilaih.", "Menjelaskan lokasi di hadapan tamu.")
        )
        val q5Pattern = AnswerPattern(
            patternId = "pat_mud_1",
            primary = true,
            label = "Pola Utama: Idhafah Bertingkat (Mudhaf + Mudhaf Ilaih)",
            voweledSentence = "فَتَحَ حَارِسُ بَوَّابَةِ الْمَسْجِدِ الْكَبِيرِ أَبْوَابَ الْقَاعَةِ الْوَاسِعَةِ أَمَامَ الضُّيُوفِ",
            translationNatural = "Penjaga gerbang masjid yang besar itu membuka pintu-pintu aula yang luas di hadapan para tamu.",
            translationGandul = "Wis mbukak sopo penjaga gerbange masjid kang gedhe ing pira-pira lawange aula kang jembar ing ngarepe pira-pira tamu.",
            sentenceMeaningExplanation = "Memperlihatkan rantai penyandaran (Idhafah berantai) baik pada posisi Fa'il, Maf'ul Bih, maupun sesudah Zharaf Makan.",
            structureDifferenceNote = "Setiap kata tengah dalam rantai Idhafah (seperti بَوَّابَةِ) bisa sekaligus menjadi Mudhaf Ilaih bagi kata sebelumnya dan Mudhaf bagi kata setelahnya.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah",
                mainElements = "Fi'il (فَتَحَ) + Fa'il (حَارِسُ) + Maf'ul Bih (أَبْوَابَ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il: فَتَحَ",
                    "├── Fa'il (Idhafah Berantai): حَارِسُ بَوَّابَةِ الْمَسْجِدِ الْكَبِيرِ",
                    "├── Maf'ul Bih (Idhafah): أَبْوَابَ الْقَاعَةِ الْوَاسِعَةِ",
                    "└── Zharaf Makan (Idhafah): أَمَامَ الضُّيُوفِ"
                ),
                elementRelationships = "Tiga kelompok Idhafah berpadu dalam satu kalimat verbal.",
                meaningConstruction = "Subjek spesifik membuka objek spesifik di lokasi spesifik."
            ),
            wordRelations = listOf(
                WordRelation(2, 3, "حَارِسُ ↔ بَوَّابَةِ", "Mudhaf dan Mudhaf Ilaih", "Mudhaf tanpa tanwin, Mudhaf Ilaih majrur", "Idhafah pertama", "Penjaga gerbang."),
                WordRelation(3, 4, "بَوَّابَةِ ↔ الْمَسْجِدِ", "Mudhaf dan Mudhaf Ilaih", "Keduanya majrur karena alasan berbeda", "Idhafah kedua", "Gerbang masjid."),
                WordRelation(9, 10, "أَمَامَ ↔ الضُّيُوفِ", "Zharaf Mudhaf dan Mudhaf Ilaih", "أَمَامَ manshub, الضُّيُوفِ majrur", "Isim setelah zharaf umumnya mudhaf ilaih", "Di hadapan para tamu.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "فَتَحَ", "Fi'il Madhi", "Wis mbukak", "Membuka"),
                GandulEntry(2, "حَارِسُ", "Fa'il & Mudhaf", "sopo penjaga", "Pelaku"),
                GandulEntry(3, "بَوَّابَةِ", "Mudhaf Ilaih & Mudhaf", "gerbange", "Rantai idhafah"),
                GandulEntry(4, "الْمَسْجِدِ", "Mudhaf Ilaih", "masjid", "Pemilik gerbang"),
                GandulEntry(5, "الْكَبِيرِ", "Na'at", "kang gedhe", "Sifat masjid"),
                GandulEntry(6, "أَبْوَابَ", "Maf'ul Bih & Mudhaf", "ing pira-pira lawange", "Objek"),
                GandulEntry(7, "الْقَاعَةِ", "Mudhaf Ilaih", "aula", "Pemilik pintu"),
                GandulEntry(8, "الْوَاسِعَةِ", "Na'at", "kang jembar", "Sifat aula"),
                GandulEntry(9, "أَمَامَ", "Zharaf Makan", "ing ngarepe", "Tempat"),
                GandulEntry(10, "الضُّيُوفِ", "Mudhaf Ilaih", "pira-pira tamu", "Isim setelah zharaf")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Idhafah Berantai (Tadakhul Idhafah)",
                    definition = "Dalam bahasa Arab, satu isim dapat menjadi Mudhaf Ilaih sekaligus menjadi Mudhaf untuk kata berikutnya.",
                    formula = "Mudhaf (sesuai kedudukan) + Mudhaf Ilaih/Mudhaf (Majrur tanpa tanwin) + Mudhaf Ilaih akhir (Majrur)",
                    conditions = "Mudhaf harus bersih dari alif-lam dan tanwin.",
                    irabEffect = "Semua kata yang berada pada posisi Mudhaf Ilaih wajib berhukum Majrur.",
                    applicationInSentence = "Kata 'بَوَّابَةِ' majrur karena menjadi Mudhaf Ilaih bagi 'حَارِسُ', dan tidak bertanwin karena menjadi Mudhaf bagi 'الْمَسْجِدِ'.",
                    extraExamples = listOf("مِفْتَاحُ بَابِ الدَّارِ", "مُدِيرُ مَكْتَبِ الْبَرِيدِ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Mudhaf tidak boleh memakai tanwin maupun alif-lam.",
                    "Kata setelah Zharaf (seperti أَمَامَ) berkedudukan sebagai Mudhaf Ilaih (Majrur)."
                ),
                structureConclusion = "Kalimat dengan tiga konstruksi Idhafah yang terhubung rapi.",
                keyHarakatReasons = listOf(
                    "حَارِسُ (Dhammah tanpa tanwin) = Fa'il + Mudhaf.",
                    "بَوَّابَةِ (Kasrah tanpa tanwin) = Mudhaf Ilaih + Mudhaf."
                )
            ),
            tokenAnalyses = q5Analyses
        )

        // 6. JAR & MAJRUR (Beginner - Published)
        // سلم المسافر المسلم على إخوانه الكرام في المطار الدولي صباحا (10 kata)
        val q6Key = "q_jar_001"
        val q6Base = listOf("سلم", "المسافر", "المسلم", "على", "إخوانه", "الكرام", "في", "المطار", "الدولي", "صباحا")
        val q6Tokens = tokensFromSentence(q6Base, q6Key)
        val q6Analyses = listOf(
            buildTokenAnalysis(q6Key, 1, "سلم", "سَلَّمَ", "Memberi salam", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi", "Fi'il madhi mabni 'ala al-fath.", "Tidak ada", "Fi'il madhi mabni.", "Menuntut Fa'il 'الْمُسَافِرُ'.", "Mabni 'ala al-Fath", "Hukum asal fi'il madhi", "Tidak ada"),
            buildTokenAnalysis(q6Key, 2, "المسافر", "الْمُسَافِرُ", "Musafir / Pelancong", "Isim", "Fa'il & Man'ut", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Pelaku yang mengucapkan salam.", "Fi'il 'سَلَّمَ'.", "Wajib rafa'.", "Disifati oleh 'الْمُسْلِمُ'."),
            buildTokenAnalysis(q6Key, 3, "المسلم", "الْمُسْلِمُ", "Yang muslim", "Isim", "Na'at", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mensifati 'الْمُسَافِرُ' yang marfu'.", "Man'ut ('الْمُسَافِرُ').", "Mengikuti rafa'.", "Menjelaskan identitas musafir."),
            buildTokenAnalysis(q6Key, 4, "على", "عَلَى", "Kepada / atas", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Mahalli", "Huruf jar mabni sukun pada alif maqshurah.", "Amil jar.", "Huruf mabni.", "Menghubungkan 'سَلَّمَ' dengan 'إِخْوَانِهِ'.", "Mabni 'ala as-Sukun", "Huruf jar mabni", "Tidak ada"),
            buildTokenAnalysis(q6Key, 5, "إخوانه", "إِخْوَانِهِ", "Saudara-saudaranya", "Isim + Dhomir", "Isim Majrur & Mudhaf", "Mu'rab (Isim) + Mabni (Dhomir)", "Majrur", "Kasrah pada ن", "Tanda Asli", "Lafzhi (pada ن)", "Didahului huruf jar 'عَلَى', sehingga huruf akhir kata dasar ('ن') berharakat Kasrah; dhomir 'هِ' mabni di mahal jar.", "Harf Jar 'عَلَى'.", "Bukan Fathah atau Dhammah pada huruf ن.", "Disifati oleh 'الْكِرَامِ'."),
            buildTokenAnalysis(q6Key, 6, "الكرام", "الْكِرَامِ", "Yang mulia", "Isim (Jamak Taksir)", "Na'at", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mengikuti i'rab jar dari 'إِخْوَانِ'.", "Man'ut ('إِخْوَانِ').", "Mengikuti jar.", "Mensifati saudara-saudara."),
            buildTokenAnalysis(q6Key, 7, "في", "فِي", "Di", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Mahalli", "Huruf jar mabni sukun.", "Amil jar.", "Huruf mabni.", "Menunjukkan tempat.", "Mabni 'ala as-Sukun", "Harf jar", "Tidak ada"),
            buildTokenAnalysis(q6Key, 8, "المطار", "الْمَطَارِ", "Bandara", "Isim", "Isim Majrur & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Isim majrur setelah 'فِي'.", "Harf Jar 'فِي'.", "Wajib kasrah.", "Disifati oleh 'الدَّوْلِيِّ'."),
            buildTokenAnalysis(q6Key, 9, "الدولي", "الدَّوْلِيِّ", "Internasional", "Isim Nisbah", "Na'at", "Mu'rab", "Majrur", "Kasrah (dengan Tasydid pada ي)", "Tanda Asli", "Lafzhi", "Mensifati 'الْمَطَارِ' yang majrur.", "Man'ut ('الْمَطَارِ').", "Ya nisbah bertasydid dan berharakat kasrah.", "Menjelaskan status bandara."),
            buildTokenAnalysis(q6Key, 10, "صباحا", "صَبَاحًا", "Pada pagi hari", "Isim", "Zharaf Zaman", "Mu'rab", "Manshub", "Fathah (Tanwin Fathah)", "Tanda Asli", "Lafzhi", "Keterangan waktu.", "Fi'il 'سَلَّمَ'.", "Zharaf Zaman wajib manshub.", "Menjelaskan waktu salam diucapkan.")
        )
        val q6Pattern = AnswerPattern(
            patternId = "pat_jar_1",
            primary = true,
            label = "Pola Utama: Dua Susunan Jar Majrur (Mutta'alliq bi al-Fi'l)",
            voweledSentence = "سَلَّمَ الْمُسَافِرُ الْمُسْلِمُ عَلَى إِخْوَانِهِ الْكِرَامِ فِي الْمَطَارِ الدَّوْلِيِّ صَبَاحًا",
            translationNatural = "Pelancong muslim itu mengucapkan salam kepada saudara-saudaranya yang mulia di bandara internasional pada pagi hari.",
            translationGandul = "Wis ngucap salam sopo wong kang lelungan kang muslim marang pira-pira sedulure kang mulya ing dalem bandara internasional ing wektu esuk.",
            sentenceMeaningExplanation = "Mengilustrasikan dua pasang Jar–Majrur ('عَلَى إِخْوَانِهِ' dan 'فِي الْمَطَارِ') yang keduanya berkaitan dengan fi'il 'سَلَّمَ'.",
            structureDifferenceNote = "Dalam bahasa Arab, kata kerja 'سَلَّمَ' (memberi salam) muta'addi dengan perantara huruf jar 'عَلَى'.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah",
                mainElements = "Fi'il (سَلَّمَ) + Fa'il (الْمُسَافِرُ)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il: سَلَّمَ",
                    "├── Fa'il: الْمُسَافِرُ + Na'at: الْمُسْلِمُ",
                    "├── Jar Majrur 1: عَلَى إِخْوَانِهِ + Na'at: الْكِرَامِ",
                    "├── Jar Majrur 2: فِي الْمَطَارِ + Na'at: الدَّوْلِيِّ",
                    "└── Zharaf Zaman: صَبَاحًا"
                ),
                elementRelationships = "Kedua Jar Majrur berta'alluq (terikat makna) pada fi'il 'سَلَّمَ'.",
                meaningConstruction = "Menjelaskan siapa yang memberi salam, kepada siapa, di mana, dan kapan."
            ),
            wordRelations = listOf(
                WordRelation(4, 5, "عَلَى ↔ إِخْوَانِهِ", "Harf Jar dan Isim Majrur", "عَلَى mengkasrahkan akhir kata dasar إِخْوَانِ", "Amil jar", "Menentukan penerima salam."),
                WordRelation(7, 8, "فِي ↔ الْمَطَارِ", "Harf Jar dan Isim Majrur", "فِي mengkasrahkan الْمَطَارِ", "Amil jar", "Menentukan lokasi.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "سَلَّمَ", "Fi'il Madhi", "Wis uluk salam", "Kata kerja"),
                GandulEntry(2, "الْمُسَافِرُ", "Fa'il", "sopo musafir", "Pelaku"),
                GandulEntry(3, "الْمُسْلِمُ", "Na'at", "kang muslim", "Sifat"),
                GandulEntry(4, "عَلَى", "Harf Jar", "marang / atas", "Kepada"),
                GandulEntry(5, "إِخْوَانِهِ", "Majrur", "sedulure musafir", "Dikasrahkan ن"),
                GandulEntry(6, "الْكِرَامِ", "Na'at", "kang mulya", "Sifat"),
                GandulEntry(7, "فِي", "Harf Jar", "ing dalem", "Di"),
                GandulEntry(8, "الْمَطَارِ", "Majrur", "bandara", "Lokasi"),
                GandulEntry(9, "الدَّوْلِيِّ", "Na'at", "kang internasional", "Sifat bandara"),
                GandulEntry(10, "صَبَاحًا", "Zharaf Zaman", "ing wektu esuk", "Waktu")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Jar dan Majrur beserta Ta'alluq",
                    definition = "Setiap isim mu'rab yang didahului huruf jar (seperti مِنْ، إِلَى، عَنْ، عَلَى، فِي) wajib berhukum Majrur, dan susunannya berta'alluq kepada fi'il atau yang semakna.",
                    formula = "Harf Jar + Isim (Majrur)",
                    conditions = "Isim berada langsung setelah huruf jar.",
                    irabEffect = "Mengubah akhir isim menjadi Kasrah (pada isim mufrad dan jamak taksir).",
                    applicationInSentence = "Kata 'إِخْوَانِهِ' (huruf ن) dan 'الْمَطَارِ' keduanya berharakat Kasrah karena didahului 'عَلَى' dan 'فِي'.",
                    extraExamples = listOf("نَظَرْتُ إِلَى السَّمَاءِ", "جَلَسْتُ عَلَى الْكُرْسِيِّ")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Huruf jar selalu memajrurkan isim setelahnya.",
                    "Perhatikan posisi huruf akhir kata dasar saat bersambung dhomir (ن pada إِخْوَانِهِ)."
                ),
                structureConclusion = "Jumlah Fi'liyah dengan dua pasang Jar Majrur bertautan pada Fi'il.",
                keyHarakatReasons = listOf(
                    "إِخْوَانِهِ (Kasrah pada ن) = Majrur karena عَلَى.",
                    "الْمَطَارِ (Kasrah) = Majrur karena فِي."
                )
            ),
            tokenAnalyses = q6Analyses
        )

        // 7. JUMLAH ISMIYAH DENGAN KANA (Advanced - Published)
        // كان العالم المسلم مخلصا في نشر العلم النافع بين الناس (10 kata)
        val q7Key = "q_ism_001"
        val q7Base = listOf("كان", "العالم", "المسلم", "مخلصا", "في", "نشر", "العلم", "النافع", "بين", "الناس")
        val q7Tokens = tokensFromSentence(q7Base, q7Key)
        val q7Analyses = listOf(
            buildTokenAnalysis(q7Key, 1, "كان", "كَانَ", "Adalah (lampau)", "Fi'il Madhi Naqish", "Fi'il Nasikh (Kana)", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi", "Fi'il madhi naqish yang merafa'kan Isim-nya dan menashabkan Khabar-nya.", "Amil nawasikh bagi Mubtada' dan Khabar.", "Tidak membutuhkan Fa'il, melainkan Isim Kana dan Khabar Kana.", "Merafa'kan 'الْعَالِمُ' dan menashabkan 'مُخْلِصًا'.", "Mabni 'ala al-Fath", "Fi'il madhi", "Tidak ada"),
            buildTokenAnalysis(q7Key, 2, "العالم", "الْعَالِمُ", "Seorang ulama / ilmuwan", "Isim", "Isim Kana & Man'ut", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Menjadi Isim Kana (asalnya Mubtada') sehingga tetap Marfu'.", "Fi'il Naqish 'كَانَ'.", "Bukan Fathah karena Kana merafa'kan isimnya.", "Disifati oleh 'الْمُسْلِمُ'."),
            buildTokenAnalysis(q7Key, 3, "المسلم", "الْمُسْلِمُ", "Yang muslim", "Isim", "Na'at", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mengikuti i'rab rafa' Isim Kana ('الْعَالِمُ').", "Man'ut ('الْعَالِمُ').", "Selaras rafa'.", "Mensifati ulama tersebut."),
            buildTokenAnalysis(q7Key, 4, "مخلصا", "مُخْلِصًا", "Ikhlas / tulus", "Isim", "Khabar Kana", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Menjadi Khabar Kana yang wajib berhukum Manshub.", "Fi'il Naqish 'كَانَ'.", "Bukan Dhammah ('مُخْلِصٌ') karena masuknya 'كَانَ' mengubah khabar dari rafa' menjadi nashab!", "Menerangkan keadaan ulama yang tulus."),
            buildTokenAnalysis(q7Key, 5, "في", "فِي", "Dalam", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Mahalli", "Harf jar mabni sukun.", "Amil jar.", "Huruf mabni.", "Ta'alluq pada 'مُخْلِصًا'.", "Mabni 'ala as-Sukun", "Harf", "Tidak ada"),
            buildTokenAnalysis(q7Key, 6, "نشر", "نَشْرِ", "Menyebarkan", "Isim (Mashdar)", "Isim Majrur & Mudhaf", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Majrur karena 'فِي' dan menjadi Mudhaf.", "Harf Jar 'فِي'.", "Tidak bertanwin karena Mudhaf.", "Bersandar pada 'الْعِلْمِ'."),
            buildTokenAnalysis(q7Key, 7, "العلم", "الْعِلْمِ", "Ilmu", "Isim", "Mudhaf Ilaih & Man'ut", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih dari 'نَشْرِ'.", "Idhafah ('نَشْرِ').", "Wajib majrur.", "Disifati oleh 'النَّافِعِ'."),
            buildTokenAnalysis(q7Key, 8, "النافع", "النَّافِعِ", "Yang bermanfaat", "Isim", "Na'at", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mengikuti jar dari 'الْعِلْمِ'.", "Man'ut ('الْعِلْمِ').", "Mengikuti jar.", "Mensifati ilmu."),
            buildTokenAnalysis(q7Key, 9, "بين", "بَيْنَ", "Di tengah / di antara", "Isim", "Zharaf Makan & Mudhaf", "Mu'rab", "Manshub", "Fathah", "Tanda Asli", "Lafzhi", "Zharaf Makan manshub yang menunjukkan lokasi penyebaran ilmu.", "Mashdar 'نَشْرِ' / 'مُخْلِصًا'.", "Zharaf makan manshub.", "Disandarkan kepada 'النَّاسِ'."),
            buildTokenAnalysis(q7Key, 10, "الناس", "النَّاسِ", "Manusia", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih sesudah zharaf 'بَيْنَ'.", "Idhafah ('بَيْنَ').", "Wajib majrur.", "Melengkapi keterangan tempat.")
        )
        val q7Pattern = AnswerPattern(
            patternId = "pat_ism_1",
            primary = true,
            label = "Pola Utama: Jumlah Ismiyah dimasuki كَانَ (Tarfa'ul Isma wa Tanshiful Khabar)",
            voweledSentence = "كَانَ الْعَالِمُ الْمُسْلِمُ مُخْلِصًا فِي نَشْرِ الْعِلْمِ النَّافِعِ بَيْنَ النَّاسِ",
            translationNatural = "Ulama muslim itu senantiasa tulus dalam menyebarkan ilmu yang bermanfaat di tengah masyarakat.",
            translationGandul = "Ana sopo wong alim kang muslim iku wong kang ikhlas ing dalem nyebarake ilmu kang migunani ing antarane menungsa.",
            sentenceMeaningExplanation = "Menunjukkan pengaruh amil nasikh 'كَانَ' yang mempertahankan rafa' pada Isim-nya dan mengubah Khabar menjadi Manshub.",
            structureDifferenceNote = "Tanpa 'كَانَ', kata 'مُخْلِصٌ' dibaca Dhammah; setelah dimasuki 'كَانَ', berubah menjadi 'مُخْلِصًا' (Manshub).",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Ismiyah (dimasuki Fi'il Nasikh كَانَ)",
                mainElements = "كَانَ + Isim Kana (الْعَالِمُ) + Khabar Kana (مُخْلِصًا)",
                structureHierarchy = listOf(
                    "Jumlah Ismiyah (Nawasikh كَانَ)",
                    "├── Fi'il Naqish: كَانَ",
                    "├── Isim Kana: الْعَالِمُ (Marfu') + Na'at: الْمُسْلِمُ",
                    "└── Khabar Kana: مُخْلِصًا (Manshub)",
                    "    ├── Jar Majrur: فِي نَشْرِ الْعِلْمِ النَّافِعِ",
                    "    └── Zharaf Makan: بَيْنَ النَّاسِ"
                ),
                elementRelationships = "كَانَ mengubah struktur Mubtada' dan Khabar menjadi Isim Kana (Marfu') dan Khabar Kana (Manshub).",
                meaningConstruction = "Menegaskan sifat ketulusan seorang ulama dalam berdakwah ilmu."
            ),
            wordRelations = listOf(
                WordRelation(1, 2, "كَانَ ↔ الْعَالِمُ", "Amil Nasikh dan Isim-nya", "كَانَ merafa'kan الْعَالِمُ", "Kaidah Kana wa Akhawatuha", "Menetapkan subjek kalimat nominal."),
                WordRelation(1, 4, "كَانَ ↔ مُخْلِصًا", "Amil Nasikh dan Khabar-nya", "كَانَ menashabkan مُخْلِصًا", "Kaidah Kana wa Akhawatuha", "Menjadikan predikat berhukum nashab.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "كَانَ", "Fi'il Naqish", "Ana / tetap", "Amil nasikh"),
                GandulEntry(2, "الْعَالِمُ", "Isim Kana", "sopo wong alim", "Marfu'"),
                GandulEntry(3, "الْمُسْلِمُ", "Na'at", "kang muslim", "Sifat"),
                GandulEntry(4, "مُخْلِصًا", "Khabar Kana", "iku wong kang ikhlas", "Manshub"),
                GandulEntry(5, "فِي", "Harf Jar", "ing dalem", "Konteks"),
                GandulEntry(6, "نَشْرِ", "Majrur & Mudhaf", "nyebarake", "Mashdar"),
                GandulEntry(7, "الْعِلْمِ", "Mudhaf Ilaih", "ilmu", "Objek maknawi"),
                GandulEntry(8, "النَّافِعِ", "Na'at", "kang migunani", "Sifat ilmu"),
                GandulEntry(9, "بَيْنَ", "Zharaf Makan", "ing antarane", "Lokasi"),
                GandulEntry(10, "النَّاسِ", "Mudhaf Ilaih", "menungsa", "Mudhaf ilaih")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Kana wa Akhawatuha (كَانَ وَأَخَوَاتُهَا)",
                    definition = "Fi'il-fi'il naqish yang masuk pada Mubtada' dan Khabar, lalu merafa'kan Mubtada' sebagai Isim-nya dan menashabkan Khabar sebagai Khabar-nya.",
                    formula = "كَانَ + Isim Kana (Marfu') + Khabar Kana (Manshub)",
                    conditions = "Masuk pada susunan Jumlah Ismiyah.",
                    irabEffect = "Isim tetap Marfu' dan Khabar berubah menjadi Manshub.",
                    applicationInSentence = "'الْعَالِمُ' dibaca Dhammah sebagai Isim Kana, sedangkan 'مُخْلِصًا' dibaca Tanwin Fathah sebagai Khabar Kana.",
                    extraExamples = listOf("كَانَ اللهُ غَفُورًا رَحِيمًا", "أَصْبَحَ الْجَوُّ بَارِدًا")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Kana merafa'kan Isim-nya dan menashabkan Khabar-nya.",
                    "Penambahan huruf Alif di akhir 'مخلصا' adalah ketentuan penulisan Tanwin Fathah pada huruf selain Ta Marbuthah."
                ),
                structureConclusion = "Jumlah Ismiyah yang dimasuki Amil Nasikh كَانَ beserta perluasan Mashdar Mudhaf dan Zharaf.",
                keyHarakatReasons = listOf(
                    "الْعَالِمُ (Dhammah) = Isim Kana marfu'.",
                    "مُخْلِصًا (Tanwin Fathah) = Khabar Kana manshub."
                )
            ),
            tokenAnalyses = q7Analyses
        )

        // 8. HAL (Advanced - Published)
        // عاد الجندي الشجاع من ساحة المعركة منتصرا رافعا رأسه (10 kata)
        val q8Key = "q_hal_001"
        val q8Base = listOf("عاد", "الجندي", "الشجاع", "من", "ساحة", "المعركة", "منتصرا", "رافعا", "رأسه", "بفخر")
        val q8Tokens = tokensFromSentence(q8Base, q8Key)
        val q8Analyses = listOf(
            buildTokenAnalysis(q8Key, 1, "عاد", "عَادَ", "Telah kembali", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah", "Bina", "Lafzhi", "Fi'il madhi mabni 'ala al-fath.", "Tidak ada", "Fi'il madhi mabni.", "Membutuhkan Fa'il 'الْجُنْدِيُّ'.", "Mabni 'ala al-Fath", "Fi'il madhi", "Tidak ada"),
            buildTokenAnalysis(q8Key, 2, "الجندي", "الْجُنْدِيُّ", "Prajurit", "Isim", "Fa'il & Shahibul Hal", "Mu'rab", "Marfu'", "Dhammah (dengan Tasydid pada ي)", "Tanda Asli", "Lafzhi", "Pelaku yang kembali (Fa'il) sekaligus pemilik keadaan (Shahibul Hal) karena berbentuk ma'rifat.", "Fi'il 'عَادَ'.", "Wajib rafa'.", "Menjadi Shahibul Hal bagi 'مُنْتَصِرًا'."),
            buildTokenAnalysis(q8Key, 3, "الشجاع", "الشُّجَاعُ", "Yang pemberani", "Isim", "Na'at", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mensifati 'الْجُنْدِيُّ' yang marfu'.", "Man'ut ('الْجُنْدِيُّ').", "Wajib rafa'.", "Menjelaskan sifat prajurit."),
            buildTokenAnalysis(q8Key, 4, "من", "مِنْ", "Dari", "Harf", "Harf Jar", "Mabni", "Mabni", "Sukun", "Bina", "Lafzhi", "Harf jar mabni sukun.", "Amil jar.", "Huruf mabni.", "Memajrurkan 'سَاحَةِ'.", "Mabni 'ala as-Sukun", "Harf jar", "Tidak ada"),
            buildTokenAnalysis(q8Key, 5, "ساحة", "سَاحَةِ", "Medan", "Isim", "Isim Majrur & Mudhaf", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Didahului 'مِنْ' dan menjadi Mudhaf.", "Harf Jar 'مِنْ'.", "Tanpa tanwin karena mudhaf.", "Bersandar pada 'الْمَعْرَكَةِ'."),
            buildTokenAnalysis(q8Key, 6, "المعركة", "الْمَعْرَكَةِ", "Pertempuran", "Isim", "Mudhaf Ilaih", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mudhaf Ilaih dari 'سَاحَةِ'.", "Idhafah ('سَاحَةِ').", "Wajib majrur.", "Melengkapi arti medan pertempuran."),
            buildTokenAnalysis(q8Key, 7, "منتصرا", "مُنْتَصِرًا", "Dalam keadaan menang", "Isim Musytaq (Isim Fa'il)", "Hal Pertama", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Isim nakirah manshub yang menjelaskan keadaan Shahibul Hal ('الْجُنْدِيُّ') saat kembali.", "Fi'il 'عَادَ'.", "Bukan Dhammah karena Hal wajib Manshub dan harus nakirah.", "Menerangkan kondisi batin/keadaan sang prajurit."),
            buildTokenAnalysis(q8Key, 8, "رافعا", "رَافِعًا", "Sambil mengangkat", "Isim Fa'il yang beramal", "Hal Kedua", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Hal kedua (Hal muta'addid) bagi Shahibul Hal yang sama, berbentuk Isim Fa'il yang beramal menashabkan Maf'ul Bih setelahnya.", "Fi'il 'عَادَ'.", "Hal selalu manshub dan nakirah.", "Menjadi amil yang menashabkan 'رَأْسَهُ'."),
            buildTokenAnalysis(q8Key, 9, "رأسه", "رَأْسَهُ", "Kepalanya", "Isim + Dhomir", "Maf'ul Bih dari Isim Fa'il رَافِعًا", "Mu'rab (Isim) + Mabni (Dhomir)", "Manshub", "Fathah pada س", "Tanda Asli", "Lafzhi (pada س)", "Objek dari Isim Fa'il 'رَافِعًا' yang beramal seperti fi'il; huruf akhir kata dasar ('س') berharakat Fathah.", "Isim Fa'il 'رَافِعًا'.", "Bukan Kasrah karena menjadi objek (Maf'ul Bih), bukan Mudhaf Ilaih.", "Menjadi objek kata 'mengangkat'."),
            buildTokenAnalysis(q8Key, 10, "بفخر", "بِفَخْرٍ", "Dengan penuh kebanggaan", "Harf Jar + Isim", "Jar dan Majrur", "Mabni (بِ) + Mu'rab (فَخْرٍ)", "Majrur", "Tanwin Kasrah pada ر", "Tanda Asli", "Lafzhi", "Didahului huruf jar Ba' za'idah/mushahabah sehingga isim nakirah 'فَخْرٍ' dibaca Tanwin Kasrah.", "Harf Jar 'بِ'.", "Wajib kasrah karena didahului huruf jar 'بِ'.", "Menjelaskan cara mengangkat kepala.")
        )
        val q8Pattern = AnswerPattern(
            patternId = "pat_hal_1",
            primary = true,
            label = "Pola Utama: Hal Ganda & Amal Isim Fa'il",
            voweledSentence = "عَادَ الْجُنْدِيُّ الشُّجَاعُ مِنْ سَاحَةِ الْمَعْرَكَةِ مُنْتَصِرًا رَافِعًا رَأْسَهُ بِفَخْرٍ",
            translationNatural = "Prajurit yang pemberani itu kembali dari medan pertempuran dalam keadaan menang sambil mengangkat kepalanya dengan bangga.",
            translationGandul = "Wis bali sopo prajurit kang kendel saking papane peperangan hale wong kang menang hale wong kang ngangkat ing sirahe kelawan bangga.",
            sentenceMeaningExplanation = "Memperlihatkan bagaimana Hal (مُنْتَصِرًا dan رَافِعًا) menjelaskan keadaan pelaku yang ma'rifat, dan bagaimana Isim Fa'il 'رَافِعًا' menashabkan Maf'ul Bih 'رَأْسَهُ'.",
            structureDifferenceNote = "Dalam bahasa Indonesia, Hal biasanya diterjemahkan dengan 'dalam keadaan...' atau 'sambil...'.",
            tarkibAnalysis = TarkibAnalysis(
                sentenceType = "Jumlah Fi'liyah",
                mainElements = "Fi'il (عَادَ) + Fa'il/Shahibul Hal (الْجُنْدِيُّ) + Hal (مُنْتَصِرًا, رَافِعًا)",
                structureHierarchy = listOf(
                    "Jumlah Fi'liyah",
                    "├── Fi'il: عَادَ",
                    "├── Fa'il (Shahibul Hal): الْجُنْدِيُّ + Na'at: الشُّجَاعُ",
                    "├── Jar Majrur: مِنْ سَاحَةِ الْمَعْرَكَةِ",
                    "├── Hal 1: مُنْتَصِرًا (Manshub)",
                    "└── Hal 2: رَافِعًا (Manshub, Isim Fa'il beramal)",
                    "    ├── Maf'ul Bih: رَأْسَهُ (Fathah pada س)",
                    "    └── Jar Majrur: بِفَخْرٍ"
                ),
                elementRelationships = "Shahibul Hal (الْجُنْدِيُّ) bersifat ma'rifat, sedangkan Hal-nya (مُنْتَصِرًا & رَافِعًا) wajib nakirah dan manshub.",
                meaningConstruction = "Menjelaskan kepulangan prajurit disertai kondisi kemenangan dan wibawa."
            ),
            wordRelations = listOf(
                WordRelation(2, 7, "الْجُنْدِيُّ ↔ مُنْتَصِرًا", "Shahibul Hal dan Hal", "Shahibul Hal marfu', Hal wajib manshub", "Hal menerangkan kondisi subjek saat peristiwa", "Menjelaskan prajurit kembali dalam kondisi menang."),
                WordRelation(8, 9, "رَافِعًا ↔ رَأْسَهُ", "Isim Fa'il dan Maf'ul Bih-nya", "Isim Fa'il menashabkan رَأْسَ", "Isim fa'il yang bertanwin dan bersandar pada shahibul hal dapat beramal seperti fi'il", "Menjelaskan apa yang diangkat.")
            ),
            gandulEntries = listOf(
                GandulEntry(1, "عَادَ", "Fi'il Madhi", "Wis bali", "Kembali"),
                GandulEntry(2, "الْجُنْدِيُّ", "Fa'il / Shahibul Hal", "sopo prajurit", "Pelaku"),
                GandulEntry(3, "الشُّجَاعُ", "Na'at", "kang kendel", "Sifat"),
                GandulEntry(4, "مِنْ", "Harf Jar", "saking", "Dari"),
                GandulEntry(5, "سَاحَةِ", "Majrur & Mudhaf", "papane", "Medan"),
                GandulEntry(6, "الْمَعْرَكَةِ", "Mudhaf Ilaih", "peperangan", "Pertempuran"),
                GandulEntry(7, "مُنْتَصِرًا", "Hal 1", "hale menang", "Keadaan (ditandai 'hale')"),
                GandulEntry(8, "رَافِعًا", "Hal 2", "hale ngangkat", "Keadaan kedua"),
                GandulEntry(9, "رَأْسَهُ", "Maf'ul Bih", "ing sirahe", "Objek dari رَافِعًا"),
                GandulEntry(10, "بِفَخْرٍ", "Jar Majrur", "kelawan bangga", "Cara/keadaan")
            ),
            kaidahList = listOf(
                KaidahRule(
                    name = "Kaidah Hal (الْحَالُ)",
                    definition = "Hal adalah isim nakirah yang manshub untuk menjelaskan keadaan Shahibul Hal (yang harus ma'rifat) ketika suatu perbuatan terjadi.",
                    formula = "Shahibul Hal (Ma'rifat) + Hal (Nakirah Manshub)",
                    conditions = "Hal wajib nakirah dan manshub, sedangkan Shahibul Hal umumnya ma'rifat.",
                    irabEffect = "Hal selalu berhukum Manshub.",
                    applicationInSentence = "Kata 'مُنْتَصِرًا' dan 'رَافِعًا' keduanya berbentuk nakirah manshub menjelaskan keadaan 'الْجُنْدِيُّ'.",
                    extraExamples = listOf("جَاءَ زَيْدٌ رَاكِبًا", "أَقْبَلَ الطَّالِبُ مُبْتَسِمًا")
                )
            ),
            learningSummary = LearningSummary(
                mainRulesLearned = listOf(
                    "Hal wajib berbentuk Nakirah dan berhukum Manshub.",
                    "Isim Fa'il yang beramal dapat menashabkan Maf'ul Bih."
                ),
                structureConclusion = "Jumlah Fi'liyah dengan dua Hal berturut-turut dan amal Isim Fa'il.",
                keyHarakatReasons = listOf(
                    "مُنْتَصِرًا (Tanwin Fathah) = Hal manshub.",
                    "رَأْسَهُ (Fathah pada س) = Maf'ul Bih dari Isim Fa'il رَافِعًا."
                )
            ),
            tokenAnalyses = q8Analyses
        )

        // 9. SAMPLE DRAFT QUESTION (Admin Workflow verification - NOT visible to players!)
        val q9Key = "q_tam_draft"
        val q9Base = listOf("اشترى", "التاجر", "الأمين", "عشرين", "كيلوغراما", "تمرا", "جيدا", "من", "السوق", "المركزي")
        val q9Tokens = tokensFromSentence(q9Base, q9Key)
        val q9Analyses = listOf(
            buildTokenAnalysis(q9Key, 1, "اشترى", "اشْتَرَى", "Membeli", "Fi'il Madhi", "Fi'il Madhi", "Mabni", "Mabni", "Fathah Muqaddarah", "Bina", "Taqdiri", "Fi'il madhi mu'tal akhir alif mabni 'ala al-fath al-muqaddar.", "Tidak ada", "Alif tidak menerima harakat.", "Membutuhkan Fa'il.", "Mabni 'ala al-Fath al-Muqaddar", "Fi'il madhi", "Tidak ada"),
            buildTokenAnalysis(q9Key, 2, "التاجر", "التَّاجِرُ", "Pedagang", "Isim", "Fa'il", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Pelaku pembelian.", "Fi'il 'اشْتَرَى'.", "Fa'il wajib rafa'.", "Disifati 'الأَمِينُ'."),
            buildTokenAnalysis(q9Key, 3, "الأمين", "الأَمِينُ", "Yang jujur", "Isim", "Na'at", "Mu'rab", "Marfu'", "Dhammah", "Tanda Asli", "Lafzhi", "Mensifati 'التَّاجِرُ'.", "Man'ut.", "Mengikuti rafa'.", "Sifat pedagang."),
            buildTokenAnalysis(q9Key, 4, "عشرين", "عِشْرِينَ", "Dua puluh", "Isim ('Adad)", "Maf'ul Bih", "Mu'rab", "Manshub", "Ya (ي)", "Tanda Pengganti", "Lafzhi", "Mulhaq Jamak Mudzakkar Salim sehingga nashabnya dengan Ya.", "Fi'il 'اشْتَرَى'.", "Bukan Wawu.", "Menunjukkan bilangan yang ditamyizkan."),
            buildTokenAnalysis(q9Key, 5, "كيلوغراما", "كِيلُوغْرَامًا", "Kilogram", "Isim", "Tamyiz 'Adad", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Tamyiz setelah bilangan 11-99 wajib mufrad manshub.", "Bilangan 'عِشْرِينَ'.", "Wajib manshub.", "Satuan berat."),
            buildTokenAnalysis(q9Key, 6, "تمرا", "تَمْرًا", "Kurma", "Isim", "Tamyiz Zat / Badal", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Menjelaskan jenis benda yang ditimbang.", "كِيلُوغْرَامًا.", "Wajib manshub.", "Disifati 'جَيِّدًا'."),
            buildTokenAnalysis(q9Key, 7, "جيدا", "جَيِّدًا", "Yang bagus", "Isim", "Na'at", "Mu'rab", "Manshub", "Tanwin Fathah", "Tanda Asli", "Lafzhi", "Mensifati 'تَمْرًا'.", "Man'ut.", "Mengikuti nashab.", "Kualitas kurma."),
            buildTokenAnalysis(q9Key, 8, "من", "مِنَ", "Dari", "Harf", "Harf Jar", "Mabni", "Mabni", "Fathah (ilttiqa' sakinain)", "Bina", "Lafzhi", "Huruf jar 'مِنْ' difathahkan nun-nya karena bertemu alif-lam sukun.", "Amil jar.", "Mencegah bertemunya dua sukun.", "Memajrurkan 'السُّوقِ'.", "Mabni 'ala as-Sukun al-Muqaddar", "Iltiqa' as-sakinain", "Tidak ada"),
            buildTokenAnalysis(q9Key, 9, "السوق", "السُّوقِ", "Pasar", "Isim", "Isim Majrur", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Majrur setelah 'مِنَ'.", "Harf Jar 'مِنَ'.", "Wajib jar.", "Disifati 'الْمَرْكَزِيِّ'."),
            buildTokenAnalysis(q9Key, 10, "المركزي", "الْمَرْكَزِيِّ", "Pusat / Sentral", "Isim", "Na'at", "Mu'rab", "Majrur", "Kasrah", "Tanda Asli", "Lafzhi", "Mengikuti jar dari 'السُّوقِ'.", "Man'ut.", "Mengikuti jar.", "Menjelaskan lokasi pasar.")
        )
        val q9Pattern = AnswerPattern(
            patternId = "pat_tam_1",
            primary = true,
            label = "Pola Utama: Tamyiz 'Adad dan Tamyiz Timbangan",
            voweledSentence = "اشْتَرَى التَّاجِرُ الأَمِينُ عِشْرِينَ كِيلُوغْرَامًا تَمْرًا جَيِّدًا مِنَ السُّوقِ الْمَرْكَزِيِّ",
            translationNatural = "Pedagang yang jujur itu membeli dua puluh kilogram kurma yang bagus dari pasar pusat.",
            translationGandul = "Wis tuku sopo pedagang kang jujur ing rong puluh kilogram apane kurma kang bagus saking pasar pusat.",
            sentenceMeaningExplanation = "Latihan Tamyiz bilangan dan ukuran (Draft untuk direview).",
            structureDifferenceNote = "Tamyiz menjelaskan jenis benda dari angka atau satuan timbangan.",
            tarkibAnalysis = TarkibAnalysis("Jumlah Fi'liyah", "Fi'il + Fa'il + Maf'ul Bih + Tamyiz", listOf("Jumlah Fi'liyah dengan Tamyiz"), "Bilangan membutuhkan Tamyiz.", "Menjelaskan jenis barang."),
            wordRelations = listOf(WordRelation(4, 5, "عِشْرِينَ ↔ كِيلُوغْرَامًا", "'Adad dan Tamyiz", "Tamyiz wajib Manshub", "Bilangan 11-99", "Menjelaskan satuan.")),
            gandulEntries = q9Analyses.map { GandulEntry(it.position, it.voweledArabic, it.nahwuPosition, it.indonesianMeaning, it.relationToOtherWords) },
            kaidahList = listOf(KaidahRule("Tamyiz", "Isim nakirah manshub yang menghilangkan kesamaran.", "Mubham + Tamyiz (Manshub)", "Nakirah manshub", "Manshub", "كِيلُوغْرَامًا تَمْرًا", listOf("اشْتَرَيْتُ صَاعًا قَمْحًا"))),
            learningSummary = LearningSummary(listOf("Tamyiz setelah bilangan 11-99 wajib mufrad manshub."), "Struktur Tamyiz.", listOf("كِيلُوغْرَامًا manshub karena tamyiz.")),
            tokenAnalyses = q9Analyses
        )

        return listOf(
            SeedQuestionSpec("q_mub_001", "LAT-MUB-001", "cat_mub", "MUB", "Mubtada dan Khabar", "Ilmu adalah Cahaya", "Lengkapi harakat akhir (mahalul i'rab) setiap kata pada jumlah ismiyah berikut.", QuestionDifficulty.BEGINNER, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q1Base.joinToString(" "), q1Pattern.translationNatural, q1Pattern.translationGandul, q1Pattern.sentenceMeaningExplanation, q1Tokens, listOf(q1Pattern))),
            SeedQuestionSpec("q_fai_001", "LAT-FAI-001", "cat_fai", "FAI", "Fa'il dan Naibul Fa'il", "Pelajar Berangkat ke Sekolah", "Tentukan harakat akhir (mahalul i'rab) dengan memperhatikan kedudukan Fa'il, Jar Majrur, dan Zharaf.", QuestionDifficulty.BEGINNER, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q2Base.joinToString(" "), q2Pattern.translationNatural, q2Pattern.translationGandul, q2Pattern.sentenceMeaningExplanation, q2Tokens, listOf(q2Pattern))),
            SeedQuestionSpec("q_maf_001", "LAT-MAF-001", "cat_maf", "MAF", "Maf'ul Bih", "Membaca Kitab Nahwu", "Tentukan harakat akhir dengan cermat. Soal ini menerima dua pola Na'at yang sah secara Nahwu.", QuestionDifficulty.INTERMEDIATE, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q3Base.joinToString(" "), q3PatternPrimary.translationNatural, q3PatternPrimary.translationGandul, q3PatternPrimary.sentenceMeaningExplanation, q3Tokens, listOf(q3PatternPrimary, q3PatternAlt))),
            SeedQuestionSpec("q_naa_001", "LAT-NAA-001", "cat_naa", "NAA", "Na'at dan Man'ut", "Memuliakan Pelajar Tekun", "Perhatikan tanda i'rab asli dan tanda pengganti (Jamak Mudzakkar Salim) pada Na'at.", QuestionDifficulty.INTERMEDIATE, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q4Base.joinToString(" "), q4Pattern.translationNatural, q4Pattern.translationGandul, q4Pattern.sentenceMeaningExplanation, q4Tokens, listOf(q4Pattern))),
            SeedQuestionSpec("q_mud_001", "LAT-MUD-001", "cat_mud", "MUD", "Mudhaf dan Mudhaf Ilaih", "Penjaga Gerbang Masjid", "Latihan rantai Idhafah bertingkat pada posisi Fa'il, Maf'ul Bih, dan Zharaf.", QuestionDifficulty.INTERMEDIATE, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q5Base.joinToString(" "), q5Pattern.translationNatural, q5Pattern.translationGandul, q5Pattern.sentenceMeaningExplanation, q5Tokens, listOf(q5Pattern))),
            SeedQuestionSpec("q_jar_001", "LAT-JAR-001", "cat_jar", "JAR", "Jar dan Majrur", "Salam Musafir di Bandara", "Tentukan harakat akhir isim majrur termasuk kata yang bersambung dengan dhomir.", QuestionDifficulty.BEGINNER, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q6Base.joinToString(" "), q6Pattern.translationNatural, q6Pattern.translationGandul, q6Pattern.sentenceMeaningExplanation, q6Tokens, listOf(q6Pattern))),
            SeedQuestionSpec("q_ism_001", "LAT-ISM-001", "cat_ism", "ISM", "Jumlah Ismiyah", "Ulama yang Tulus", "Tentukan harakat Isim dan Khabar pada susunan Jumlah Ismiyah yang dimasuki 'كَانَ'.", QuestionDifficulty.ADVANCED, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q7Base.joinToString(" "), q7Pattern.translationNatural, q7Pattern.translationGandul, q7Pattern.sentenceMeaningExplanation, q7Tokens, listOf(q7Pattern))),
            SeedQuestionSpec("q_hal_001", "LAT-HAL-001", "cat_hal", "HAL", "Hal", "Kepulangan Prajurit Pemberani", "Latihan tingkat tinggi mengenai Hal ganda dan amal Isim Fa'il.", QuestionDifficulty.ADVANCED, QuestionStatus.PUBLISHED, QuestionVersionContent(1, q8Base.joinToString(" "), q8Pattern.translationNatural, q8Pattern.translationGandul, q8Pattern.sentenceMeaningExplanation, q8Tokens, listOf(q8Pattern))),
            SeedQuestionSpec("q_tam_001", "LAT-TAM-001", "cat_tam", "TAM", "Tamyiz", "Dua Puluh Kilogram Kurma", "Soal draft Tamyiz bilangan untuk uji coba pemeriksaan Reviewer.", QuestionDifficulty.INTERMEDIATE, QuestionStatus.REVIEW, QuestionVersionContent(1, q9Base.joinToString(" "), q9Pattern.translationNatural, q9Pattern.translationGandul, q9Pattern.sentenceMeaningExplanation, q9Tokens, listOf(q9Pattern)))
        )
    }
}
