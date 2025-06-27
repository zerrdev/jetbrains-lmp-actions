package dev.zerr.lmpactions;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class LmpOperator {

    public int extract(String lmpContent, Path destDir) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(lmpContent));
        String line;
        boolean isReadingFile = false;
        String currentFilePath = null;
        StringBuilder currentFileContent = new StringBuilder();
        int extractedFileCount = 0;

        while ((line = reader.readLine()) != null) {
            if (!isReadingFile && line.matches("^\\[FILE_START: .+]$")) {
                isReadingFile = true;
                currentFilePath = line.replaceAll("^\\[FILE_START: (.+)]$", "$1").trim();
                currentFileContent.setLength(0);
                continue;
            }
            if (isReadingFile && line.matches("^\\[FILE_END: .+]$")) {
                String foundFilePath = line.replaceAll("^\\[FILE_END: (.+)]$", "$1").trim();
                if (currentFilePath.equals(foundFilePath)) {
                    Path fullPath = destDir.resolve(currentFilePath);
                    Files.createDirectories(fullPath.getParent());
                    Files.writeString(fullPath, currentFileContent.toString());
                    isReadingFile = false;
                    extractedFileCount++;
                }
                continue;
            }
            if (isReadingFile) {
                currentFileContent.append(line).append('\n');
            }
        }
        if (isReadingFile) {
            throw new IOException("Unclosed file declaration: " + currentFilePath);
        }
        return extractedFileCount;
    }

    public List<String> parseFileList(String lmpContent) {
        List<String> files = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(lmpContent));
        String line;
        
        try {
            while ((line = reader.readLine()) != null) {
                if (line.matches("^\\[FILE_START: .+]$")) {
                    String filePath = line.replaceAll("^\\[FILE_START: (.+)]$", "$1").trim();
                    files.add(filePath);
                }
            }
        } catch (IOException e) {
            // Should not happen with StringReader
        }
        
        return files;
    }

    public Map<String, String> parseFileContents(String lmpContent) {
        Map<String, String> fileContents = new HashMap<>();
        BufferedReader reader = new BufferedReader(new StringReader(lmpContent));
        String line;
        boolean isReadingFile = false;
        String currentFilePath = null;
        StringBuilder currentFileContent = new StringBuilder();

        try {
            while ((line = reader.readLine()) != null) {
                if (!isReadingFile && line.matches("^\\[FILE_START: .+]$")) {
                    isReadingFile = true;
                    currentFilePath = line.replaceAll("^\\[FILE_START: (.+)]$", "$1").trim();
                    currentFileContent.setLength(0);
                    continue;
                }
                if (isReadingFile && line.matches("^\\[FILE_END: .+]$")) {
                    String foundFilePath = line.replaceAll("^\\[FILE_END: (.+)]$", "$1").trim();
                    if (currentFilePath.equals(foundFilePath)) {
                        fileContents.put(currentFilePath, currentFileContent.toString());
                        isReadingFile = false;
                    }
                    continue;
                }
                if (isReadingFile) {
                    currentFileContent.append(line).append('\n');
                }
            }
        } catch (IOException e) {
            // Should not happen with StringReader
        }
        
        return fileContents;
    }

    public String copyFolderAsLmp(Path folderPath, List<String> excludeExtensions, List<Pattern> excludePatterns, Path relativeTo) throws IOException {
        StringBuilder lmpContent = new StringBuilder();
        List<Path> files = getAllFiles(folderPath, excludeExtensions, excludePatterns, relativeTo);
        for (Path file : files) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            String relPath = relativeTo.relativize(file).toString().replace(File.separatorChar, '/');
            lmpContent.append("[FILE_START: ").append(relPath).append("]\n");
            lmpContent.append(content);
            if (!content.endsWith("\n")) {
                lmpContent.append('\n');
            }
            lmpContent.append("[FILE_END: ").append(relPath).append("]\n\n");
        }
        return lmpContent.toString();
    }

    public String copyFileAsLmp(Path filePath, Path relativeTo) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        String relPath = relativeTo != null ? relativeTo.relativize(filePath).toString().replace(File.separatorChar, '/') : filePath.getFileName().toString();
        StringBuilder lmpContent = new StringBuilder();
        lmpContent.append("[FILE_START: ").append(relPath).append("]\n");
        lmpContent.append(content);
        if (!content.endsWith("\n")) {
            lmpContent.append('\n');
        }
        lmpContent.append("[FILE_END: ").append(relPath).append("]\n\n");
        return lmpContent.toString();
    }

    private List<Path> getAllFiles(Path dir, List<String> excludeExtensions, List<Pattern> excludePatterns, Path relativeTo) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walk(dir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String ext = getExtension(path.getFileName().toString()).toLowerCase();
                    String relPath = relativeTo.relativize(path).toString().replace(File.separatorChar, '/');
                    if (excludeExtensions.contains(ext)) return;
                    for (Pattern pattern : excludePatterns) {
                        if (pattern.matcher(relPath).find()) return;
                    }
                    result.add(path);
                });
        return result;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot) : "";
    }
}
