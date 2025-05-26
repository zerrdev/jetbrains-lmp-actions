package dev.zerr.lmpactions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class LmpCopyWithInstructionAction extends AnAction {

    private static final List<String> DEFAULT_EXCLUDE_EXTENSIONS = Arrays.asList(
            ".exe", ".dll", ".so", ".bin", ".class", ".jar", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".mp3", ".mp4", ".wav", ".zip", ".tar", ".gz", ".7z", ".rar", ".iso"
    );
    private static final List<Pattern> DEFAULT_EXCLUDE_PATTERNS = Arrays.asList(
            Pattern.compile("^node_modules/"), Pattern.compile("^\\.git/"), Pattern.compile("^\\.idea/"), Pattern.compile("^\\.vscode/"), Pattern.compile("^__pycache__/")
    );

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        if (files == null || files.length == 0) {
            return;
        }

        // Show instruction dialog
        InstructionDialog dialog = new InstructionDialog(project);
        if (!dialog.showAndGet()) {
            return; // User cancelled
        }

        String instruction = dialog.getInstruction();
        if (instruction == null || instruction.trim().isEmpty()) {
            Messages.showWarningDialog(project, "No instruction provided.", "LMP Copy with Instruction");
            return;
        }

        LmpOperator operator = new LmpOperator();
        StringBuilder lmpContent = new StringBuilder();
        Path projectRoot = Path.of(project.getBasePath());

        try {
            for (VirtualFile vf : files) {
                Path path = Path.of(vf.getPath());
                if (vf.isDirectory()) {
                    lmpContent.append(operator.copyFolderAsLmp(path, DEFAULT_EXCLUDE_EXTENSIONS, DEFAULT_EXCLUDE_PATTERNS, projectRoot));
                } else if (vf.isValid() && !vf.isDirectory()) {
                    lmpContent.append(operator.copyFileAsLmp(path, projectRoot));
                }
            }

            // Add instruction at the end
            String finalContent = formatLmpWithInstructions(lmpContent.toString(), instruction);
            
            CopyPasteManager.getInstance().setContents(new StringSelection(finalContent));
            Messages.showInfoMessage(project, "LMP content with instructions copied to clipboard.", "LMP Copy with Instruction");
        } catch (Exception ex) {
            Messages.showErrorDialog(project, "Error copying files: " + ex.getMessage(), "LMP Copy with Instruction");
        }
    }

    private String formatLmpWithInstructions(String lmpContent, String instruction) {
        String baseInstructions = """
                Follow these instructions **exactly and without deviation**:
                * Wrap the entire output in a **single fenced code block** using triple backticks (e.g., \\`\\`\\`txt). This outer block must contain the complete contents of the LMP file.
                * Inside the LMP file:
                  - Do **not** include any fenced code blocks (e.g., \\`\\`\\`), markdown, or any kind of code formatting.
                  - Output must be **raw plain text only**.
                  - Use this format for each file:
                    [FILE_START: path/to/file.ext] \s
                    ...file contents... \s
                    [FILE_END: path/to/file.ext]
                * The LMP file must contain **modified files** only.
                * Always return the **complete modified file(s)** — do not include placeholders like "rest of file" or "..." and do not omit unchanged parts.
                * Preserve the **exact directory and file structure**.
                * Do **not** do unrequested modifications.
                * For all documentation files (e.g., README, guides, manuals), use the **AsciiDoc (.adoc)** format. \s
                  - Do **not** use Markdown under any circumstances.
                  - Apply AsciiDoc syntax consistently throughout all documentation files.
                * DO NO EXPLAIN NOTHING, JUST SEND THE PROJECT, PLEASE!!!
                """;

        return """
                <rules>
                  %s
                </rules>
                <files>
                  ```
                  %s
                  ```
                </files>
                <instruction>
                  %s
                </instruction>
                """.formatted(baseInstructions, lmpContent, instruction);
    }

    private static class InstructionDialog extends DialogWrapper {
        private JTextArea instructionArea;

        protected InstructionDialog(@Nullable Project project) {
            super(project);
            setTitle("LMP Copy - Edit Instruction");
            init();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setPreferredSize(new Dimension(500, 300));

            JLabel label = new JLabel("Enter your instruction for this LMP file:");
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

            instructionArea = new JTextArea(10, 50);
            instructionArea.setLineWrap(true);
            instructionArea.setWrapStyleWord(true);
            //instructionArea.setPlaceholderText("e.g., Add a login feature to this React app");

            JScrollPane scrollPane = new JScrollPane(instructionArea);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            panel.add(label, BorderLayout.NORTH);
            panel.add(scrollPane, BorderLayout.CENTER);

            return panel;
        }

        public String getInstruction() {
            return instructionArea != null ? instructionArea.getText() : null;
        }

        @Override
        protected @Nullable JComponent createSouthPanel() {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            
            JButton cancelButton = new JButton("Cancel");
            cancelButton.addActionListener(e -> doCancelAction());
            
            JButton okButton = new JButton("Copy with Instruction");
            okButton.addActionListener(e -> doOKAction());
            okButton.setDefaultCapable(true);
            
            panel.add(cancelButton);
            panel.add(okButton);
            
            return panel;
        }
    }
}
