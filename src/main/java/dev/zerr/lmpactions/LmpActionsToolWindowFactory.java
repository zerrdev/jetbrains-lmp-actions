package dev.zerr.lmpactions;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import com.intellij.ui.content.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class LmpActionsToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea lmpInput = new JTextArea(12, 80);
        JTextField outputDir = new JTextField(project.getBasePath(), 40);
        JButton extractButton = new JButton("Extract LMP");
        JLabel statusLabel = new JLabel(" ");

        extractButton.addActionListener(e -> {
            String lmpText = lmpInput.getText();
            String dir = outputDir.getText();
            if (lmpText.isEmpty() || dir.isEmpty()) {
                statusLabel.setText("Please provide both LMP content and output directory.");
                return;
            }
            try {
                LmpOperator operator = new LmpOperator();
                int count = operator.extract(lmpText, java.nio.file.Path.of(dir));
                statusLabel.setText("Extracted " + count + " files.");
            } catch (Exception ex) {
                statusLabel.setText("Extraction failed: " + ex.getMessage());
            }
        });

        JButton copyPromptButton = new JButton("Copy Create Prompt");
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
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Paste LMP content:"), BorderLayout.NORTH);
        topPanel.add(new JScrollPane(lmpInput), BorderLayout.CENTER);

        JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dirPanel.add(new JLabel("To:"));
        dirPanel.add(outputDir);
        dirPanel.add(extractButton);
        dirPanel.add(copyPromptButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(dirPanel, BorderLayout.CENTER);
        panel.add(statusLabel, BorderLayout.SOUTH);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
