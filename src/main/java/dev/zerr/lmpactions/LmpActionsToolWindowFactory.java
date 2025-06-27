package dev.zerr.lmpactions;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.*;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.*;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LmpActionsToolWindowFactory implements ToolWindowFactory, DumbAware {
    
    private Map<String, String> currentFileContents = new HashMap<>();
    private Project project;
    
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        this.project = project;
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(JBUI.Borders.empty(8));

        // Top panel with prompt button
        JPanel topPanel = createTopPanel();
        
        // Main content with vertical split
        JBSplitter mainSplitter = new JBSplitter(true, 0.7f);
        // make mainSplitter grow
        mainSplitter.setPreferredSize(new Dimension(800, 600));
        mainSplitter.setFirstComponent(createInputPanel());
        mainSplitter.setSecondComponent(createBottomPanel());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(mainSplitter, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(JBUI.Borders.emptyBottom(8));
        
        JButton copyPromptButton = new JButton("Copy Create Prompt");
        copyPromptButton.setPreferredSize(JBUI.size(160, 28));
        
        copyPromptButton.addActionListener(e -> {
            String createPrompt = """
            <rules>
                  Follow these rules **exactly and without deviation**:
                * Wrap the entire output in a **single fenced code block** using triple backticks (e.g., \\`\\`\\`txt). This outer block must contain the complete contents of the LMP file.
                * Inside the LMP file:
                  - Do **not** include any fenced code blocks (e.g., \\`\\`\\`), markdown, or any kind of code formatting.
                  - Output must be **raw plain text only**.
                  - Use this format for each file:
                    [FILE_START: path/to/file.ext] \s
                    ...file contents... \s
                    [FILE_END: path/to/file.ext]
                * The LMP file must contain **all files required** for a fully functional, runnable project.
                * For all documentation files (e.g., README, guides, manuals), use the **AsciiDoc (.adoc)** format. \s
                  - Do **not** use Markdown under any circumstances.
                  - Apply AsciiDoc syntax consistently throughout all documentation files.
                * DO NO EXPLAIN NOTHING, JUST SEND THE PROJECT, PLEASE!!!
                </rules>
                <instruction>
                  Create a\s
                </instruction>
            """;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(createPrompt), null);
        });
        
        topPanel.add(copyPromptButton);
        return topPanel;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.emptyBottom(4));
        
        JLabel inputLabel = new JLabel("Paste LMP content:");
        inputLabel.setBorder(JBUI.Borders.emptyBottom(4));
        
        JTextArea lmpInput = new JTextArea();
        lmpInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lmpInput.setLineWrap(true);
        lmpInput.setWrapStyleWord(true);
        lmpInput.setBorder(JBUI.Borders.empty(4));

        // Tree for file structure
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Files");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        Tree fileTree = new Tree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);
        
        // Add click listener for file tree
        fileTree.addTreeSelectionListener(e -> {
            TreePath path = e.getPath();
            if (path != null) {
                String filePath = getFilePathFromTreePath(path);
                if (filePath != null && currentFileContents.containsKey(filePath)) {
                    handleFileClick(filePath);
                }
            }
        });

        // Horizontal split for input and tree
        JBSplitter inputSplitter = new JBSplitter(true, 0.7f);
        
        JPanel textAreaPanel = new JPanel(new BorderLayout());
        textAreaPanel.add(new JBScrollPane(lmpInput), BorderLayout.CENTER);
        inputSplitter.setFirstComponent(textAreaPanel);
        
        JPanel treePanel = new JPanel(new BorderLayout());
        JLabel treeLabel = new JLabel("File Structure:");
        treeLabel.setBorder(JBUI.Borders.emptyBottom(4));
        treePanel.add(treeLabel, BorderLayout.NORTH);
        treePanel.add(new JBScrollPane(fileTree), BorderLayout.CENTER);
        inputSplitter.setSecondComponent(treePanel);

        // Document listener for real-time tree update
        lmpInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFileTree(lmpInput.getText(), treeModel, rootNode);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFileTree(lmpInput.getText(), treeModel, rootNode);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFileTree(lmpInput.getText(), treeModel, rootNode);
            }
        });

        inputPanel.add(inputLabel, BorderLayout.NORTH);
        inputPanel.add(inputSplitter, BorderLayout.CENTER);
        
        return inputPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // make bottomPanel grow
        bottomPanel.setPreferredSize(new Dimension(0, 200));
        
        // Directory and extract section
        JPanel extractPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // make extractPanel grow
        extractPanel.setPreferredSize(new Dimension(0, 60));
        extractPanel.setBorder(JBUI.Borders.emptyTop(4));
        
        JLabel dirLabel = new JLabel("Output Directory:");
        dirLabel.setBorder(JBUI.Borders.emptyRight(8));
        
        JTextField outputDir = new JTextField(project.getBasePath(), 25);
        
        JButton extractButton = new JButton("Extract LMP");
        extractButton.setPreferredSize(JBUI.size(100, 28));
        
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setBorder(JBUI.Borders.emptyLeft(8));
        
        extractButton.addActionListener(e -> {
            // Extract button logic would go here
            statusLabel.setText("Extraction functionality to be implemented");
        });
        
        extractPanel.add(dirLabel);
        extractPanel.add(outputDir);
        extractPanel.add(Box.createHorizontalStrut(8));
        extractPanel.add(extractButton);
        extractPanel.add(statusLabel);
        
        bottomPanel.add(extractPanel, BorderLayout.NORTH);
        
        return bottomPanel;
    }

    private void updateFileTree(String lmpContent, DefaultTreeModel treeModel, DefaultMutableTreeNode rootNode) {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            currentFileContents.clear();
            
            if (lmpContent == null || lmpContent.trim().isEmpty()) {
                treeModel.nodeStructureChanged(rootNode);
                return;
            }

            try {
                LmpOperator operator = new LmpOperator();
                List<String> files = operator.parseFileList(lmpContent);
                currentFileContents = operator.parseFileContents(lmpContent);
                
                // Create hierarchical structure
                Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();
                nodeMap.put("", rootNode);
                
                for (String filePath : files) {
                    String[] parts = filePath.split("/");
                    StringBuilder currentPath = new StringBuilder();
                    DefaultMutableTreeNode currentNode = rootNode;
                    
                    for (int i = 0; i < parts.length; i++) {
                        String part = parts[i];
                        if (i > 0) {
                            currentPath.append("/");
                        }
                        currentPath.append(part);
                        String pathKey = currentPath.toString();
                        
                        if (!nodeMap.containsKey(pathKey)) {
                            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(part);
                            currentNode.add(newNode);
                            nodeMap.put(pathKey, newNode);
                            currentNode = newNode;
                        } else {
                            currentNode = nodeMap.get(pathKey);
                        }
                    }
                }
                
                treeModel.nodeStructureChanged(rootNode);
                
            } catch (Exception e) {
                treeModel.nodeStructureChanged(rootNode);
            }
        });
    }

    private String getFilePathFromTreePath(TreePath treePath) {
        if (treePath.getPathCount() <= 1) {
            return null; // Root node
        }
        
        StringBuilder filePath = new StringBuilder();
        for (int i = 1; i < treePath.getPathCount(); i++) {
            if (i > 1) {
                filePath.append("/");
            }
            filePath.append(treePath.getPathComponent(i).toString());
        }
        
        String path = filePath.toString();
        return currentFileContents.containsKey(path) ? path : null;
    }

    private void handleFileClick(String filePath) {
        String lmpContent = currentFileContents.get(filePath);
        if (lmpContent == null) return;
        
        // Check if file exists in project
        Path projectPath = Paths.get(project.getBasePath());
        Path fullFilePath = projectPath.resolve(filePath);
        
        if (Files.exists(fullFilePath)) {
            // File exists - show diff
            showDiffWithExistingFile(filePath, lmpContent, fullFilePath);
        } else {
            // File doesn't exist - show preview
            showFilePreview(filePath, lmpContent);
        }
    }

    private void showDiffWithExistingFile(String filePath, String lmpContent, Path existingFilePath) {
        try {
            String existingContent = Files.readString(existingFilePath);
            
            DiffContentFactory contentFactory = DiffContentFactory.getInstance();
            DiffContent existingDiffContent = contentFactory.create(existingContent);
            DiffContent lmpDiffContent = contentFactory.create(lmpContent);
            
            SimpleDiffRequest diffRequest = new SimpleDiffRequest(
                "Compare: " + filePath,
                existingDiffContent,
                lmpDiffContent,
                "Existing File",
                "LMP Content"
            );
            
            DiffManager.getInstance().showDiff(project, diffRequest, DiffDialogHints.DEFAULT);
            
        } catch (IOException e) {
            showFilePreview(filePath, lmpContent);
        }
    }

    private void showFilePreview(String filePath, String content) {
        // Create a virtual file for preview
        String fileName = Paths.get(filePath).getFileName().toString();
        LightVirtualFile virtualFile = new LightVirtualFile(fileName, content);

        // Let IntelliJ automatically detect the file type
        FileType detectedFileType = FileTypeRegistry.getInstance().getFileTypeByFileName(fileName);
        if (detectedFileType != UnknownFileType.INSTANCE) {
            virtualFile.setFileType(detectedFileType);
        }

        // Open in editor
        FileEditorManager.getInstance(project).openFile(virtualFile, true);
    }
}
