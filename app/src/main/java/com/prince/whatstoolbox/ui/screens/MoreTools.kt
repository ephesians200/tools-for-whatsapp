package com.prince.whatstoolbox.ui.screens
import androidx.compose.runtime.Composable
import com.prince.whatstoolbox.util.Content

/** Routes a tool id to its screen. Screens live in ChatTools / ImageTools / MediaTools / BusinessTools / Wrapped. */
@Composable fun MoreTool(id: String) { when (id) {
    "UsernameConnectCard" -> ConnectCard(); "UsernameVCardExporter" -> VCard(); "BatchChatLauncher" -> Batch(); "LinkAndQrBuilder" -> LinkQr()
    "QrScannerDirectChat" -> QrScan(); "SelfNoteLauncher" -> SelfNote()
    "ChatOrderExtractorCsv" -> OrderExtractor(); "ChatLinkExtractor" -> LinkExtractor(); "GroupParticipationAnalyzer" -> Participation()
    "OfflineCaptionLibrary" -> ListTool(Content.captions); "MetaAiPromptHelper" -> ListTool(Content.metaPrompts)
    "StatusPosterDesigner" -> Poster(); "AudioToVideoStatus" -> AudioVideo(); "StatusVideoCompressor" -> Compressor(); "NoCropProfileFitter" -> Fitter()
    "OpusToMp3Converter" -> Opus(); "VoiceNoteTranscriber" -> Transcribe(); "OfflineStickerMaker" -> Stickers(); "HighResDocumentPrepper" -> DocPrep()
    "PhotoRedactionTool" -> Redact(); "ExifStripperAndWatermarker" -> ExifWm(); "FollowUpReminder" -> Reminder(); "MessageToCalendarEvent" -> CalendarTool()
    "InstantPdfInvoiceMaker" -> Invoice(); "ChatWallpaperStudio" -> Wallpaper(false); "AmoledBlackWallpaper" -> Wallpaper(true)
    else -> FormTool(id) } }
