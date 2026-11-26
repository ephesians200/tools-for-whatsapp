package com.prince.whatstoolbox.domain

enum class Category(val label: String, val emoji: String) {
    USERNAME("Username & Identity", "🪪"), CHAT("Direct Chat", "💬"), EXPORT("Chat Power Tools", "📊"),
    STATUS("Status & Video", "🎬"), MEDIA("Media & Voice", "🎙️"), PRIVACY("Privacy & Links", "🛡️"),
    TEXT("Text & Creative", "✍️"), PRODUCTIVITY("Productivity", "🗂️"), BUSINESS("Seller Tools", "🧾"), LATEST("Latest Features", "✨")
}
/** [title] is the human display name, [blurb] a one-line description, [keywords] extra search terms (e.g. "pdf", "invoice"). */
data class Tool(val id: String, val title: String, val category: Category, val blurb: String = "", val keywords: String = "", val premium: Boolean = false)

/** Single source of truth for all tools. */
object ToolRegistry {
    /**
     * Tools behind the one-time purchase. Picked because they save the most time / money (video, voice, PDFs, invoices, bulk messaging, private storage).
     * Edit this one set to move a tool between free and Pro; everything else (lock badge, paywall, Vault tab, shortcuts) follows automatically.
     */
    val PREMIUM_IDS = setOf(
        "UsernameConnectCard", "UsernameVault", "BatchChatLauncher", "ChatToStyledPdf", "ChatOrderExtractorCsv",
        "StatusVideoSplitter", "StatusVideoCompressor", "AudioToVideoStatus", "VoiceNoteTranscriber", "OpusToMp3Converter", "OfflineStickerMaker",
        "PhotoRedactionTool", "ExifStripperAndWatermarker", "CannedResponseVault", "FollowUpReminder", "InstantPdfInvoiceMaker", "TextCatalogBuilder", "MiniCrmNotes")
    /** Categories whose grid ends with a video-ad tile (Direct Chat and Latest Features have none). */
    val AD_CATEGORIES = setOf(Category.USERNAME, Category.EXPORT, Category.STATUS, Category.MEDIA, Category.PRIVACY, Category.TEXT, Category.PRODUCTIVITY, Category.BUSINESS)
    private val meta = mapOf(
        "UsernameConnectCard" to "Username Connect Card|Share a card with your @username and a QR|qr card share profile",
        "UsernameFinderBridge" to "Username Finder|Copy a handle and jump into WhatsApp search|search handle find",
        "UsernameVCardExporter" to "Username vCard Export|Save a handle as a contact card (.vcf)|contact vcf export",
        "BrandHandleGenerator" to "Brand Handle Ideas|Generate @username ideas for your name or brand|username name generator business",
        "UsernameVault" to "Handle Vault|Keep handles, greetings and keys encrypted|username pin key private",
        "DirectChat" to "Direct Chat|Message any number without saving it|wa.me number open link contact",
        "BatchChatLauncher" to "Batch Chat Launcher|Work through a list of numbers one by one|bulk multiple numbers queue",
        "SmartNumberCleaner" to "Number Cleaner|Fix messy phone numbers into WhatsApp format|format country code clean",
        "LinkAndQrBuilder" to "Link & QR Builder|Create a wa.me link and QR code|qr code link",
        "QrScannerDirectChat" to "QR Scanner|Scan a QR code and open the chat|scan camera",
        "SelfNoteLauncher" to "Message Yourself|Open your own chat for quick notes|self note saved messages",
        "ChatAnalyticsWrapped" to "Chat Wrapped|Stats and story cards from an exported chat|analytics statistics export emoji",
        "ChatToStyledPdf" to "Chat to PDF|Turn an exported chat into a printable PDF|pdf export print",
        "ChatOrderExtractorCsv" to "Order Extractor (CSV)|Pull items and prices out of a chat|orders csv sales excel",
        "ChatLinkExtractor" to "Link & Contact Extractor|List links, emails and phones from a chat|links emails phones",
        "GroupParticipationAnalyzer" to "Group Participation|See who talks and who stays silent|group members activity",
        "StatusVideoSplitter" to "Status Video Splitter|Cut a long video into Status-sized clips|split clips cut seconds",
        "StatusVideoCompressor" to "Status Video Compressor|Shrink a video to 720p for Status|compress reduce size",
        "StatusPosterDesigner" to "Status Poster Designer|Make a text poster for Status|text status design image",
        "AudioToVideoStatus" to "Audio to Video|Turn an image and audio into a video Status|music mp4",
        "OfflineCaptionLibrary" to "Caption Library|Ready-made captions and greetings|quotes greetings good morning",
        "NoCropProfileFitter" to "No-Crop Profile Photo|Fit any photo into a square without cropping|avatar dp profile picture",
        "OpusToMp3Converter" to "Voice Note to MP3|Convert a voice note (.opus) to MP3|audio opus convert",
        "VoiceNoteTranscriber" to "Voice Note Transcriber|Turn a voice note into text|speech to text transcribe audio",
        "OfflineStickerMaker" to "Sticker Maker|Build a sticker pack from your photos|stickers pack",
        "HighResDocumentPrepper" to "Send Photos Uncompressed|Share original-quality photos as documents|hd quality original photos",
        "PhotoRedactionTool" to "Photo Redaction|Hide faces, numbers and other details|blur censor black out hide",
        "ExifStripperAndWatermarker" to "Strip Metadata & Watermark|Remove GPS/EXIF data and stamp a watermark|exif gps kyc privacy",
        "TrackingLinkCleaner" to "Link Cleaner|Strip tracking junk from links|utm tracking url clean redirect",
        "MarkdownStyler" to "Text Formatter|Bold, italic, strike-through and more|markdown bold italic format",
        "UnicodeFancyFonts" to "Fancy Fonts|50+ stylish text styles|fonts stylish unicode",
        "QuickTextRepeater" to "Text Repeater|Repeat a text up to 5,000 times|spam repeat",
        "TextToEmojiArt" to "Emoji Text Art|Spell words with emoji blocks|emoji art banner",
        "InvisibleSpaceGenerator" to "Invisible Text|Generate blank messages|blank empty space",
        "FlipReverseText" to "Flip & Reverse Text|Upside-down or reversed text|mirror upside down",
        "WordCharacterCounter" to "Word & Character Counter|Count words, characters and reading time|count length",
        "CannedResponseVault" to "Saved Replies|Encrypted quick replies|canned snippets templates",
        "FollowUpReminder" to "Follow-up Reminder|Get a nudge to message a customer|remind notification",
        "MessageToCalendarEvent" to "Message to Calendar|Turn a message into a calendar event|event date schedule",
        "GroupMeetingFormatter" to "Meeting Announcement|Format a meeting with agenda and RSVP|agenda rsvp",
        "DraftVault" to "Draft Vault|Keep message drafts encrypted|notes drafts",
        "InstantPdfInvoiceMaker" to "Invoice Maker|Create a PDF invoice in seconds|pdf invoice receipt bill",
        "TextCatalogBuilder" to "Catalog Builder|Format a product list with prices|products prices shop",
        "StandardOrderForm" to "Order Form|A copy-paste order form for customers|orders template",
        "BillSplitter" to "Bill Splitter|Split a bill with tip and custom shares|split tip money",
        "MiniCrmNotes" to "Customer Notes|Private notes for each customer|crm contacts",
        "ChatListOrganizer" to "Chat Lists Planner|Plan WhatsApp lists for your sales stages|labels organize leads",
        "ChatWallpaperStudio" to "Wallpaper Studio|Make a custom chat wallpaper|background theme",
        "AmoledBlackWallpaper" to "AMOLED Wallpaper|Pure-black wallpaper with a soft glow|dark black",
        "GroupEventFormatter" to "Event Announcement|Format a group event post|event invite",
        "AnnouncementDispatcher" to "Announcement Writer|A clear announcement with a mention|announce broadcast",
        "ChannelPollCreator" to "Poll Builder|Check a poll before you post it|poll vote channel",
        "MetaAiPromptHelper" to "Meta AI Prompts|Handy prompts to try|ai prompts assistant"
    )
    private val raw = mapOf(
        Category.USERNAME to "UsernameConnectCard UsernameFinderBridge UsernameVCardExporter BrandHandleGenerator UsernameVault",
        Category.CHAT to "DirectChat BatchChatLauncher SmartNumberCleaner LinkAndQrBuilder QrScannerDirectChat SelfNoteLauncher",
        Category.EXPORT to "ChatAnalyticsWrapped ChatToStyledPdf ChatOrderExtractorCsv ChatLinkExtractor GroupParticipationAnalyzer",
        Category.STATUS to "StatusVideoSplitter StatusVideoCompressor StatusPosterDesigner AudioToVideoStatus OfflineCaptionLibrary",
        Category.MEDIA to "NoCropProfileFitter OpusToMp3Converter VoiceNoteTranscriber OfflineStickerMaker HighResDocumentPrepper",
        Category.PRIVACY to "PhotoRedactionTool ExifStripperAndWatermarker TrackingLinkCleaner",
        Category.TEXT to "MarkdownStyler UnicodeFancyFonts QuickTextRepeater TextToEmojiArt InvisibleSpaceGenerator FlipReverseText WordCharacterCounter",
        Category.PRODUCTIVITY to "CannedResponseVault FollowUpReminder MessageToCalendarEvent GroupMeetingFormatter DraftVault",
        Category.BUSINESS to "InstantPdfInvoiceMaker TextCatalogBuilder StandardOrderForm BillSplitter MiniCrmNotes",
        Category.LATEST to "ChatListOrganizer ChatWallpaperStudio AmoledBlackWallpaper GroupEventFormatter AnnouncementDispatcher ChannelPollCreator MetaAiPromptHelper")
    val all: List<Tool> = raw.flatMap { (c, ids) -> ids.split(" ").map { id ->
        val p = meta[id]?.split("|").orEmpty()
        Tool(id, p.getOrNull(0) ?: id.replace(Regex("(?<=[a-z])(?=[A-Z])"), " "), c, p.getOrNull(1).orEmpty(), p.getOrNull(2).orEmpty(), id in PREMIUM_IDS) } }
    private val index = all.associateBy { it.id }
    fun byId(id: String) = index.getValue(id)
    fun byIdOrNull(id: String) = index[id]
    fun isLocked(id: String, premium: Boolean) =
        !premium && id in PREMIUM_IDS &&
                !(com.prince.whatstoolbox.BuildConfig.DEBUG && id == "StatusVideoSplitter")   // TEMP: demo-video unlock
}
