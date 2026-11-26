package com.prince.whatstoolbox.domain
enum class VaultKind(val key: String, val label: String, val titleLabel: String, val bodyLabel: String, val extraLabel: String?) {
    SNIPPET("SNIPPET", "Replies", "Title", "Reply text", null),
    DRAFT("DRAFT", "Drafts", "Draft title", "Draft message", null),
    CRM("CRM", "Notes", "Customer name", "Private note", "Phone or @handle"),
    HANDLE("HANDLE", "Handles", "Handle (@name)", "Custom greeting", "Key / PIN")
}
