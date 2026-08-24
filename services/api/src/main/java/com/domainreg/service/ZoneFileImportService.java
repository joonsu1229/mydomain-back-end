package com.domainreg.service;

import com.domainreg.core.entity.DnsRecord;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ZoneFileImportService {

    private static final java.util.Set<String> VALID_TYPES =
        java.util.Set.of("A", "AAAA", "CNAME", "MX", "TXT", "NS", "SRV");

    public List<ImportResult> parse(String zoneText) {
        List<ImportResult> results = new ArrayList<>();
        String origin = null;
        int defaultTtl = 3600;

        for (String line : zoneText.split("\n")) {
            line = line.trim();

            // skip comments and blank lines
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                continue;
            }

            // $ORIGIN directive
            if (line.toUpperCase().startsWith("$ORIGIN")) {
                origin = line.substring(7).trim().replaceAll("\\.$", "");
                continue;
            }

            // $TTL directive
            if (line.toUpperCase().startsWith("$TTL")) {
                try {
                    defaultTtl = Integer.parseInt(line.substring(4).trim());
                } catch (NumberFormatException ignored) {}
                continue;
            }

            // Parse record line
            ParseResult parsed = parseRecordLine(line, origin, defaultTtl);
            if (parsed != null) {
                results.add(new ImportResult(parsed.type, parsed.name, parsed.content,
                    parsed.ttl, parsed.priority, parsed.raw, null));
            } else {
                results.add(new ImportResult(null, null, null, 0, null, line,
                    "Unrecognized format"));
            }
        }

        return results;
    }

    private ParseResult parseRecordLine(String line, String origin, int defaultTtl) {
        // RFC 1035 format: [NAME] [TTL] [CLASS] TYPE RDATA
        // CLASS is usually IN (optional), we skip it
        String[] parts = line.split("\\s+");
        if (parts.length < 2) return null;

        int idx = 0;
        String name = null;
        String type = null;
        int ttl = defaultTtl;
        String content = null;
        Integer priority = null;

        // Detect if first token is a name (not a number/class/type)
        String first = parts[0];
        if (!first.equals("IN") && !VALID_TYPES.contains(first.toUpperCase())
            && !first.matches("\\d+")) {
            name = first;
            idx++;
        }

        // TTL (optional, numeric)
        if (idx < parts.length && parts[idx].matches("\\d+")) {
            // Could be TTL or part of content — check if followed by known type or IN
            if (idx + 1 < parts.length) {
                String next = parts[idx + 1];
                if (next.equals("IN") || VALID_TYPES.contains(next.toUpperCase())) {
                    ttl = Integer.parseInt(parts[idx]);
                    idx++;
                }
            }
        }

        // CLASS (optional, usually IN)
        if (idx < parts.length && parts[idx].equals("IN")) {
            idx++;
        }

        // TYPE
        if (idx >= parts.length) return null;
        String rawType = parts[idx].toUpperCase().replaceAll("\\.$", "");
        if (!VALID_TYPES.contains(rawType)) return null;
        type = rawType;
        idx++;

        // Content (may contain priority for MX/SRV)
        // MX: priority hostname, SRV: priority weight port target
        if (idx >= parts.length) return null;

        if (("MX".equals(type) || "SRV".equals(type)) && parts[idx].matches("\\d+")) {
            priority = Integer.parseInt(parts[idx]);
            idx++;
            // remaining parts are the content
            StringBuilder sb = new StringBuilder();
            for (int i = idx; i < parts.length; i++) {
                if (i > idx) sb.append(" ");
                sb.append(parts[i]);
            }
            content = sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = idx; i < parts.length; i++) {
                if (i > idx) sb.append(" ");
                sb.append(parts[i]);
            }
            content = sb.toString().replaceAll("\\.$", "");
        }

        // If name is null and origin is set, use @ (root)
        if (name == null && origin != null) {
            name = "@";
        } else if (name == null) {
            name = "@";
        }

        return new ParseResult(type, name, content, ttl, priority, line);
    }

    private record ParseResult(String type, String name, String content,
                                int ttl, Integer priority, String raw) {}

    public record ImportResult(
        String type, String name, String content,
        int ttl, Integer priority, String rawLine, String error
    ) {
        public boolean isSuccess() { return error == null && type != null; }
    }
}
