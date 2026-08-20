package com.example.smsexpensetracker.parser

object CategoryClassifier {

    const val CATEGORY_FOOD = "Food & Dining"
    const val CATEGORY_GROCERIES = "Groceries"
    const val CATEGORY_SHOPPING = "Shopping"
    const val CATEGORY_BILLS = "Bills & Utilities"
    const val CATEGORY_TRANSPORT = "Transport"
    const val CATEGORY_ENTERTAINMENT = "Entertainment"
    const val CATEGORY_TRANSFERS = "Transfers"
    const val CATEGORY_HEALTH = "Health & Medical"
    const val CATEGORY_INVESTMENTS = "Investments"
    const val CATEGORY_OTHERS = "Others"

    val ALL_CATEGORIES = listOf(
        CATEGORY_FOOD,
        CATEGORY_GROCERIES,
        CATEGORY_SHOPPING,
        CATEGORY_BILLS,
        CATEGORY_TRANSPORT,
        CATEGORY_ENTERTAINMENT,
        CATEGORY_TRANSFERS,
        CATEGORY_HEALTH,
        CATEGORY_INVESTMENTS,
        CATEGORY_OTHERS
    )

    private val defaultKeywords: Map<String, List<String>> = mapOf(
        CATEGORY_FOOD to listOf(
            "swiggy", "zomato", "mcdonald", "starbucks", "kfc", "dominos", "burger king",
            "eatclub", "cafe", "restaurant", "dine", "biryani", "pizza", "subway", "haldiram",
            "chai", "coffee", "bar", "pub", "brewery", "bakery", "cake", "food", "tiffin", "mess",
            "eats", "dhabha", "kitchen", "sweets", "bistro", "pastry", "rolls"
        ),
        CATEGORY_GROCERIES to listOf(
            "blinkit", "zepto", "instamart", "bigbasket", "dmart", "nature basket", "supermarkt",
            "supermarket", "provision", "vegetable", "grocery", "milk", "country delight", "bbdaily",
            "spencer", "reliance fresh", "more retail", "fruits", "organic", "mandi", "mart",
            "dairy", "kirana", "meat", "licious"
        ),
        CATEGORY_SHOPPING to listOf(
            "amazon", "flipkart", "myntra", "zara", "nykaa", "ajio", "meesho", "tata cliq",
            "retail", "lifestyle", "decathlon", "ikea", "h&m", "hnm", "max fashion", "shoppers stop",
            "croma", "reliance digital", "mall", "cloth", "apparel", "jewelry", "lenskart",
            "optics", "store", "footwear", "bata", "trends", "westside", "pantaloons", "uniqlo"
        ),
        CATEGORY_BILLS to listOf(
            "airtel", "jio", "vi ", "vodafone", "idea", "bescom", "tneb", "msedcl", "electricity",
            "gas", "water", "broadband", "act fibernet", "tata play", "tatasky", "dth", "billdesk",
            "recharge", "bsnl", "indane", "bharat gas", "hp gas", "cesc", "cesu", "uppcl", "postpaid",
            "prepaid", "electricity bill", "utility", "bbps", "credit card bill", "cred club"
        ),
        CATEGORY_TRANSPORT to listOf(
            "uber", "ola", "rapido", "metro", "irctc", "makemytrip", "fastag", "toll", "petrol",
            "fuel", "shell", "hpcl", "bpcl", "iocl", "indian oil", "redbus", "goibibo", "indigo",
            "air india", "parking", "blusmart", "zoomcar", "autope", "railway", "train", "flight",
            "auto", "cab", "petroleum"
        ),
        CATEGORY_ENTERTAINMENT to listOf(
            "netflix", "spotify", "bookmyshow", "pvr", "inox", "prime video", "hotstar", "youtube",
            "steam", "cinema", "playstation", "multiplex", "movie", "theatre", "gaming", "gaana",
            "wynk", "apple music", "disney", "sony liv", "zee5", "audible", "kindle"
        ),
        CATEGORY_TRANSFERS to listOf(
            "transfer", "sent to", "p2p", "upi/", "payment to", "transferred to", "fund transfer",
            "self transfer", "imps/p2a", "neft to", "paid to friend", "received from"
        ),
        CATEGORY_HEALTH to listOf(
            "apollo", "pharmeasy", "1mg", "hospital", "pharmacy", "clinic", "medplus", "diagnostic",
            "practo", "chemist", "medicine", "dental", "max healthcare", "fortis", "dr lal",
            "pathlabs", "care", "wellness", "curefit", "cult.fit"
        ),
        CATEGORY_INVESTMENTS to listOf(
            "zerodha", "groww", "upstox", "kuvera", "mutual fund", "sip", "camsonline", "kfintech",
            "angel one", "indmoney", "coin", "etmoney", "shares", "stock", "bse", "nse", "mf"
        )
    )

    /**
     * Classifies a transaction based on payee/merchant name, raw SMS text, and any user-defined overrides.
     */
    fun classify(
        merchantOrPayee: String,
        rawSms: String,
        userCustomMappings: Map<String, String> = emptyMap()
    ): String {
        val merchantNormalized = merchantOrPayee.lowercase().trim()
        val textNormalized = "$merchantNormalized ${rawSms.lowercase()}"

        // 1. Check user custom mappings first (highest priority)
        for ((keyword, category) in userCustomMappings) {
            val kw = keyword.lowercase().trim()
            if (kw.isNotBlank() && (merchantNormalized.contains(kw) || textNormalized.contains(kw))) {
                return category
            }
        }

        // 2. Check merchant name directly against default keywords
        for ((category, keywords) in defaultKeywords) {
            for (keyword in keywords) {
                if (merchantNormalized.contains(keyword)) {
                    return category
                }
            }
        }

        // 3. Check full SMS text against default keywords
        for ((category, keywords) in defaultKeywords) {
            for (keyword in keywords) {
                if (textNormalized.contains(keyword)) {
                    return category
                }
            }
        }

        return CATEGORY_OTHERS
    }
}
