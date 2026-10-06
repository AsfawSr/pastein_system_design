package com.asfaw.pastebin.paste;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasteRepository extends JpaRepository<Paste, String> {
}
