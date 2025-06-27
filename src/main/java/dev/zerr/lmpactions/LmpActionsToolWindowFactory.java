package dev.zerr.lmpactions;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;
import com.intellij.ui.treeStructure.Tree;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LmpActionsToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Área de texto com melhor aparência
        JTextArea lmpInput = new JTextArea(12, 80);
        lmpInput.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        lmpInput.setLineWrap(true);
        lmpInput.setWrapStyleWord(true);
        lmpInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Tree para mostrar arquivos
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Files");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        Tree fileTree = new Tree(treeModel);
        fileTree.setRootVisible(true);
        fileTree.setShowsRootHandles(true);

        // Campo de diretório com melhor layout
        JTextField outputDir = new JTextField(project.getBasePath(), 30);
        outputDir.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        // Botões com melhor aparência
        JButton extractButton = new JButton("Extract LMP");
        extractButton.setPreferredSize(new Dimension(120, 30));
        extractButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        JButton copyPromptButton = new JButton("Copy Create Prompt");
        copyPromptButton.setPreferredSize(new Dimension(160, 30));
        copyPromptButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));

        // Label de status com melhor visibilidade
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Listener para atualizar a árvore quando o texto mudar
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

        // Lógica dos botões
        extractButton.addActionListener(e -> {
            String lmpText = lmpInput.getText();
            String dir = outputDir.getText();
            if (lmpText.isEmpty() || dir.isEmpty()) {
                statusLabel.setText("Please provide both LMP content and output directory.");
                statusLabel.setForeground(Color.RED);
                return;
            }
            try {
                LmpOperator operator = new LmpOperator();
                int count = operator.extract(lmpText, java.nio.file.Path.of(dir));
                statusLabel.setText("Extracted " + count + " files.");
                statusLabel.setForeground(new Color(0, 120, 0));
            } catch (Exception ex) {
                statusLabel.setText("Extraction failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
            }
        });

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
            statusLabel.setText("Prompt copied to clipboard.");
            statusLabel.setForeground(new Color(0, 120, 0));
        });

        // Panel superior com botão de prompt
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        topPanel.add(copyPromptButton);

        // Panel central com área de texto e árvore lado a lado
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        
        JLabel inputLabel = new JLabel("Paste LMP content:");
        inputLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        midPanel.add(inputLabel, BorderLayout.NORTH);

        // Split pane para dividir texto e árvore
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(new JScrollPane(lmpInput));
        
        JPanel treePanel = new JPanel(new BorderLayout());
        JLabel treeLabel = new JLabel("File Structure:");
        treeLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        treeLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
        treePanel.add(treeLabel, BorderLayout.NORTH);
        treePanel.add(new JScrollPane(fileTree), BorderLayout.CENTER);
        
        splitPane.setRightComponent(treePanel);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.6);
        
        midPanel.add(splitPane, BorderLayout.CENTER);

        // Panel de diretório com melhor espaçamento
        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        dirPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        JLabel dirLabel = new JLabel("Output Directory:");
        dirLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        dirLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        dirPanel.add(dirLabel);
        dirPanel.add(outputDir);
        dirPanel.add(Box.createHorizontalStrut(10));
        dirPanel.add(extractButton);

        // Panel de status
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        statusPanel.add(statusLabel);

        // Montagem final do layout
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(midPanel, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(dirPanel, BorderLayout.NORTH);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void updateFileTree(String lmpContent, DefaultTreeModel treeModel, DefaultMutableTreeNode rootNode) {
        SwingUtilities.invokeLater(() -> {
            rootNode.removeAllChildren();
            
            if (lmpContent == null || lmpContent.trim().isEmpty()) {
                treeModel.nodeStructureChanged(rootNode);
                return;
            }

            try {
                LmpOperator operator = new LmpOperator();
                List<String> files = operator.parseFileList(lmpContent);
                
                // Criar estrutura hierárquica
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
                // Se houver erro no parsing, limpar a árvore
                treeModel.nodeStructureChanged(rootNode);
            }
        });
    }
}
