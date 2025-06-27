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

// Lógica dos botões (mantida igual)
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

// Panel central com área de texto
        JPanel midPanel = new JPanel(new BorderLayout());
        midPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        JLabel inputLabel = new JLabel("Paste LMP content:");
        inputLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        inputLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        midPanel.add(inputLabel, BorderLayout.NORTH);
        midPanel.add(new JScrollPane(lmpInput), BorderLayout.CENTER);

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
}
