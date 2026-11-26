package com.prince.whatstoolbox.ui
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.prince.whatstoolbox.domain.Category
import com.prince.whatstoolbox.domain.Tool

fun Category.icon(): ImageVector = when (this) {
    Category.USERNAME -> Icons.Default.Badge; Category.CHAT -> Icons.Default.Chat; Category.EXPORT -> Icons.Default.Analytics; Category.STATUS -> Icons.Default.Movie
    Category.MEDIA -> Icons.Default.Mic; Category.PRIVACY -> Icons.Default.Shield; Category.TEXT -> Icons.Default.TextFields; Category.PRODUCTIVITY -> Icons.Default.Event
    Category.BUSINESS -> Icons.Default.Storefront; Category.LATEST -> Icons.Default.AutoAwesome }
fun Tool.icon(): ImageVector = when (id) {
    "UsernameConnectCard" -> Icons.Default.Badge; "UsernameFinderBridge" -> Icons.Default.PersonSearch; "UsernameVCardExporter" -> Icons.Default.ContactPage
    "BrandHandleGenerator" -> Icons.Default.AlternateEmail; "UsernameVault" -> Icons.Default.Lock
    "DirectChat" -> Icons.Default.Chat; "BatchChatLauncher" -> Icons.Default.PlaylistPlay; "SmartNumberCleaner" -> Icons.Default.CleaningServices; "LinkAndQrBuilder" -> Icons.Default.QrCode
    "QrScannerDirectChat" -> Icons.Default.QrCodeScanner; "SelfNoteLauncher" -> Icons.Default.EditNote
    "ChatAnalyticsWrapped" -> Icons.Default.Analytics; "ChatToStyledPdf" -> Icons.Default.PictureAsPdf; "ChatOrderExtractorCsv" -> Icons.Default.TableChart
    "ChatLinkExtractor" -> Icons.Default.Link; "GroupParticipationAnalyzer" -> Icons.Default.Groups
    "StatusVideoSplitter" -> Icons.Default.ContentCut; "StatusVideoCompressor" -> Icons.Default.Compress; "StatusPosterDesigner" -> Icons.Default.Palette
    "AudioToVideoStatus" -> Icons.Default.MusicVideo; "OfflineCaptionLibrary" -> Icons.Default.FormatQuote
    "NoCropProfileFitter" -> Icons.Default.Crop; "OpusToMp3Converter" -> Icons.Default.Audiotrack; "VoiceNoteTranscriber" -> Icons.Default.Mic
    "OfflineStickerMaker" -> Icons.Default.EmojiEmotions; "HighResDocumentPrepper" -> Icons.Default.HighQuality
    "PhotoRedactionTool" -> Icons.Default.VisibilityOff; "ExifStripperAndWatermarker" -> Icons.Default.BrandingWatermark; "TrackingLinkCleaner" -> Icons.Default.LinkOff
    "MarkdownStyler" -> Icons.Default.FormatBold; "UnicodeFancyFonts" -> Icons.Default.Style; "QuickTextRepeater" -> Icons.Default.Repeat; "TextToEmojiArt" -> Icons.Default.GridView
    "InvisibleSpaceGenerator" -> Icons.Default.SpaceBar; "FlipReverseText" -> Icons.Default.SwapVert; "WordCharacterCounter" -> Icons.Default.FormatListNumbered
    "CannedResponseVault" -> Icons.Default.Reply; "FollowUpReminder" -> Icons.Default.Alarm; "MessageToCalendarEvent" -> Icons.Default.EventAvailable
    "GroupMeetingFormatter" -> Icons.Default.Groups; "DraftVault" -> Icons.Default.Drafts
    "InstantPdfInvoiceMaker" -> Icons.Default.ReceiptLong; "TextCatalogBuilder" -> Icons.Default.Inventory2; "StandardOrderForm" -> Icons.Default.ShoppingCart
    "BillSplitter" -> Icons.Default.Calculate; "MiniCrmNotes" -> Icons.Default.ContactPhone
    "ChatListOrganizer" -> Icons.Default.ListAlt; "ChatWallpaperStudio" -> Icons.Default.Wallpaper; "AmoledBlackWallpaper" -> Icons.Default.DarkMode
    "GroupEventFormatter" -> Icons.Default.Event; "AnnouncementDispatcher" -> Icons.Default.Campaign; "ChannelPollCreator" -> Icons.Default.Poll; "MetaAiPromptHelper" -> Icons.Default.SmartToy
    else -> category.icon() }
